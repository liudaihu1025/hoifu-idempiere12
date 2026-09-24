/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *
 * Contributor(s): victor.perez@e-evolution.com http://www.e-evolution.com    *
 *****************************************************************************/

package org.adempiere.model.engines;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_M_CostElement;
import org.compiere.model.MAcctSchema;
import org.compiere.model.MCost;
import org.compiere.model.MCostDetail;
import org.compiere.model.MCostElement;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategoryAcct;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTransaction;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.libero.model.MPPCostCollector;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderCost;
import org.libero.model.RoutingService;
import org.libero.model.RoutingServiceFactory;
import org.libero.tables.I_PP_Order_BOMLine;

import com.hoifu.enums.HFSysConfigEnum;

/**
 * Cost Engine
 * @author victor.perez@e-evolution.com http://www.e-evolution.com
 *
 */
public class CostEngine
{
	/**	Logger							*/
	protected transient CLogger	log = CLogger.getCLogger (getClass());

	public String getCostingMethod(MProduct product, int C_AcctSchema_ID) {
		// 添加空值检查，直接返回MAcctSchema的成本计价方法
		MAcctSchema mAcctSchema = MAcctSchema.get(Env.getCtx(), C_AcctSchema_ID, null);
		if (product == null) {
			return mAcctSchema.getCostingMethod();
		}

		MProductCategoryAcct acct = MProductCategoryAcct.get(product.getCtx(), product.getM_Product_Category_ID(),
				C_AcctSchema_ID, null);
		String costingMethod = Objects.nonNull(acct) ? acct.getCostingMethod() : "";
		if (costingMethod == null || costingMethod.isEmpty()) {
			costingMethod = mAcctSchema.getCostingMethod();
		}

		return costingMethod;
	}
	
	public BigDecimal getResourceStandardCostRate(MPPCostCollector cc, int S_Resource_ID, CostDimension d, String trxName)
	{
		final MProduct resourceProduct = MProduct.forS_Resource_ID(Env.getCtx(), S_Resource_ID, null);
		return getProductStandardCostPrice(
				cc,
				resourceProduct,
				MAcctSchema.get(Env.getCtx(), d.getC_AcctSchema_ID()),
				MCostElement.get(Env.getCtx(), d.getM_CostElement_ID())
		);
	}
	
	public BigDecimal getResourceActualCostRate(MPPCostCollector cc, int S_Resource_ID, CostDimension d, String trxName)
	{
		if (S_Resource_ID <= 0)
			return Env.ZERO;
		final MProduct resourceProduct = MProduct.forS_Resource_ID(Env.getCtx(), S_Resource_ID, null);
		return getProductActualCostPrice(
				cc,
				resourceProduct,
				MAcctSchema.get(Env.getCtx(), d.getC_AcctSchema_ID()),
				MCostElement.get(Env.getCtx(), d.getM_CostElement_ID()),
				trxName
		);
	}
	
	public BigDecimal getProductActualCostPrice(MPPCostCollector cc, MProduct product, MAcctSchema as, MCostElement element, String trxName)
	{
		if (Objects.isNull(product)) return Env.ZERO;
		
		CostDimension d = new CostDimension(product,
				as, as.getM_CostType_ID(),
				cc.getAD_Org_ID(), //AD_Org_ID,
				product.getM_AttributeSetInstance_ID(), //M_ASI_ID,
				element.getM_CostElement_ID());
		MCost cost = d.toQuery(MCost.class, trxName).firstOnly();
		if(cost == null)
		{	
			return Env.ZERO;
			//throw new AdempiereException("@NotFound@ @M_Cost_ID@ - "+as+", "+element); 
		}	
		BigDecimal price = Env.ZERO.compareTo(cost.getCurrentCostPrice()) < 0 ? cost.getCurrentCostPrice()
				: cost.getCurrentCostPrice().add(cost.getCurrentCostPriceLL());
		return roundCost(price, as.getC_AcctSchema_ID());
	}

	public BigDecimal getProductStandardCostPrice(MPPCostCollector cc, MProduct product, MAcctSchema as, MCostElement element)
	{
		if (Objects.isNull(product)) return Env.ZERO;
		
		CostDimension d = new CostDimension(product,
				as, as.getM_CostType_ID(),
				cc.getAD_Org_ID(), //AD_Org_ID,
				product.getM_AttributeSetInstance_ID(), //M_ASI_ID,
				element.getM_CostElement_ID());
		MPPOrderCost oc = d.toQuery(MPPOrderCost.class, MPPOrderCost.COLUMNNAME_PP_Order_ID+"=?",
				new Object[]{cc.getPP_Order_ID()},
				cc.get_TrxName())
		.firstOnly();
		if (oc == null)
		{
			return Env.ZERO;
		}
		BigDecimal costs = oc.getCurrentCostPrice().add(oc.getCurrentCostPriceLL());
		return roundCost(costs, as.getC_AcctSchema_ID());
	}
	
	protected BigDecimal roundCost(BigDecimal price, int C_AcctSchema_ID)
	{
		// Fix Cost Precision 
		int precision = MAcctSchema.get(Env.getCtx(), C_AcctSchema_ID).getCostingPrecision();
		BigDecimal priceRounded = price;
		if (priceRounded.scale() > precision)
		{
			priceRounded = priceRounded.setScale(precision, RoundingMode.HALF_UP);
		}
		return priceRounded;
	}

	public Collection<MCost> getByElement (MProduct product, MAcctSchema as, 
			int M_CostType_ID, int AD_Org_ID, int M_AttributeSetInstance_ID, int M_CostElement_ID)
	{
		CostDimension cd = new CostDimension(product, as, M_CostType_ID,
				AD_Org_ID, M_AttributeSetInstance_ID,
				M_CostElement_ID);
		return cd.toQuery(MCost.class, product.get_TrxName())
		.setOnlyActiveRecords(true)
		.list();
	}

