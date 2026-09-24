package com.hoifu.service.extsync.wms;  
  
import java.math.BigDecimal;  
import java.util.List;  
import java.util.Map;  
  
import org.compiere.model.MBPartner;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.MProduct;  
import org.compiere.model.MUOM;  
import org.compiere.model.MUser;  
import org.compiere.model.MWarehouse;  
import org.json.JSONArray;  
import org.json.JSONObject;  
  
import com.hoifu.model.MInOutRequisition;  
import com.hoifu.model.MInOutRequisitionLine;  
  
/**  
 * 出入库申请单(M_InOut_Requisition) -> WMS 报文转换服务  
 */  
public class WmsInOutRequisitionService {  

	private static final int LINE_STATUS_FIXED = 20;  
	private static final int OPERATE_FLAG_FIXED = 1;  
	/** 取消场景固定操作标志 */  
	private static final int OPERATE_FLAG_CANCEL = -1;  
  
	public static String buildInOutRequisitionPayload(String eventType, MInOutRequisition po,  
			Map<String, Object> syncContext) {  
	  
		if (po.isOutStock()) {  
			return buildOutboundPayload(po);  
		} else {  
			return buildInboundPayload(po);  
		}  
	}
	
	// =========================================================  
	// 取消  
	// =========================================================  
	public static String buildInOutRequisitionCancelPayload(String eventType, MInOutRequisition po,  
			Map<String, Object> syncContext) {  
	  
		JSONObject body = new JSONObject();  
	  
		body.put("businessCode", po.getDocumentNo());  
	  
		// deliveryType 规则与入库/出库报文保持一致，按 IsSOTrx 区分方向  
		int deliveryType = po.isSOTrx() ? po.resolveOutboundDeliveryType() : po.resolveInboundDeliveryType();  
		body.put("deliveryType", deliveryType);  
	  
		body.put("operateFlag", OPERATE_FLAG_CANCEL);  
	  
		putOperator(body, po);  
	  
		return body.toString();  
	} 
  
	// =========================================================  
	// 入库场景（IsSOTrx = 'N'）  
	// =========================================================  
	private static String buildInboundPayload(MInOutRequisition po) {  
		JSONObject body = new JSONObject();  
  
		body.put("businessCode", po.getDocumentNo());  
		body.put("partnerCode", resolvePartnerCode(po));  
  
		int orderId = po.getC_Order_ID();  
		if (orderId > 0) {  
			MOrder order = new MOrder(po.getCtx(), orderId, po.get_TrxName());  
			body.put("purchaseCode", order.getDocumentNo());  
		} else {  
			body.put("purchaseCode", "");  
		}  
  
		body.put("deliveryType", po.resolveInboundDeliveryType());  
		putOperator(body, po);  
  
		JSONArray lines = new JSONArray();  
		List<MInOutRequisitionLine> lineList = po.getLines();  
		if (lineList != null) {  
			for (MInOutRequisitionLine line : lineList) {  
				lines.put(buildInboundLine(po, line));  
			}  
		}  
		body.put("lines", lines);  
  
		return body.toString();  
	}  
  
	private static JSONObject buildInboundLine(MInOutRequisition po, MInOutRequisitionLine line) {  
		JSONObject item = new JSONObject();  
  
		item.put("warehouseCode", resolveWarehouseCode(po));  
		item.put("lineBusinessCode", String.valueOf(line.getLine()));  
  
		int orderLineId = line.getC_OrderLine_ID();  
		if (orderLineId > 0) {  
			MOrderLine orderLine = new MOrderLine(po.getCtx(), orderLineId, po.get_TrxName());  
			item.put("purchaseLineCode", String.valueOf(orderLine.getLine()));  
		} else {  
			item.put("purchaseLineCode", "");  
		}  
  
		int productId = line.getM_Product_ID();  
		MProduct product = productId > 0 ? MProduct.get(po.getCtx(), productId) : null;  
		item.put("materialCode", product != null ? product.getValue() : "");  
  
		//JSONArray codeList = new JSONArray();  
		//codeList.put(po.getDocumentNo());  
//		if (product != null) {  
//			String productNoCust = product.get_ValueAsString("ProductNoCust");  
//			if (productNoCust != null && !productNoCust.isEmpty()) {  
//				codeList.put(productNoCust);  
//			}  
//		}  
		//item.put("codeList", codeList);  
		
		BigDecimal qty = line.getQtyRequested();  
		item.put("number", qty != null ? qty : BigDecimal.ZERO);  
		item.put("status", LINE_STATUS_FIXED);  
		//item.put("lot", LOT_FIXED);  
		item.put("remark", line.getDescription() != null ? line.getDescription() : "");  

  
		return item;  
	}  
  

