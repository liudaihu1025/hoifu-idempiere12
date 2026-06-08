package com.hoifu.event;

import java.util.List;

import org.adempiere.base.event.AbstractEventHandler;
import org.adempiere.base.event.IEventManager;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAllocationHdr;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProduct;
import org.compiere.model.PO;
import org.eevolution.model.I_PP_Order;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.event.Event;

import com.hoifu.event.processor.DefectSummaryEventProcessor;
import com.hoifu.event.processor.IEventProcessor;
import com.hoifu.event.processor.IPQCEventProcessor;
import com.hoifu.event.processor.ProductEventProcessor;
import com.hoifu.event.processor.QCInOutEventProcessor;
import com.hoifu.event.processor.QCInOutLineEventProcessor;
import com.hoifu.event.processor.VoucherEventProcessor;
import com.hoifu.model.qc.X_QC_DefectRecord;
import com.hoifu.service.IGlVoucherService;
import com.hoifu.service.impl.GlVoucherServiceImpl;
import com.hoifu.service.qc.IDefectSummaryService;
import com.hoifu.service.qc.IIPQCService;
import com.hoifu.service.qc.IIQCService;
import com.hoifu.service.qc.IOQCService;
import com.hoifu.service.qc.IRQCService;
import com.hoifu.service.qc.impl.DefectSummaryServiceImpl;
import com.hoifu.service.qc.impl.IPQCServiceImpl;
import com.hoifu.service.qc.impl.IQCServiceImpl;
import com.hoifu.service.qc.impl.OQCServiceImpl;
import com.hoifu.service.qc.impl.RQCServiceImpl;

/**
 * 通用的事件监听器，后续新的监听事件新增Processor即可
 * 
 * @ClassName: BaseEnventHandler
 * @author ldh
 * @date 2026年5月11日
 */
@Component(reference = @Reference(name = "IEventManager", bind = "bindEventManager", unbind = "unbindEventManager", policy = ReferencePolicy.STATIC, cardinality = ReferenceCardinality.MANDATORY, service = IEventManager.class))
public class BaseEnventHandler extends AbstractEventHandler {

	private final IIQCService iqcService = new IQCServiceImpl();
	private final IOQCService oqcService = new OQCServiceImpl();
	private final IIPQCService ipqcService = new IPQCServiceImpl();
	private final IRQCService rqcService = new RQCServiceImpl();
	private final IGlVoucherService voucherService = new GlVoucherServiceImpl();
	private final IDefectSummaryService defectService = new DefectSummaryServiceImpl();

	private List<IEventProcessor> processors;

	@Override
	protected void initialize() {
		// ===== 凭证模块 =====
		registerEvent(IEventTopics.DOC_AFTER_POST);
		registerEvent(IEventTopics.DOC_BEFORE_REACTIVATE);
		registerEvent(IEventTopics.DOC_AFTER_REVERSECORRECT);
		registerEvent(IEventTopics.DOC_AFTER_REVERSEACCRUAL);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, MAllocationHdr.Table_Name);

		// ===== 品质模块 =====
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MInOutLine.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, MInOut.Table_Name);
		registerTableEvent(IEventTopics.DOC_AFTER_PREPARE, I_PP_Order.Table_Name);
		registerTableEvent(IEventTopics.DOC_BEFORE_COMPLETE, I_PP_Order.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, X_QC_DefectRecord.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, X_QC_DefectRecord.Table_Name);
		registerTableEvent(IEventTopics.PO_BEFORE_DELETE, X_QC_DefectRecord.Table_Name);

		// ===== 物料管理 =====
		registerTableEvent(IEventTopics.PO_BEFORE_NEW, MProduct.Table_Name);  
		registerTableEvent(IEventTopics.PO_BEFORE_CHANGE, MProduct.Table_Name);
		registerTableEvent(IEventTopics.PO_AFTER_NEW, MProduct.Table_Name);  
		registerTableEvent(IEventTopics.PO_AFTER_CHANGE, MProduct.Table_Name);
		// ===== 初始化处理器链 =====
		processors = List.of(
				new VoucherEventProcessor(voucherService),
				new QCInOutLineEventProcessor(iqcService, oqcService, rqcService),
				new QCInOutEventProcessor(iqcService, oqcService, rqcService), 
				new IPQCEventProcessor(ipqcService),
				new DefectSummaryEventProcessor(defectService),
				new ProductEventProcessor());
	}

	@Override
	protected void doHandleEvent(Event event) {
		PO po = getPO(event);
		if (po == null)
			return;
		String topic = event.getTopic();

		// 只执行匹配到的第一个分支
		processors.stream().filter(p -> p.supports(po, topic)).findFirst().ifPresent(p -> p.process(po, topic));
	}
}