	/**
	 * Get Cost Detail
	 * @param model Model Inventory Line
	 * @param as Account Schema
	 * @param M_CostElement_ID Cost Element
	 * @param M_AttributeSetInstance_ID
	 * @return MCostDetail 
	 */
	private MCostDetail getCostDetail(IDocumentLine model, MTransaction mtrx ,MAcctSchema as, int M_CostElement_ID)
	{
		final String whereClause = "AD_Client_ID=? AND AD_Org_ID=?"
			+" AND "+model.get_TableName()+"_ID=?" 
			+" AND "+MCostDetail.COLUMNNAME_M_Product_ID+"=?"
			+" AND "+MCostDetail.COLUMNNAME_M_AttributeSetInstance_ID+"=?"
			+" AND "+MCostDetail.COLUMNNAME_C_AcctSchema_ID+"=?"
			//						+" AND "+MCostDetail.COLUMNNAME_M_CostType_ID+"=?"
			+" AND "+MCostDetail.COLUMNNAME_M_CostElement_ID+"=?";
		final Object[] params = new Object[]{
				mtrx.getAD_Client_ID(), 
				mtrx.getAD_Org_ID(), 
				model.get_ID(),
				mtrx.getM_Product_ID(),
				mtrx.getM_AttributeSetInstance_ID(),
				as.getC_AcctSchema_ID(),
				//as.getM_CostType_ID(), 
				M_CostElement_ID, 
		};
		return new Query(mtrx.getCtx(),MCostDetail.Table_Name, whereClause , mtrx.get_TrxName())
		.setParameters(params)
		.firstOnly();
	}	
	
	private BigDecimal calculateFG(MPPCostCollector current, MCostElement element, boolean isVariance) {
		List<MPPCostCollector> ccs = new Query(current.getCtx(), MPPCostCollector.Table_Name, "DocStatus='CO' AND PP_Order_ID=?", current.get_TrxName())
				.setParameters(current.getPP_Order_ID())
				.list();
		
		MPPOrder mo = current.getPP_Order();
		BigDecimal totalMO = mo.getQtyEntered();
		BigDecimal deliveredQty = mo.getQtyDelivered();
		BigDecimal deliveredIssue = Env.ZERO;
		BigDecimal amtIssue = Env.ZERO;
		BigDecimal FOHCosts = Env.ZERO;
		
		if(current.isReversal())
			deliveredQty = deliveredQty.add(current.getMovementQty());
		
		for(MPPCostCollector cc :  ccs){
			MCostDetail cd = getCostDetail(cc);
			if(cd==null)
				continue;
			if(cc.getCostCollectorType().equals(MPPCostCollector.COSTCOLLECTORTYPE_ComponentIssue)){
				amtIssue = amtIssue.add(cd.getAmt().abs());
			}else if(cc.getCostCollectorType().equals(MPPCostCollector.COSTCOLLECTORTYPE_MaterialReceipt)){
				if(current.isReversal() && cc.get_ID()==current.getReversal_ID())
					continue;
				
				deliveredIssue = deliveredIssue.add(cd.getAmt().abs());
			}else if(cc.getCostCollectorType().equals(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl)){
				FOHCosts = FOHCosts.add(cd.getAmt().abs());
			}else if(cc.getCostCollectorType().equals(MPPCostCollector.COSTCOLLECTORTYPE_MixVariance)){
				amtIssue = amtIssue.add(cd.getAmt().abs());
			}
		}
		
		if(isVariance){
			return amtIssue.add(FOHCosts).subtract(deliveredIssue);
		}else{
			BigDecimal remainingIssue = amtIssue.add(FOHCosts).subtract(deliveredIssue);
			BigDecimal movementQty = current.getMovementQty().abs();
			BigDecimal percentage = movementQty.divide(totalMO.subtract(deliveredQty), 8, RoundingMode.HALF_EVEN);
			return percentage.multiply(remainingIssue);
		}
	}

	private MCostDetail getCostDetail(MPPCostCollector cc)
	{
		final String whereClause = MCostDetail.COLUMNNAME_PP_Cost_Collector_ID+"=?";
		MCostDetail cd = new Query(cc.getCtx(), MCostDetail.Table_Name, whereClause, cc.get_TrxName())
		.setParameters(new Object[]{cc.getPP_Cost_Collector_ID()})
		.first();
		return cd;
	}

