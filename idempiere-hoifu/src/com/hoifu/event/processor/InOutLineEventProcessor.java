package com.hoifu.event.processor;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.adempiere.base.Core;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.hoifu.service.qc.IIQCService;
import com.hoifu.service.qc.IOQCService;
import com.hoifu.service.qc.IRQCService;

public class InOutLineEventProcessor implements IEventProcessor {

	private static final CLogger log = CLogger.getCLogger(InOutLineEventProcessor.class);

	private final IIQCService iqcService;
	private final IOQCService oqcService;
	private final IRQCService rqcService;

	public InOutLineEventProcessor(IIQCService iqcService, IOQCService oqcService, IRQCService rqcService) {
		this.iqcService = iqcService;
		this.oqcService = oqcService;
		this.rqcService = rqcService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MInOutLine;

	}

	@Override
	public void process(PO po, String topic) {
		MInOutLine line = (MInOutLine) po;
		changeValueByProduct(line, topic);
		updateHeaderWeight(line , topic);
		if (IEventTopics.PO_AFTER_NEW.equals(topic)) {
			handleQC(line); // 保存后逻辑
		}
	}

	// ── QC 逻辑 ──────────────────────────────────────────────────────────────
	private void handleQC(MInOutLine line) {
		if (!MSysConfig.getBooleanValue("QC_ENABLE_CHECK", false))
			return;
		MInOut parent = (MInOut) line.getParent();
		if (rqcService.isReturnDocument(parent))
			rqcService.createFromLine(parent, line);
		else if (!parent.isSOTrx())
			iqcService.createFromReceiptLine(line);
		else
			oqcService.createFromShipmentLine(line);
	}


	private void updateHeaderWeight(MInOutLine line, String topic) {  
	    boolean isNew = IEventTopics.PO_AFTER_NEW.equals(topic);  
	    boolean isChange = IEventTopics.PO_AFTER_CHANGE.equals(topic)  
	            && (line.is_ValueChanged("Weight") || line.is_ValueChanged(MInOutLine.COLUMNNAME_QtyEntered));
	    if (!isNew && !isChange) {
	    	return;
	    }
	    
	    int M_InOut_ID = line.getM_InOut_ID();  
	    int M_InOutLine_ID = line.get_ID();  
	    if (M_InOut_ID <= 0)  
	        return;  
	  
	    BigDecimal total = DB.getSQLValueBDEx(line.get_TrxName(),  
	        "SELECT COALESCE(SUM(Weight * QtyEntered), 0) FROM M_InOutLine " +  
	        "WHERE M_InOut_ID=? AND IsActive='Y'",  
	        M_InOut_ID);  
	    if (total == null)  
	        total = BigDecimal.ZERO;  
	  
	    MInOut parent = new MInOut(line.getCtx(), M_InOut_ID, line.get_TrxName());  
	    parent.set_ValueOfColumn("Weight", total);  
	    parent.saveEx();  
	}
	
	private void changeValueByProduct(MInOutLine line, String topic) {

		// 仅在 PO_BEFORE_CHANGE 且 M_Product_ID 确实变化时，或 PO_BEFORE_NEW 时触发
		boolean isChange = IEventTopics.PO_BEFORE_CHANGE.equals(topic)
				&& line.is_ValueChanged(MInOutLine.COLUMNNAME_M_Product_ID);
		boolean isNew = IEventTopics.PO_BEFORE_NEW.equals(topic);

		if (!isChange && !isNew) {
			return;
		}

		Object productIdObj = line.get_Value(MInOutLine.COLUMNNAME_M_Product_ID);
		if (productIdObj == null)
			return;
		int productId = ((Number) productIdObj).intValue();
		if (productId <= 0)
			return;

		MProduct product = MProduct.get(line.getCtx(), productId);
		if (product == null)
			return;

		// 面积
		Object boxArea = product.get_Value("BoxArea");
		if (boxArea != null)
			line.set_ValueOfColumn("Area", boxArea);

		// 规格：CardLength * CardWidth
		BigDecimal cardLength = toBD(product.get_Value("CardLength"));
		BigDecimal cardWidth = toBD(product.get_Value("CardWidth"));
		if (cardLength.compareTo(BigDecimal.ZERO) != 0 && cardWidth.compareTo(BigDecimal.ZERO) != 0) {
			line.set_ValueOfColumn("Specification", cardLength.stripTrailingZeros().toPlainString() + "*"
					+ cardWidth.stripTrailingZeros().toPlainString());
		}

		// 重量
		Object weightNet = product.get_Value("WeightNet");
		if (weightNet != null)
			line.set_ValueOfColumn("Weight", weightNet);

		// 压线
		Object creaseLine = product.get_Value("CreaseLine");
		if (creaseLine != null)
			line.set_ValueOfColumn("CreaseLine", creaseLine);
	}

	// ── 工具方法 ──────────────────────────────────────────────────────────────
	private BigDecimal toBD(Object val) {
		if (val instanceof BigDecimal)
			return (BigDecimal) val;
		if (val == null)
			return BigDecimal.ZERO;
		try {
			return new BigDecimal(val.toString());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}
}