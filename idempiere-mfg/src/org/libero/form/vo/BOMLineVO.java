// idempiere-mfg/src/org/libero/form/vo/BOMLineVO.java  
package org.libero.form.vo;

import java.math.BigDecimal;

/**
 * BOM物料行值对象（内存暂存，提交时统一持久化）
 */
public class BOMLineVO {
	public int lineNo;
	public int productId;
	public String productValue;
	public String productName;
	public int uomId;
	public String uomName;
	/** BOM数量 */
	public BigDecimal qtyBOM = BigDecimal.ONE;
	/** 比例用量 */
	public BigDecimal nQtyBOM = BigDecimal.ONE;
	/** 需求用量（由比例用量计算得出） */
	public BigDecimal qtyRequiered = null;
	/** 实际领用数量（只读，来自已完成的领料） */
	public BigDecimal qtyDelivered = BigDecimal.ZERO;
	/** 所属工序 AD_Routing_Node_ID */
	public int routingNodeId;
	public int ppOrderNodeId = 0; // PP_Order_BOMLine.PP_Order_Node_ID
	public String routingNodeName = "";
	/** 已持久化的 PP_Order_BOMLine_ID，0 表示新行 */
	public int ppOrderBOMLineId;
	/** 已持久化的 PP_Product_BOMLine_ID，0 表示新行 */
	public int ppProductBOMLineId;
	public boolean isDeleted = false;
	
	/** 累计纸张放损数（主物料用，提交时保存到 PP_Order_BOMLine.QtyPaperTotalScrap） */
	public BigDecimal qtyPaperTotalScrap = BigDecimal.ZERO;
	
	/** 当前组织累计库存数量（只读，查询时填充） */  
	public BigDecimal qtyOnHand = BigDecimal.ZERO;
	/** 效果名称（来自 dy_graphicdesigneffect.name，可选） */  
	public String effectName = "";
}