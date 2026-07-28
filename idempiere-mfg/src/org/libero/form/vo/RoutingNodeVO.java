// idempiere-mfg/src/org/libero/form/vo/RoutingNodeVO.java  
package org.libero.form.vo;

import java.math.BigDecimal;

/**
 * 工艺工序行值对象
 */
public class RoutingNodeVO {
	public int lineNo;
	public int adRoutingNodeId;
	public String routingNodeValue;
	public String routingNodeName;
	public String description;
	/** 工艺难度等级代码（processdifficulty，用户从下拉选择，UI显示） */
	public String processdifficulty = "";
	/** 难度系数（从 c_paperscrapstd 读取，不在UI显示，仅用于损耗计算） */
	public BigDecimal difficultyfactor = BigDecimal.ONE;
	/** 标准基础数量（来自 C_PaperScrapStd.stdbaseqty） */  
	public BigDecimal stdbaseqty = BigDecimal.ZERO;  
	/** 标准放损率（来自 C_PaperScrapStd.stdscraprate） */  
	public BigDecimal stdscraprate = BigDecimal.ZERO;
	/** 色数（印刷工序必填，默认1） */
	public BigDecimal colorCount = BigDecimal.ONE;
	/** 标准加工数（PP_Order_Node.QtyRequiered，用户可编辑） */
	public BigDecimal qtyRequiered = null;
	public BigDecimal qtyDelivered = BigDecimal.ZERO;
	public BigDecimal qtyScrap = BigDecimal.ZERO;
	/** 纸张放损数（PP_Order_Node.QtyPaperScrap，提交时保存） */
	public BigDecimal qtyPaperScrap = BigDecimal.ZERO;
	/** 累加放损数（PP_Order_Node.QtyPaperTotalScrap，提交时保存） */
	public BigDecimal qtyPaperTotalScrap = BigDecimal.ZERO;
	/** 累计纸张放损率（PP_Order_Node.RatePaperTotalScrap，提交时保存） */
	public BigDecimal ratePaperTotalScrap = BigDecimal.ZERO;
	public int ppOrderNodeId;
	public int adWFNodeId;
	public boolean isDeleted = false;
	public boolean isCurrentNode = false;
	/** 效果名称（来自 dy_graphicdesigneffect.name，可选） */  
	public String effectName = "";
	/** 工序组ID */  
	public int operationClassId = 0;  
	/** 工序组名称 */  
	public String operationClassName = "";
}