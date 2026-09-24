package com.hoifu.event.processor;

import com.hoifu.model.MInOutRequisition;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.util.ContextRunnable;
import org.compiere.Adempiere;
import org.compiere.model.MBPartner;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.MWarehouse;
import org.compiere.model.PO;
import org.compiere.model.X_S_Resource;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.compiere.util.Trx;
import org.compiere.util.TrxEventListener;
import org.json.JSONObject;

import com.hoifu.model.M_Ext_System_Sync_Log;
import com.hoifu.service.extsync.ExtSyncResult;
import com.hoifu.service.extsync.ExtSyncSwitchService;
import com.hoifu.service.extsync.ExternalSystemAdapterRegistry;
import com.hoifu.service.extsync.IExternalSystemAdapter;
import com.hoifu.service.extsync.wms.WmsSystemAdapter;
import com.hoifu.utils.RetryBackoffPolicy;
import org.compiere.model.MBPartnerLocation;
import java.util.Map;

/**
 * 通用外部系统实时同步触发处理器 
 * 覆盖：MProduct/MBPartner/S_Resource/MInOut/I_PP_Order/X_PP_Order_Workflow/X_PP_Order_Node/X_PP_Order_BOMLine
 */
public class ExtSyncTriggerEventProcessor implements IEventProcessor {

	private static final CLogger log = CLogger.getCLogger(ExtSyncTriggerEventProcessor.class);
	private static final int MAX_QUICK_RETRY = 2; // 异步任务内快速重试次数（不含首次）

	private final ExtSyncSwitchService switchService;
	private final ExternalSystemAdapterRegistry adapterRegistry;

	public ExtSyncTriggerEventProcessor() {
		this.switchService = new ExtSyncSwitchService();
		this.adapterRegistry = new ExternalSystemAdapterRegistry();
	}

	@Override
	public boolean supports(PO po, String topic) {
		boolean isTargetTable = po instanceof MProduct || po instanceof MBPartner || po instanceof X_S_Resource
				|| po instanceof MWarehouse || po instanceof MLocator || po instanceof MBPartnerLocation;

		boolean isTargetTopic = IEventTopics.PO_AFTER_NEW.equals(topic) || IEventTopics.PO_AFTER_CHANGE.equals(topic)
				|| IEventTopics.PO_AFTER_DELETE.equals(topic) || IEventTopics.DOC_AFTER_COMPLETE.equals(topic);

		boolean isDocAfterCO =  ((po instanceof MInOutRequisition) && IEventTopics.DOC_AFTER_COMPLETE.equals(topic))
				|| ((po instanceof MInOutRequisition) && IEventTopics.DOC_AFTER_REVERSECORRECT.equals(topic));
		return isTargetTable && isTargetTopic || isDocAfterCO;
	}

	@Override
	public void process(PO po, String topic) {
		String businessType = resolveBusinessType(po);
		if (businessType == null)
			return;
		String eventType = resolveEventType(topic);
		if (eventType == null)
			return;

		for (IExternalSystemAdapter adapter : adapterRegistry.getAllAdapters()) {
			String systemType = adapter.getSystemType();

			if (!adapter.supports(po, businessType, eventType))
				continue;
			if (!switchService.isSyncEnabled(systemType))
				continue;
			if (!switchService.isOrgAllowed(systemType, po.getAD_Org_ID()))
				continue;

			registerAfterCommitSync(po, businessType, eventType, adapter);
		}
	}

	private void registerAfterCommitSync(PO po, String businessType, String eventType, IExternalSystemAdapter adapter) {
		Trx trx = Trx.get(po.get_TrxName(), false);
		if (trx == null) {
			log.warning("无事务上下文，跳过实时同步: " + businessType + "/" + po.get_ID());
			return;
		}
		final Map<String, Object> syncContext = adapter.captureSyncContext(po, businessType, eventType);
		final String tableName = po.get_TableName();
		final int tableId = po.get_Table_ID();
		final int recordId = po.get_ID();
		final int clientId = po.getAD_Client_ID();
		final int orgId = po.getAD_Org_ID();

		trx.addTrxEventListener(new TrxEventListener() {  
		    @Override  
		    public void afterCommit(Trx t, boolean success) {  
		        t.removeTrxEventListener(this);  
		        if (!success)  
		            return; // 事务回滚，不推送  
		  
		        Adempiere.getThreadPoolExecutor().submit(new ContextRunnable() {  
		            @Override  
		            protected void doRun() {  
		                doSyncWithRetry(tableName, tableId, recordId, businessType, eventType, adapter, clientId,  
		                        orgId, syncContext);  
		            }  
		        });  
		    }  
		  
		    @Override  
		    public void afterRollback(Trx t, boolean success) {  
		        t.removeTrxEventListener(this);  
		    }  
		  
		    @Override  
		    public void afterClose(Trx t) {  
		        t.removeTrxEventListener(this);  
		    }  
		});
	}

