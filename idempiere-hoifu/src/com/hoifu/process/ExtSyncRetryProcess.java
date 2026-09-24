package com.hoifu.process;

import java.util.List;
import java.util.Optional;

import org.adempiere.base.annotation.Process;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.SvrProcess;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.json.JSONObject;

import com.hoifu.model.M_Ext_System_Sync_Log;
import com.hoifu.service.extsync.ExtSyncResult;
import com.hoifu.service.extsync.ExternalSystemAdapterRegistry;
import com.hoifu.service.extsync.IExternalSystemAdapter;
import com.hoifu.utils.RetryBackoffPolicy;

/**
 * 通用外部系统同步-失败手动重试进程。 不是主同步路径（主路径是事务提交后异步实时推送），
 * 只处理 Ext_System_Sync_Log 中 Status=FAILED 且到达 Next_Retry_Time 的少量记录， 可手动执行
 * @ClassName: ExtSyncRetryProcess
 * @author ldh
 * @date 2026年8月19日
 */
@Process
public class ExtSyncRetryProcess extends SvrProcess {

	private static final CLogger log = CLogger.getCLogger(ExtSyncRetryProcess.class);
	private static final int BATCH_LIMIT = 100;
	private static final int RETRY_MAX_COUNT = 5; // 超过则不再自动重试，转人工介入

	private final ExternalSystemAdapterRegistry adapterRegistry = new ExternalSystemAdapterRegistry();

	@Override
	protected void prepare() {
		// 当前无需自定义参数；如需只处理某个系统，可加一个可选Parameter: P_SystemType
	}

	@Override
	protected String doIt() throws Exception {
		int successCount = 0, failCount = 0, exhaustedCount = 0;

		// 遍历当前已注册的全部外部系统类型（目前只有WMS，未来自动覆盖新接入系统）
		for (String systemType : adapterRegistry.getAllSystemTypes()) {
			List<M_Ext_System_Sync_Log> pending = M_Ext_System_Sync_Log.loadPendingRetries(systemType, BATCH_LIMIT);

			for (M_Ext_System_Sync_Log record : pending) {
				if (record.getRetry_Count() >= RETRY_MAX_COUNT) {
					exhaustedCount++;
					continue;
				}

				Optional<IExternalSystemAdapter> adapterOpt = adapterRegistry.findAdapter(systemType,
						record.getBusiness_Type());
				if (adapterOpt.isEmpty()) {
					// 找不到适配器（例如配置数据脏了），直接标记失败等待下次或人工处理
					record.markRetryFailed("未找到匹配的适配器: " + systemType + "/" + record.getBusiness_Type());
					failCount++;
					continue;
				}
				IExternalSystemAdapter adapter = adapterOpt.get();

				long start = System.currentTimeMillis();
				try {
					// 重新加载最新PO数据；DELETE事件记录已物理删除，po传null，
					// 由Adapter的buildRequest仅依赖tableId/recordId构造请求体
					PO po = null;
					if (!"DELETE".equals(record.getEvent_Type())) {
						po = MTable.get(Env.getCtx(), record.getAD_Table_ID()).getPO(record.getRecord_ID(), null);
						if (po == null || po.get_ID() <= 0) {
							record.markRetryFailed("重新加载记录失败(可能已被删除)");
							failCount++;
							continue;
						}
					}

					String request = adapter.buildRequest(po, record.getBusiness_Type(), record.getEvent_Type(), null);
					if (request == null) {
						// 按业务规则不需要同步，直接跳过，不记录FAILED，也不进入重试
						log.info("跳过同步: systemType=" + adapter.getSystemType() + ", businessType=" + record.getBusiness_Type()
								+ ", recordId=" + record.getRecord_ID());
						continue;
					}
					String apiUrl = adapter.resolveApiUrl(record.getBusiness_Type(), record.getEvent_Type());
					ExtSyncResult result = adapter.send(apiUrl, request);
					long execTime = System.currentTimeMillis() - start;

					if (result.isSuccess()) {
						record.markSuccess(result.getRawResponse(), execTime);
						successCount++;
					} else {
						// 业务失败（HTTP层面没报错，但WMS/MES自己说处理失败），
						// 走和异常同样的重试计数逻辑，把错误信息记录下来
						record.markRetryFailed(result.getMessage());
						failCount++;
					}
					
				} catch (Exception e) {
					log.warning("手动重试失败: systemType=" + systemType + ", logId=" + record.get_ID() + ", error="
							+ e.getMessage());
					record.markRetryFailed(e.getMessage());
					failCount++;
				}
			}
		}

		return "重试完成: 成功=" + successCount + ", 失败=" + failCount + ", 已耗尽重试次数=" + exhaustedCount;
	}
}