	/**
	 * Create Cost Detail (Material Issue, Material Receipt)
	 * @param model
	 * @param mtrx Material Transaction
	 */
	public void createCostDetail (IDocumentLine model , MTransaction mtrx)
	{
		final MPPCostCollector cc = (model instanceof MPPCostCollector ? (MPPCostCollector)model : null);
		if (Objects.isNull(cc)) return;
		
		for(MAcctSchema as : getAcctSchema(mtrx))
		{
			// Cost Detail
			final MProduct product = MProduct.get(mtrx.getCtx(), mtrx.getM_Product_ID());
			final String costingMethod = product.getCostingMethod(as);
			// Check costing method
			if (!getCostingMethod(product, as.getC_AcctSchema_ID()).equals(costingMethod))
			{
				throw new AdempiereException("创建成本明细时成本方法不匹配 - "+costingMethod);
			}
			//
			for (MCostElement element : getCostElements(mtrx.getCtx(), product, as))
			{
				//
				//	Delete Unprocessed zero Differences
				deleteCostDetail(model, as, element.get_ID(), mtrx.getM_AttributeSetInstance_ID());
				//
				// Get Costs
				BigDecimal qty = mtrx.getMovementQty();
				BigDecimal price = getProductActualCostPrice(cc, product, as, element, mtrx.get_TrxName());
				BigDecimal amt = roundCost(price.multiply(qty), as.getC_AcctSchema_ID());

				// 成本归集单是生产入库, 则计算材料/人工/设备成本
				if (cc.getCostCollectorType().equals(MPPCostCollector.COSTCOLLECTORTYPE_MaterialReceipt)) {
					// 成本要素是材料
					if (element.getCostElementType().equals(MCostElement.COSTELEMENTTYPE_Material)) {
//						amt = calculateFG(cc, element, false);

						MPPProductBOM bom = MPPProductBOM.get(product.getCtx(), MPPProductBOM.getBOMSearchKey(product));
						if (product.isBOM() && Objects.nonNull(bom)) {
							// 递归计算物料BOM的总成本金额
							amt = this.calculateCostFromBOM(cc, element, as, product, bom, 0);
						}
					} else if (MCostElement.COSTELEMENTTYPE_Resource.equals(element.getCostElementType())
							|| MCostElement.COSTELEMENTTYPE_Overhead.equals(element.getCostElementType())) {// 成本要素是资源/制造成本
						// 获取该工单的所有活动控制<生产报工>成本收集器
						List<MPPCostCollector> activities = new Query(cc.getCtx(), MPPCostCollector.Table_Name,
								"PP_Order_ID=? AND CostCollectorType=? AND DocStatus='CO'", cc.get_TrxName())
								.setParameters(new Object[] { cc.getPP_Order_ID(), MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl })
								.list();
						
						// 计算人工/设备成本和工时数量
						amt = this.calculateActivityCostsForReceipt(cc, element, as, activities);
						qty = this.calculateActivityHoursForReceipt(cc, element, as, activities);
					}
				}

				if (cc.isReversal())
					amt = amt.negate();
				//
				// Create / Update Cost Detail
				MCostDetail cd = getCostDetail(model, mtrx ,as, element.get_ID());
				if (cd == null)		//	createNew
				{	
					cd = new MCostDetail (as, mtrx.getAD_Org_ID(), 
							mtrx.getM_Product_ID(), mtrx.getM_AttributeSetInstance_ID(), 
							element.get_ID(),
							amt,
							qty,
							model.getDescription(),
							new Timestamp(System.currentTimeMillis()),
							0,
							mtrx.get_TrxName());
					if(model instanceof MPPCostCollector)
						cd.setPP_Cost_Collector_ID(model.get_ID());
				}
				else
				{
					cd.setDeltaAmt(amt.subtract(cd.getAmt()));
					cd.setDeltaQty(mtrx.getMovementQty().subtract(cd.getQty()));
					if (cd.isDelta())
					{
						cd.setProcessed(false);
						cd.setAmt(amt);
						cd.setQty(mtrx.getMovementQty());
					}
				}
				cd.saveEx();
				processCostDetail(cd);

				// 对于非材料类型的成本要素，确保成本表更新. processCostDetail方法底层逻辑只处理材料成本
				// idempiere核心设计原则:成本核算方法（如平均法、先进先出法、后进先出法等）是库存计价方法，专门用于管理物料在库存流转中的成本计算。不适用于其他类型的成本要素)
				if (!MCostElement.COSTELEMENTTYPE_Material.equals(element.getCostElementType())
						&& cc.getCostCollectorType().equals(MPPCostCollector.COSTCOLLECTORTYPE_MaterialReceipt)) {
					// 手动更新成本记录和成本明细部分字段
					try {
						this.updateNotMaterialCost(product, as, element, amt, qty, mtrx, cd);
					} catch (Exception e) {
						log.severe("手动更新成本记录和成本明细发生异常" + e.getMessage());
					}
				}

				log.config("" + cd);
			} // for ELements	
		} // Account Schema 			
	}

	/**
	 * 更新非材料成本要素的成本记录  - 暂时只支持移动平均和标准成本
	 * @Title: updateNotMaterialCost
	 * @param product
	 * @param as
	 * @param element
	 * @param amt
	 * @param qty
	 * @param mtrx
	 * @return void
	 */
	private void updateNotMaterialCost(MProduct product, MAcctSchema as, MCostElement element, BigDecimal amt,
			BigDecimal qty, MTransaction mtrx, MCostDetail cd) {

		CostDimension d = new CostDimension(product, as, as.getM_CostType_ID(), mtrx.getAD_Org_ID(),
				mtrx.getM_AttributeSetInstance_ID(), element.getM_CostElement_ID());
		MCost cost = d.toQuery(MCost.class, mtrx.get_TrxName()).firstOnly();

		if (cost == null) {
			cost = new MCost(product, mtrx.getM_AttributeSetInstance_ID(), as, mtrx.getAD_Org_ID(),
					element.getM_CostElement_ID());
			cost.saveEx();
		}

		// 根据成本计价方法选择算法
		String method = element.getCostingMethod();
		if (method != null && MCostElement.COSTINGMETHOD_StandardCosting.equals(method)) {
			// 标准成本法
			if (cost.getCurrentCostPrice().signum() == 0) {
				BigDecimal price = qty.signum() != 0 ? amt.divide(qty, 12, RoundingMode.HALF_UP) : Env.ZERO;
				cost.setCurrentCostPrice(price);
			}
			cost.add(amt, qty);
		} else {
			// 其他方法使用移动平均作为默认
			cost.setWeightedAverage(amt, qty);
		}

		cost.saveEx();
		
		// 手动更新成本明细字段
		cd.setCurrentCostPrice(cost.getCurrentCostPrice());
		cd.setCurrentQty(cost.getCurrentQty());
		cd.setCumulatedAmt(cost.getCumulatedAmt());
		cd.setCumulatedQty(cost.getCumulatedQty());
		cd.saveEx();
	}