	/**
	 * 异步线程内执行：重新加载最新PO数据(非DELETE场景) -> 构建请求 -> 调用 -> 快速重试 -> 记录日志。
	 */
	private void doSyncWithRetry(String tableName, int tableId, int recordId, String businessType, String eventType,
			IExternalSystemAdapter adapter, int clientId, int orgId, Map<String, Object> context) {
		// 1. 重新加载最新数据。DELETE事件此时记录已物理删除，PO重新加载会得到空对象，
		// 因此DELETE场景直接传null，由各Adapter的buildRequest按需仅使用tableId/recordId构造请求体。
		PO po = null;
		if (!IExternalSystemAdapter.EVENT_DELETE.equals(eventType)) {
			po = MTable.get(Env.getCtx(), tableId).getPO(recordId, null);
			if (po == null || po.get_ID() <= 0) {
				log.warning("重新加载记录失败(可能已被删除): " + tableName + "_ID=" + recordId);
				return;
			}
		}

		String request = null;
		JSONObject response = null;
		String apiUrl = null;
		String lastErrorMessage = null;
		long start = System.currentTimeMillis();
		int attempt = 0;
		Exception lastException = null;

		while (attempt <= MAX_QUICK_RETRY) {
			try {
				if (request == null) {
					request = adapter.buildRequest(po, businessType, eventType, context);
					if (request == null) {
						// 按业务规则不需要同步，直接跳过，不记录FAILED，也不进入重试
						log.info("跳过同步: systemType=" + adapter.getSystemType() + ", businessType=" + businessType
								+ ", recordId=" + recordId);
						return;
					}
					apiUrl = adapter.resolveApiUrl(businessType, eventType);
				}
				ExtSyncResult result = adapter.send(apiUrl, request);
				response = new JSONObject(result.getRawResponse());
				long execTime = System.currentTimeMillis() - start;

				if (result.isSuccess()) {
					M_Ext_System_Sync_Log.logSuccess(adapter.getSystemType(), tableId, recordId, businessType,
							eventType, apiUrl, request, response, execTime, clientId, orgId);
					return; // 业务成功，结束
				} else {
					// 业务失败（HTTP层面没报错，但WMS/MES自己说处理失败），
					// 走和异常同样的重试计数逻辑，把错误信息记录下来
					lastErrorMessage = result.getMessage();
					attempt++;
					if (attempt <= MAX_QUICK_RETRY) {
						Thread.sleep(RetryBackoffPolicy.calcQuickRetryDelayMs(attempt));
					}
				}
			} catch (Exception e) {
				// 网络异常/超时等传输层错误，走原有异常重试分支
				lastException = e;
				attempt++;
				if (attempt <= MAX_QUICK_RETRY) {
					try {
						Thread.sleep(RetryBackoffPolicy.calcQuickRetryDelayMs(attempt));
					} catch (InterruptedException e1) {
						log.warning("睡眠异常： " + e1.getMessage());
					}
				}
			}
		}
		
		// 快速重试全部失败，落库等待手动重试进程后续重试
		long execTime = System.currentTimeMillis() - start;
		M_Ext_System_Sync_Log.logFailure(adapter.getSystemType(), tableId, recordId, businessType, eventType, apiUrl,
				request, response, lastException != null ? lastException.getMessage() : lastErrorMessage, execTime, clientId,
				orgId, 0);
	}
	
	/** 根据PO具体类型分发到WMS业务类型常量，新增业务对象时只需在此追加一个分支 */
	private String resolveBusinessType(PO po) {
		if (po instanceof MProduct)
			return WmsSystemAdapter.BT_PRODUCT;
		if (po instanceof MBPartner)
			return WmsSystemAdapter.BT_BPARTNER;
		if (po instanceof X_S_Resource)
			return WmsSystemAdapter.BT_RESOURCE;
		if (po instanceof MInOutRequisition) 
		    return ((MInOutRequisition) po).isOutStock()  ? WmsSystemAdapter.BT_STOCK_OUT : WmsSystemAdapter.BT_STOCK_IN;  
		if (po instanceof MWarehouse)  
			return WmsSystemAdapter.BT_WAREHOUSE;  
		if (po instanceof MLocator)  
			return WmsSystemAdapter.BT_LOCATOR;
		return null;
	}

	private String resolveEventType(String topic) {
		if (IEventTopics.PO_AFTER_NEW.equals(topic))
			return IExternalSystemAdapter.EVENT_INSERT;
		if (IEventTopics.PO_AFTER_CHANGE.equals(topic))
			return IExternalSystemAdapter.EVENT_UPDATE;
		if (IEventTopics.PO_AFTER_DELETE.equals(topic))
			return IExternalSystemAdapter.EVENT_DELETE;
		if (IEventTopics.DOC_AFTER_COMPLETE.equals(topic))  
			return IExternalSystemAdapter.EVENT_COMPLETE;
		if (IEventTopics.DOC_AFTER_REVERSECORRECT.equals(topic))
			return IExternalSystemAdapter.EVENT_VOID;
		return null;
	}
}