	// =========================================================  
	// 出库场景（IsSOTrx = 'Y'）  
	// =========================================================  
	private static String buildOutboundPayload(MInOutRequisition po) {  
		JSONObject body = new JSONObject();  
  
		body.put("businessCode", po.getDocumentNo());  
		body.put("partnerCode", resolvePartnerCode(po));  
		body.put("deliveryType", po.resolveOutboundDeliveryType());  
		body.put("operateFlag", OPERATE_FLAG_FIXED);  
		putOperator(body, po);  
  
		JSONArray lines = new JSONArray();  
		List<MInOutRequisitionLine> lineList = po.getLines();  
		if (lineList != null) {  
			for (MInOutRequisitionLine line : lineList) {  
				lines.put(buildOutboundLine(po, line));  
			}  
		}  
		body.put("lines", lines);  
  
		return body.toString();  
	}  
  
	private static JSONObject buildOutboundLine(MInOutRequisition po, MInOutRequisitionLine line) {  
		JSONObject item = new JSONObject();  
  
		item.put("lineBusinessCode", String.valueOf(line.getLine()));  
		item.put("warehouseCode", resolveWarehouseCode(po));  
  
		int productId = line.getM_Product_ID();  
		MProduct product = productId > 0 ? MProduct.get(po.getCtx(), productId) : null;  
		item.put("materialCode", product != null ? product.getValue() : "");  
  
		//item.put("materialBatch", LOT_FIXED);  
  
		item.put("materialState", LINE_STATUS_FIXED);  
		if (line.getDescription() != null) {
			item.put("remark", line.getDescription());
		}
		  
  
		BigDecimal qty = line.getQtyRequested();  
		item.put("numberRequired", qty != null ? qty : BigDecimal.ZERO);  
  
		int uomId = line.getC_UOM_ID();  
		String uomName = "";  
		if (uomId > 0) {  
			MUOM uom = MUOM.get(po.getCtx(), uomId);  
			uomName = uom != null ? uom.getName() : "";  
			item.put("materialUnit", uomName);  
		}  

  
		return item;  
	}  
  

	// =========================================================  
	// 公共辅助方法  
	// =========================================================  
	private static String resolvePartnerCode(MInOutRequisition po) {  
		int bpartnerId = po.getC_BPartner_ID();  
		if (bpartnerId <= 0) return "";  
		MBPartner bp = MBPartner.get(po.getCtx(), bpartnerId);  
		return bp != null ? bp.getValue() : "";  
	}  
  
	private static String resolveWarehouseCode(MInOutRequisition po) {  
		int warehouseId = po.getM_Warehouse_ID();  
		if (warehouseId <= 0) return "";  
		MWarehouse wh = MWarehouse.get(po.getCtx(), warehouseId);  
		// TODO: 确认 M_Warehouse 是否有自定义 Value 列  
		return wh != null ? wh.getValue() : "";  
	}  
	  
	
	/**  
	 * 往主 body 上追加操作人信息，CreatedBy -> AD_User。  
	 */  
	private static void putOperator(JSONObject body, MInOutRequisition po) {  
		int createdBy = po.getCreatedBy();  
		String operatorId = "";  
		String operatorName = "";  
		if (createdBy > 0) {  
			MUser user = MUser.get(po.getCtx(), createdBy);  
			if (user != null) {  
				operatorId = user.getValue();  
				operatorName = user.getName();  
			}  
		}  
		body.put("operatorId", operatorId);  
		body.put("operatorName", operatorName);  
	}  
}