	// 递归方法中添加深度限制  
	private static final int MAX_BOM_DEPTH = 10;  
	
	/**
	 * 递归计算物料BOM的总成本金额: 实时卷积成品BOM材料总成本 * BOM数量 * 入库数量 = 成品的总材料成本
	 * @Title: calculateMaterialCostFromBOMRecursive
	 * @param cc
	 * @param element
	 * @param as
	 * @param product
	 * @param qty
	 * @return
	 * @return BigDecimal
	 */
	private BigDecimal calculateCostFromBOM(MPPCostCollector cc, MCostElement element, MAcctSchema as,
			MProduct product, MPPProductBOM bom, int depth) {
		if (depth > MAX_BOM_DEPTH) {  
	        log.warning("BOM recursion depth exceeded for product: " + product.getValue());  
	        return Env.ZERO;  
	    }   
		BigDecimal totalCost = Env.ZERO;

		// 遍历BOM行，递归计算组件成本
		for (MPPProductBOMLine bomLine : bom.getLines()) {
			if (bomLine.isCoProduct() || bomLine.isByProduct())
				continue;

			MProduct component = MProduct.get(cc.getCtx(), bomLine.getM_Product_ID());
//			BigDecimal bomQty = bomLine.getQty(true);
			BigDecimal bomQty = bomLine.getQtyBOM(); // 不经过 UOM 精度舍入,只用于金额计算
			BigDecimal componentQty = cc.getMovementQty().multiply(bomQty);

			// 只有当组件是BOM物料时才递归调用本身方法
			if (component.isBOM()) {
				MPPProductBOM componentBom = MPPProductBOM.get(component.getCtx(), MPPProductBOM.getBOMSearchKey(component));
				if (componentBom != null) {
					BigDecimal componentCost = calculateCostFromBOM(cc, element, as, component, componentBom, depth + 1);
					totalCost = totalCost.add(componentCost);
				}
			} else {
				// 非BOM物料直接获取当前成本
				BigDecimal componentCurrentCost = getProductActualCostPrice(cc, component, as, element,
						cc.get_TrxName());
				totalCost = totalCost.add(componentCurrentCost.multiply(componentQty));
			}
		}

		return roundCost(totalCost, as.getC_AcctSchema_ID());
	}

	/**
	 * 根据入库数量按比例计算人工/设备成本金额
	 * 
	 * @Title: calculateActivityCostsForReceipt
	 * @param cc
	 * @param element
	 * @param as
	 * @return
	 * @return BigDecimal
	 */
	private BigDecimal calculateActivityCostsForReceipt(MPPCostCollector cc, MCostElement element, MAcctSchema as, List<MPPCostCollector> activities) {
		if (Objects.isNull(activities) || activities.isEmpty()) {
			return Env.ZERO;
		}

		// 计算入库数量比例分配
		MPPOrder order = cc.getPP_Order();
		BigDecimal totalQty = order.getQtyEntered();
		BigDecimal receiptQty = cc.getMovementQty().abs();
		BigDecimal proportion = totalQty.signum() > 0 ? receiptQty.divide(totalQty, 8, RoundingMode.HALF_EVEN)
				: Env.ZERO;

		// 按照入库数量比例计算当前生产入库的总人工或设备成本金额
		BigDecimal totalActivityCost = activities.stream()
				.map(activity -> getCostDetail(activity, element.getM_CostElement_ID())).filter(Objects::nonNull)
				.map(MCostDetail::getAmt).filter(Objects::nonNull).map(BigDecimal::abs)
				.map(activityAmt -> activityAmt.multiply(proportion)).reduce(Env.ZERO, BigDecimal::add);

		return roundCost(totalActivityCost, as.getC_AcctSchema_ID());
	}
	
	/**
	 * 按照入库数量比例计算报工工时
	 * @Title: calculateActivityHoursForReceipt
	 * @param cc
	 * @param element
	 * @param as
	 * @param activities
	 * @return
	 * @return BigDecimal
	 */
	private BigDecimal calculateActivityHoursForReceipt(MPPCostCollector cc, MCostElement element, MAcctSchema as, List<MPPCostCollector> activities) {
		if (Objects.isNull(activities) || activities.isEmpty()) {
			return Env.ZERO;
		}

		// 计算入库数量比例分配
		MPPOrder order = cc.getPP_Order();
		BigDecimal totalQty = order.getQtyEntered();
		BigDecimal receiptQty = cc.getMovementQty().abs();
		BigDecimal proportion = totalQty.signum() > 0 ? receiptQty.divide(totalQty, 8, RoundingMode.HALF_EVEN)
				: Env.ZERO;

		// 计算工单总实际工时
		BigDecimal totalActualHours = activities.stream().map(MPPCostCollector::getDurationReal)
				.filter(Objects::nonNull).reduce(Env.ZERO, BigDecimal::add);

		// 按照入库数量比例计算当前生产入库的实际工时
		return totalActualHours.multiply(proportion);
	}

