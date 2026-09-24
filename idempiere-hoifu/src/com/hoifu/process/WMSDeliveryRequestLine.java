package com.hoifu.process;

import java.math.BigDecimal;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MInventory;
import org.compiere.model.MProcessPara;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.AdempiereUserError;
import org.eevolution.model.X_PP_Cost_Collector;
import org.json.JSONObject;

import com.hoifu.model.MInOutRequisition;
import com.hoifu.model.MRestApiCallLog;
import com.hoifu.service.InOutRequisitionToInOutService;
import com.hoifu.service.InOutRequisitionToInventoryService;
import com.hoifu.service.InOutRequisitionToPPMaterialOrderService;

/**
 * WMS 请求收货确认（按单据行） 供外部 WMS 系统调用，根据 DocumentNo + Line 定位到具体的收货单行， 回写/校验数量，并以
 * RequestCode 作为幂等标识，避免重复请求造成重复处理。
 */
@org.adempiere.base.annotation.Process
public class WMSDeliveryRequestLine extends SvrProcess {


	private static final String param_DocumentNo = "DocumentNo";  
	private static final String param_Line = "Line";  
	private static final String param_Qty = "Qty";  
	private static final String param_RequestCode = "RequestCode";  
	
	/** 单据号，对应 M_InOut.DocumentNo */
	private String p_DocumentNo = null;
	/** 行号，对应 M_InOutLine.Line */
	private int p_Line = 0;
	/** 数量 */
	private BigDecimal p_Qty = null;
	/** WMS 请求编码，用于幂等校验 */
	private String p_RequestCode = null;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals(param_DocumentNo))
				p_DocumentNo = para[i].getParameterAsString();
			else if (name.equals(param_Line))
				p_Line = para[i].getParameterAsInt();
			else if (name.equals(param_Qty))
				p_Qty = (BigDecimal) para[i].getParameter();
			else if (name.equals(param_RequestCode))
				p_RequestCode = para[i].getParameterAsString();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}
	}

	@Override
	protected String doIt() throws Exception {
		// 1. 参数校验
		if (p_DocumentNo == null || p_DocumentNo.trim().isEmpty())
			throw new AdempiereUserError("请填写单号（" + param_DocumentNo +"）");
		if (p_Line <= 0)
			throw new AdempiereUserError("请填写行号（" + param_Line +"）");
		if (p_Qty == null)
			throw new AdempiereUserError("请填写数量（" + param_Qty +"）");
		if (p_RequestCode == null || p_RequestCode.trim().isEmpty())
			throw new AdempiereUserError("请填写请求编码（" + param_RequestCode +"）");

		if (MRestApiCallLog.isRequestCodeExists(p_RequestCode, getCtx(), get_TrxName())) {
		    getProcessInfo().setJsonData(MRestApiCallLog.getLastResponseBodyDataByRequestCode(p_RequestCode, getCtx(), get_TrxName()));  
			return  "请求成功（该请求为重复请求）";
		}
		
		MInOutRequisition requisition = new Query(getCtx(), MInOutRequisition.Table_Name,  
				"DocumentNo=? AND IsActive='Y'", get_TrxName())  
				.setParameters(p_DocumentNo)  
				.setOnlyActiveRecords(true)  
				.first();  
		if (requisition == null || requisition.get_ID() == 0)  
			throw new AdempiereException("找不到出入库申请单，单号：" + p_DocumentNo);  
		
		String tableName = requisition.getTableName();

		if (MInOut.Table_Name.equals(tableName))
		{
			InOutRequisitionToInOutService.createFromRequisition(p_DocumentNo, p_Line, p_Qty, getCtx(), get_TrxName());	
		}
		if (MInventory.Table_Name.equals(tableName))
		{
			InOutRequisitionToInventoryService.createFromRequisition(p_DocumentNo, p_Line, p_Qty, getCtx(), get_TrxName());
		}
		
		if (X_PP_Cost_Collector.Table_Name.equals(tableName))
		{
			InOutRequisitionToPPMaterialOrderService.createFromRequisition(p_DocumentNo, p_Line, p_Qty, getCtx(), get_TrxName());
		}
		
		JSONObject result = new JSONObject();
		result.put(param_DocumentNo, p_DocumentNo);
		result.put(param_Line, p_Line);
		result.put(param_Qty, p_Qty);
		result.put(param_RequestCode, p_RequestCode);

		getProcessInfo().setJsonData(result.toString());

		// 4. doIt() 仍然只能返回字符串，作为 summary 提示
		return "请求成功";
	}
}