	private int deleteCostDetail(IDocumentLine model, MAcctSchema as ,int M_CostElement_ID,
			int M_AttributeSetInstance_ID)
	{
		//	Delete Unprocessed zero Differences
		String sql = "DELETE FROM " + MCostDetail.Table_Name
		+ " WHERE Processed='N' AND COALESCE(DeltaAmt,0)=0 AND COALESCE(DeltaQty,0)=0"
		+ " AND "+model.get_TableName()+"_ID=?" 
		+ " AND "+MCostDetail.COLUMNNAME_C_AcctSchema_ID+"=?" 
		+ " AND "+MCostDetail.COLUMNNAME_M_AttributeSetInstance_ID+"=?"
		//			+ " AND "+MCostDetail.COLUMNNAME_M_CostType_ID+"=?"
		+ " AND "+MCostDetail.COLUMNNAME_M_CostElement_ID+"=?";
		Object[] parameters = new Object[]{ model.get_ID(), 
				as.getC_AcctSchema_ID(), 
				M_AttributeSetInstance_ID,
				//as.getM_CostType_ID(),
				M_CostElement_ID};

		int no =DB.executeUpdateEx(sql,parameters, model.get_TrxName());
		if (no != 0)
			log.config("Deleted #" + no);
		return no;
	}
	
	private void processCostDetail(MCostDetail cd)
	{
		if (!cd.isProcessed())
		{
				cd.process();
		}
	}

	public static boolean isActivityControlElement(I_M_CostElement element)
	{
		String costElementType = element.getCostElementType();
		return MCostElement.COSTELEMENTTYPE_Resource.equals(costElementType)
		|| MCostElement.COSTELEMENTTYPE_Overhead.equals(costElementType)
		|| MCostElement.COSTELEMENTTYPE_BurdenMOverhead.equals(costElementType);
	}

	private Collection<MCostElement> getCostElements(Properties ctx, MProduct product, MAcctSchema schema)
	{
		return MCostElement.getByCostingMethod(ctx, getCostingMethod(product, schema.getC_AcctSchema_ID()));
	}

	private Collection<MCostElement> getAllCostElements(PO po)
	{
		return MCostElement.getCostElementsWithCostingMethods(po);
	}
	
	private Collection<MAcctSchema> getAcctSchema(PO po)
	{
		int AD_Org_ID = po.getAD_Org_ID();
		MAcctSchema[] ass = MAcctSchema.getClientAcctSchema(po.getCtx(), po.getAD_Client_ID());
		ArrayList<MAcctSchema> list = new ArrayList<MAcctSchema>(ass.length);
		for (MAcctSchema as : ass)
		{
			if(!as.isSkipOrg(AD_Org_ID))
				list.add(as);
		}
		return list;
	}
	
	private MCostDetail getCostDetail(MPPCostCollector cc, int M_CostElement_ID)  
	{  
	    final String whereClause = MCostDetail.COLUMNNAME_PP_Cost_Collector_ID+"=?"  
	        +" AND "+MCostDetail.COLUMNNAME_M_CostElement_ID+"=?";  
	    MCostDetail cd = new Query(cc.getCtx(), MCostDetail.Table_Name, whereClause, cc.get_TrxName())  
	        .setParameters(new Object[]{cc.getPP_Cost_Collector_ID(), M_CostElement_ID})  
	        .setOrderBy("Created ASC")   // 按创建时间升序，取最早那条  
	        .first();
	    return cd;  
	}

	private MPPCostCollector createVarianceCostCollector(MPPCostCollector cc, String CostCollectorType)
	{
		MPPCostCollector ccv = new MPPCostCollector(cc.getCtx(), 0, cc.get_TrxName());
		MPPCostCollector.copyValues(cc, ccv);
		ccv.setAD_Org_ID(cc.getAD_Org_ID()); // 设置组织
		ccv.setProcessing(false);
		ccv.setProcessed(false);
		ccv.setDocStatus(MPPCostCollector.STATUS_Drafted);
		ccv.setDocAction(MPPCostCollector.ACTION_Complete);
		ccv.setCostCollectorType(CostCollectorType);
		ccv.setDocumentNo(null); // reset
		ccv.saveEx();
		return ccv;
	}
	
	/**
	 * Create & Proce Cost Detail for Variances
	 * @param ccv
	 * @param amt
	 * @param qty
	 * @param cd (optional)
	 * @param product
	 * @param as
	 * @param element
	 * @return
	 */
	private MCostDetail createVarianceCostDetail(MPPCostCollector ccv, BigDecimal amt, BigDecimal qty,
			MCostDetail cd, MProduct product, MAcctSchema as, MCostElement element)
	{
		final MCostDetail cdv = new MCostDetail(ccv.getCtx(), 0, ccv.get_TrxName());
		if (cd != null)
		{
			MCostDetail.copyValues(cd, cdv);
			cdv.setProcessed(false);
		}
		if (product != null)
		{
			cdv.setM_Product_ID(product.getM_Product_ID());
			cdv.setM_AttributeSetInstance_ID(0);
		}
		if (as != null)
		{
			cdv.setC_AcctSchema_ID(as.getC_AcctSchema_ID());
		}
		if (element != null)
		{
			cdv.setM_CostElement_ID(element.getM_CostElement_ID());
		}
		//
		cdv.setPP_Cost_Collector_ID(ccv.getPP_Cost_Collector_ID());
		cdv.setAmt(amt);
		cdv.setQty(qty);
		cdv.saveEx();
		processCostDetail(cdv);
		return cdv;
	}
	
	public void createActivityControl(MPPCostCollector cc)
	{
		if (!cc.isCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl))
			return;
		//
		final MProduct product = MProduct.forS_Resource_ID(cc.getCtx(), cc.getS_Resource_ID(), null);
		final RoutingService routingService = RoutingServiceFactory.get().getRoutingService(cc.getAD_Client_ID());
		final BigDecimal qty = routingService.getResourceBaseValue(cc.getS_Resource_ID(), cc);
		for (MAcctSchema as : getAcctSchema(cc))
		{
			for (MCostElement element : getCostElements(cc.getCtx(), product, as))
			{
				if (!isActivityControlElement(element))
				{
					continue;
				}
				final CostDimension d = new CostDimension(product,
						as,
						as.getM_CostType_ID(),
						cc.getAD_Org_ID(), //AD_Org_ID,
						product.getM_AttributeSetInstance_ID(), //M_ASI_ID
						element.getM_CostElement_ID());
				final BigDecimal price = getResourceActualCostRate(cc, cc.getS_Resource_ID(), d, cc.get_TrxName());
				BigDecimal costs = price.multiply(qty);
				if (costs.scale() > as.getCostingPrecision())
					costs = costs.setScale(as.getCostingPrecision(), RoundingMode.HALF_UP);
				//
				MCostDetail cd = new MCostDetail(as,
						cc.getAD_Org_ID(), //AD_Org_ID,
						d.getM_Product_ID(),
						product.getM_AttributeSetInstance_ID(), // M_AttributeSetInstance_ID,
						element.getM_CostElement_ID(),
						costs.negate(),
						qty.negate(),
						"", // Description,
						new Timestamp(System.currentTimeMillis()),
						0,
						cc.get_TrxName());
				cd.setPP_Cost_Collector_ID(cc.getPP_Cost_Collector_ID());
				cd.saveEx();
				processCostDetail(cd);
			}
		}
		// ===== 班组直接人工分录 =====
		createDirectLaborCostDetails(cc);
	}

	/**
	 * 直接人工成本模块：为本次报工单班组下"直接人工=Y"的每个成员， 各生成一条 M_CostDetail（后续过账走现有
	 * Doc_PPCostCollector.createFacts() 逻辑，
	 * 借：P_WIP_Acct，贷：P_Labor_Acct，与设备资源分录复用同一套记账映射）。
	 *
	 * 前提：只处理生产报工(CostCollectorType=160)，不处理非生产报工(161)、领料、入库等其它类型。
	 */
	private void createDirectLaborCostDetails(MPPCostCollector cc) {
		if (!cc.isCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl))
			return;

		int workTeamId = cc.get_ValueAsInt("C_WorkTeam_ID");
		if (workTeamId <= 0)
			return; // 没有班组信息，不生成直接人工分录

		// 只取"直接人工=Y"的班组成员
		List<org.libero.tables.X_C_WorkTeamMember> members = new Query(cc.getCtx(),
				org.libero.tables.X_C_WorkTeamMember.Table_Name,
				"C_WorkTeam_ID=? AND IsDirectLabor='Y' AND IsActive='Y'", cc.get_TrxName()).setParameters(workTeamId)
				.setOrderBy("AD_User_ID") // 固定顺序，保证所有并发事务按同一顺序加锁，消除死锁
				.list();
		if (members.isEmpty())
			return;

		for (org.libero.tables.X_C_WorkTeamMember member : members) {
			int userId = member.getAD_User_ID();
			if (userId <= 0)
				continue;

			// 人员 -> 资源（S_Resource.AD_User_ID=userId 且 S_ResourceType为人力资源）
			int resourceId = getResourceIdByUser(cc.getAD_Client_ID(), userId);
			if (resourceId <= 0) {
				log.warning("未找到用户 AD_User_ID=" + userId + " 对应的人力资源(S_Resource)，跳过直接人工分录生成");
				continue;
			}

			// 同一员工同时只能有一个报工操作在处理：对该员工的人力资源行加行级锁，
			// 保证并发报工时，"当月已报工工时"的读取+截断计算不会被并发写入干扰。
			lockResourceForUpdate(resourceId, cc.get_TrxName());

			// 资源 -> 产品
			final MProduct product = MProduct.forS_Resource_ID(cc.getCtx(), resourceId, null);
			if (product == null) {
				log.warning("未找到 S_Resource_ID=" + resourceId + " 对应的产品，跳过直接人工分录生成");
				continue;
			}

			// 本次可计入工时：按月工时上限截断，仅在内存计算，不落库、不阻断单据完成
			BigDecimal qty = calcCreditableHoursForUser(cc, userId);
			if (qty.signum() <= 0)
				continue; // 本月已无可计入工时

			for (MAcctSchema as : getAcctSchema(cc)) {
				for (MCostElement element : getCostElements(cc.getCtx(), product, as)) {
					if (!isActivityControlElement(element))
						continue;

					final CostDimension d = new CostDimension(product, as, as.getM_CostType_ID(), cc.getAD_Org_ID(),
							product.getM_AttributeSetInstance_ID(), element.getM_CostElement_ID());
					// 人员 -> 资源 -> 产品 -> 产品成本（M_Cost.CurrentCostPrice）
					final BigDecimal price = getResourceActualCostRate(cc, resourceId, d, cc.get_TrxName());
					BigDecimal costs = price.multiply(qty);
					if (costs.scale() > as.getCostingPrecision())
						costs = costs.setScale(as.getCostingPrecision(), RoundingMode.HALF_UP);

					MCostDetail cd = new MCostDetail(as, cc.getAD_Org_ID(), d.getM_Product_ID(),
							product.getM_AttributeSetInstance_ID(), element.getM_CostElement_ID(), costs.negate(),
							qty.negate(), "直接人工-" + org.compiere.model.MUser.getNameOfUser(userId),
							new Timestamp(System.currentTimeMillis()), 0, cc.get_TrxName());
					cd.setPP_Cost_Collector_ID(cc.getPP_Cost_Collector_ID());
					cd.saveEx();
					processCostDetail(cd);
				}
			}
		}
	}

	/**
	 * 按 AD_User_ID 查该员工绑定的人力资源(S_Resource)。 匹配条件：S_Resource.AD_User_ID=userId 且 关联的
	 * S_ResourceType.Value=系统配置 HF_HR_RESOURCE_TYPE_VALUE（默认"1000000"） 。 找不到返回 -1。
	 */
	private int getResourceIdByUser(int AD_Client_ID, int AD_User_ID) {
		String hrResourceTypeValue = MSysConfig.getValue("HF_HR_RESOURCE_TYPE_VALUE", "1000000", AD_Client_ID);
		String sql = "SELECT r.S_Resource_ID FROM S_Resource r "
				+ "JOIN S_ResourceType rt ON rt.S_ResourceType_ID = r.S_ResourceType_ID "
				+ "WHERE r.AD_Client_ID=? AND r.AD_User_ID=? AND rt.Value=? AND r.IsActive='Y'";
		return DB.getSQLValueEx(null, sql, AD_Client_ID, AD_User_ID, hrResourceTypeValue);
	}

	/**
	 * 对某个人力资源行加行级锁，防止同一员工的多个报工单并发处理时， "读取当月已报工工时 -> 计算可计入工时 -> 生成分录"这一段出现并发脏读。
	 */
	private void lockResourceForUpdate(int S_Resource_ID, String trxName) {
		String sql = "SELECT S_Resource_ID FROM S_Resource WHERE S_Resource_ID=? FOR UPDATE";
		DB.getSQLValueEx(trxName, sql, S_Resource_ID);
	}

	/**
	 * 计算某员工本次报工单可计入直接人工成本的有效工时。 举例：已报工258h，本次报工3h，monthlyLimit=260 →
	 * 剩余=2，min(3,2)=2。
	 */
	private BigDecimal calcCreditableHoursForUser(MPPCostCollector cc, int userId) {
		BigDecimal currentDuration = cc.getDurationReal();
		BigDecimal monthlyLimit = HFSysConfigEnum.HF_MONTHLY_WORK_HOURS.getBigDecimalValue(cc.getAD_Client_ID());

		String sql = "SELECT COALESCE(SUM(cc.DurationReal), 0) " + "FROM PP_Cost_Collector cc "
				+ "JOIN C_WorkTeamMember wtm ON wtm.C_WorkTeam_ID = cc.C_WorkTeam_ID " + "WHERE wtm.AD_User_ID = ? "
				+ "  AND wtm.IsActive = 'Y' " + "  AND cc.DocStatus = ? " + "  AND cc.PP_Cost_Collector_ID <> ? "
				+ "  AND cc.MovementDate >= date_trunc('month', ?::timestamp) "
				+ "  AND cc.MovementDate < date_trunc('month', ?::timestamp) + interval '1 month'";

		BigDecimal reportedHours = DB.getSQLValueBD(cc.get_TrxName(), sql, userId, MPPCostCollector.DOCSTATUS_Completed,
				cc.get_ID(), cc.getMovementDate(), cc.getMovementDate());
		if (reportedHours == null)
			reportedHours = BigDecimal.ZERO;

		BigDecimal remaining = monthlyLimit.subtract(reportedHours);
		if (remaining.compareTo(BigDecimal.ZERO) < 0)
			remaining = BigDecimal.ZERO;

		// 可计入时长 = min(本次报工时长, 剩余可报工时长)
		BigDecimal creditable = currentDuration.compareTo(remaining) > 0 ? remaining : currentDuration;

		if (creditable.compareTo(currentDuration) < 0) {
			log.warning("@" + MUser.getNameOfUser(userId) + "(AD_User_ID=" + userId + ")（已报工" + reportedHours
					+ "小时，当前报工" + currentDuration + "小时），员工当月累计报工时间不可超过" + monthlyLimit + "小时，多余"
					+ currentDuration.subtract(creditable) + "小时不生成分录");
		}
		return creditable;
	}
	
	public void createUsageVariances(MPPCostCollector ccuv)
	{
		// Apply only for material Usage Variance
		if (!ccuv.isCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_UsegeVariance))
		{
			throw new IllegalArgumentException("Cost Collector is not Material Usage Variance");
		}
		//
		final MProduct product;
		final BigDecimal qty;
		if (ccuv.getPP_Order_BOMLine_ID() > 0)
		{
			product = MProduct.get(ccuv.getCtx(), ccuv.getM_Product_ID());
			qty = ccuv.getMovementQty();
		}
		else
		{
			product = MProduct.forS_Resource_ID(ccuv.getCtx(), ccuv.getS_Resource_ID(), null);
			final RoutingService routingService = RoutingServiceFactory.get().getRoutingService(ccuv.getAD_Client_ID());
			qty = routingService.getResourceBaseValue(ccuv.getS_Resource_ID(), ccuv);
		}
		
		// 如果产品为空，跳过差异计算
		if (Objects.isNull(product)) return;
		
		//
		for(MAcctSchema as : getAcctSchema(ccuv))
		{
			for (MCostElement element : getCostElements(ccuv.getCtx(), product, as))
			{
				final BigDecimal price = getProductActualCostPrice(ccuv, product, as, element, ccuv.get_TrxName());
				final BigDecimal amt = roundCost(price.multiply(qty), as.getC_AcctSchema_ID());
				//
				// Create / Update Cost Detail
				createVarianceCostDetail(ccuv,
						amt, qty,
						null, // no original cost detail
						product,
						as,
						element);
			} // for ELements	
		} // Account Schema 			
	}
	
	public void createRateVariances(MPPCostCollector cc)  
	{  
		final MProduct product;  
		if (cc.isCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl))  
		{  
			// 优化：直接从 PP_Order_Node 自身读取 S_Resource_ID，不再反查 AD_WF_Node  
			product = MProduct.forS_Resource_ID(cc.getCtx(), cc.getPP_Order_Node().getS_Resource_ID(), null);  
		}  
		else if (cc.isCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_ComponentIssue))  
		{  
			final I_PP_Order_BOMLine bomLine = cc.getPP_Order_BOMLine();  
			product = MProduct.get(cc.getCtx(), bomLine.getM_Product_ID());  
		}  
		else  
		{  
			return;  
		}  
	  
		// 如果产品为空，跳过差异计算  
		if (Objects.isNull(product)) return;  
		  
		MPPCostCollector ccrv = null; // Cost Collector - Rate Variance  
		for (MAcctSchema as : getAcctSchema(cc))  
		{  
			for (MCostElement element : getCostElements(cc.getCtx(), product, as))  
			{  
				final MCostDetail cd = getCostDetail(cc, element.getM_CostElement_ID());  
				if (cd == null)  
					continue;  
				//  
				final BigDecimal qty = cd.getQty();  
				final BigDecimal priceStd = getProductStandardCostPrice(cc, product, as, element);  
				final BigDecimal priceActual = getProductActualCostPrice(cc, product, as, element, cc.get_TrxName());  
				final BigDecimal amtStd = roundCost(priceStd.multiply(qty), as.getC_AcctSchema_ID());  
				final BigDecimal amtActual = roundCost(priceActual.multiply(qty), as.getC_AcctSchema_ID());  
				if (amtStd.compareTo(amtActual) == 0)  
					continue;  
				//  
				if (ccrv == null)  
				{  
					ccrv = createVarianceCostCollector(cc, MPPCostCollector.COSTCOLLECTORTYPE_RateVariance);  
				}  
				//  
				createVarianceCostDetail(ccrv,  
						amtActual.negate(), qty.negate(),  
						cd, null, as, element);  
				createVarianceCostDetail(ccrv,  
						amtStd, qty,  
						cd, null, as, element);  
			}  
		}  
		//  
		if (ccrv != null)  
		{  
			boolean ok = ccrv.processIt(MPPCostCollector.ACTION_Complete);  
			ccrv.saveEx();  
			if (!ok)  
				throw new AdempiereException(ccrv.getProcessMsg());  
		}  
	}
	public void createMethodVariances(MPPCostCollector cc)  
	{  
		if(cc.isCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_MethodChangeVariance))  
		{		  
			for (MAcctSchema as : getAcctSchema(cc))  
			{  
			  
				for (MCostElement element : getCostElements(cc.getCtx(), cc.getM_Product(), as))  
				{  
					final MProduct product = cc.getM_Product();   
					final BigDecimal qty = cc.getMovementQty();  
					final BigDecimal priceStd = getProductActualCostPrice(cc, product, as, element, cc.get_TrxName());  
					final BigDecimal amtStd = priceStd.multiply(qty);  
					createVarianceCostDetail(cc,  
							amtStd,qty,  
							null, product, as, element);  
				}  
			}  
			return;  
		}  
		//create the variance for routing	  
		if (!cc.isCostCollectorType(MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl))  
			return;  
		//  
		// 优化：直接从 PP_Order_Node 自身读取 S_Resource_ID，不再反查 AD_WF_Node  
		final int std_resource_id = cc.getPP_Order_Node().getS_Resource_ID();  
		final int actual_resource_id = cc.getS_Resource_ID();  
		if (std_resource_id == actual_resource_id)  
		{  
			return;  
		}  
		// 添加空值检查 - 如果任一资源id为空，跳过差异计算    
		if (std_resource_id <= 0 || actual_resource_id <= 0) {    
		    return;   
		}  
		//  
		MPPCostCollector ccmv = null; // Cost Collector - Method Change Variance  
		final RoutingService routingService = RoutingServiceFactory.get().getRoutingService(cc.getAD_Client_ID());  
		for (MAcctSchema as : getAcctSchema(cc))  
		{  
			for (MCostElement element : getCostElements(cc.getCtx(), cc.getM_Product(), as))  
			{  
				final MProduct resourcePStd = MProduct.forS_Resource_ID(cc.getCtx(), std_resource_id, null);   
				final MProduct resourcePActual = MProduct.forS_Resource_ID(cc.getCtx(), actual_resource_id, null);  
				final BigDecimal priceStd = getProductActualCostPrice(cc, resourcePStd, as, element, cc.get_TrxName());  
				final BigDecimal priceActual = getProductActualCostPrice(cc, resourcePActual, as, element, cc.get_TrxName());  
				if (priceStd.compareTo(priceActual) == 0)  
				{  
					continue;  
				}  
				//  
				if (ccmv == null)  
				{  
					ccmv = createVarianceCostCollector(cc, MPPCostCollector.COSTCOLLECTORTYPE_MethodChangeVariance);  
				}  
				//  
				final BigDecimal qty = routingService.getResourceBaseValue(cc.getS_Resource_ID(), cc);  
				final BigDecimal amtStd = priceStd.multiply(qty);   
				final BigDecimal amtActual = priceActual.multiply(qty);  
				//  
				createVarianceCostDetail(ccmv,  
						amtActual, qty,  
						null, resourcePActual, as, element);  
				createVarianceCostDetail(ccmv,  
						amtStd.negate(), qty.negate(),  
						null, resourcePStd, as, element);  
			}  
		}  
		//  
		if (ccmv != null)  
		{  
			boolean ok = ccmv.processIt(MPPCostCollector.ACTION_Complete);  
			ccmv.saveEx();  
			if (!ok)  
				throw new AdempiereException(ccmv.getProcessMsg());  
		}  
	}
}
