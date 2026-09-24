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
 * Contributor(s): Victor Perez www.e-evolution.com                           *
 *                 Teo Sarca, www.arhipac.ro                                  *
 *****************************************************************************/
package org.libero.model;


import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DocTypeNotFoundException;
import org.adempiere.exceptions.FillMandatoryException;
import org.adempiere.exceptions.NoVendorForProductException;
import org.adempiere.model.engines.CostEngineFactory;
import org.adempiere.model.engines.IDocumentLine;
import org.adempiere.model.engines.StorageEngine;
import org.compiere.model.I_C_UOM;
import org.compiere.model.MBPartner;
import org.compiere.model.MClientInfo;
import org.compiere.model.MCostDetail;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MPeriod;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPO;
import org.compiere.model.MResource;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTransaction;
import org.compiere.model.MUOM;
import org.compiere.model.MUser;
import org.compiere.model.MWarehouse;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.Query;
import org.compiere.print.ReportEngine;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.TimeUtil;
import org.libero.exceptions.ActivityProcessedException;
import org.libero.tables.I_PP_Cost_Collector;
import org.libero.tables.X_C_WorkTeamMember;
import org.libero.tables.X_PP_Cost_Collector;

import com.hoifu.enums.HFSysConfigEnum;
import com.hoifu.utils.WeChatRobotUtils;

/**
 *	PP Cost Collector Model
 *	
 *  @author victor.perez@e-evolution.com, e-Evolution http://www.e-evolution.com
 *			<li> Original contributor of Manufacturing Standard Cost
 * 			<li> FR [ 2520591 ] Support multiples calendar for Org 
 *			@see http://sourceforge.net/tracker2/?func=detail&atid=879335&aid=2520591&group_id=176962 
 *  
 *  @author Teo Sarca, www.arhipac.ro 
 *  @version $Id: MPPCostCollector.java,v 1.1 2004/06/19 02:10:34 vpj-cd Exp $
 */
public class MPPCostCollector extends X_PP_Cost_Collector implements DocAction , IDocumentLine
{
	private static final long serialVersionUID = 1L;
	
	// 在现有常量后添加
	public static final String COSTCOLLECTORTYPE_SUBCONTRACTING_ISSUE = "131"; // 委外发料
	public static final String COSTCOLLECTORTYPE_SUBCONTRACTING_RETURN = "132"; // 委外退料
	public static final String COSTCOLLECTORTYPE_SUBCONTRACTING_REPLENISHMENT = "133"; // 委外补领

	// 报工类型（CostCollectorType=160 的子类型）
	public static final String HF_WORKREPORTTYPE_Setup    = "ZJB"; // 装校版

	public static final String HF_WORKREPORTTYPE_Produce  = "SC";  // 生产报工

	public static final String HF_WORKREPORTTYPE_WaitMat  = "DL";  // 生产待料

	private boolean isReversal = false;
	
    /**
     * Create & Complete Cost Collector 
     * @param order
     * @param M_Product_ID
     * @param M_Locator_ID
     * @param M_AttributeSetInstance_ID
     * @param S_Resource_ID
     * @param PP_Order_BOMLine_ID
     * @param PP_Order_Node_ID
     * @param C_DocType_ID
     * @param CostCollectorType
     * @param movementdate
     * @param qty
     * @param scrap
     * @param reject
     * @param durationSetup
     * @param duration
     * @param trxName
     * @return completed cost collector
     */
	/**
	 * 创建成本收集器
	 * @param immediateComplete 是否立即完成（过账），如果为false则创建为草稿状态
	 */
	public static MPPCostCollector createCollector (MPPOrder order,
	        int M_Product_ID,
	        int M_Locator_ID,
	        int M_AttributeSetInstance_ID,
	        int S_Resource_ID,
	        int PP_Order_BOMLine_ID,
	        int PP_Order_Node_ID,
	        int C_DocType_ID,
	        String CostCollectorType,
	        Timestamp movementdate,
	        BigDecimal qty,
	        BigDecimal scrap,
	        BigDecimal reject,
	        int durationSetup,
	        BigDecimal duration,
	        boolean immediateComplete
	    )
	{
	    MPPCostCollector cc = new MPPCostCollector(order);
	    cc.setPP_Order_BOMLine_ID(PP_Order_BOMLine_ID);
	    cc.setPP_Order_Node_ID(PP_Order_Node_ID);
	    cc.setC_DocType_ID(C_DocType_ID);
	    cc.setC_DocTypeTarget_ID(C_DocType_ID);
	    cc.setCostCollectorType(CostCollectorType);
	    
	    // 根据immediateComplete参数设置不同的初始状态
	    if (immediateComplete) {
	        cc.setDocAction(MPPCostCollector.DOCACTION_Complete);
	        cc.setDocStatus(MPPCostCollector.DOCSTATUS_Drafted);
	    } else {
	        // 设置为草稿状态，不立即完成
	        cc.setDocAction(MPPCostCollector.DOCACTION_Prepare);
	        cc.setDocStatus(MPPCostCollector.DOCSTATUS_Drafted);
	    }
	    
	    cc.setIsActive(true);
	    cc.setM_Locator_ID(M_Locator_ID);
	    cc.setM_AttributeSetInstance_ID(M_AttributeSetInstance_ID);
	    cc.setS_Resource_ID(S_Resource_ID);
	    cc.setMovementDate(movementdate);
	    cc.setDateAcct(movementdate);
	    cc.setMovementQty(qty);
	    cc.setScrappedQty(scrap);
	    cc.setQtyReject(reject);
	    cc.setSetupTimeReal(new BigDecimal(durationSetup));
	    cc.setDurationReal(duration);
	    cc.setPosted(false);
	    cc.setProcessed(false);
	    cc.setProcessing(false);
	    cc.setUser1_ID(order.getUser1_ID());
	    cc.setUser2_ID(order.getUser2_ID());
	    cc.setM_Product_ID(M_Product_ID);
	    if(PP_Order_Node_ID > 0)
	    {   
	        cc.setIsSubcontracting(PP_Order_Node_ID);
	    }
	    // If this is an material issue, we should use BOM Line's UOM
	    if (PP_Order_BOMLine_ID > 0)
	    {
	        cc.setC_UOM_ID(0); // we set the BOM Line UOM on beforeSave
	    }
	    cc.saveEx(order.get_TrxName());
	    
	    if (immediateComplete) {
	        if (!cc.processIt(MPPCostCollector.DOCACTION_Complete))
	        {
	            throw new AdempiereException(cc.getProcessMsg());
	        }
	        cc.saveEx(order.get_TrxName());
	    }
	    
	    return cc;
	}

	// 保持原有的方法，默认立即完成（为了向后兼容）
	public static MPPCostCollector createCollector (MPPOrder order,
	        int M_Product_ID,
	        int M_Locator_ID,
	        int M_AttributeSetInstance_ID,
	        int S_Resource_ID,
	        int PP_Order_BOMLine_ID,
	        int PP_Order_Node_ID,
	        int C_DocType_ID,
	        String CostCollectorType,
	        Timestamp movementdate,
	        BigDecimal qty,
	        BigDecimal scrap,
	        BigDecimal reject,
	        int durationSetup,
	        BigDecimal duration
	    )
	{
	    return createCollector(order, M_Product_ID, M_Locator_ID, M_AttributeSetInstance_ID, S_Resource_ID,
	            PP_Order_BOMLine_ID, PP_Order_Node_ID, C_DocType_ID, CostCollectorType, movementdate, qty, scrap, reject,
	            durationSetup, duration, true); // 默认立即完成
	}
	
	public static void setPP_Order(I_PP_Cost_Collector cc, MPPOrder order)
	{
		cc.setPP_Order_ID(order.getPP_Order_ID());
		cc.setPP_Order_Workflow_ID(order.getMPPOrderWorkflow().get_ID());
		cc.setAD_Org_ID(order.getAD_Org_ID());
		cc.setM_Warehouse_ID(order.getM_Warehouse_ID());
		cc.setAD_OrgTrx_ID(order.getAD_OrgTrx_ID());
		cc.setC_Activity_ID(order.getC_Activity_ID());
		cc.setC_Campaign_ID(order.getC_Campaign_ID());
		cc.setC_Project_ID(order.getC_Project_ID());
		cc.setDescription(order.getDescription());
		cc.setS_Resource_ID(order.getS_Resource_ID());
		cc.setM_Product_ID(order.getM_Product_ID());
		cc.setC_UOM_ID(order.getC_UOM_ID());
		cc.setM_AttributeSetInstance_ID(order.getM_AttributeSetInstance_ID());
		cc.setMovementQty(order.getQtyOrdered());
	}
	


	/**
	 * 	Standard Constructor
	 *	@param ctx context
	 *	@param PP_Cost_Collector id
	 */
	public MPPCostCollector(Properties ctx, int PP_Cost_Collector_ID, String trxName)
	{
		super (ctx, PP_Cost_Collector_ID,trxName);
		if (PP_Cost_Collector_ID == 0)
		{
			//setC_DocType_ID(0);
			setDocStatus (DOCSTATUS_Drafted);	// DR
			setDocAction (DOCACTION_Complete);	// CO
			setMovementDate (new Timestamp(System.currentTimeMillis()));	// @#Date@
			setIsActive(true);
			setPosted (false);
			setProcessing (false);
			setProcessed (false);
		}	
	}	//	MPPCostCollector

	/**
	 * 	Load Constructor
	 *	@param ctx context
	 *	@param rs result set
	 */
	public MPPCostCollector(Properties ctx, ResultSet rs,String trxName)
	{
		super (ctx, rs, trxName);
	}	//	MPPCostCollector
	
	/**
	 * 	Load Constructor
	 *	@param MPPOrder
	 */
	public MPPCostCollector(MPPOrder order)
	{
		this(order.getCtx(), 0 , order.get_TrxName());
		setPP_Order(this, order);
		m_order = order;	
	}	//	MPPCostCollector


	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else
			setDescription(desc + " | " + description);
	}	//	addDescription
	
	
	public void setC_DocTypeTarget_ID(String docBaseType)
	{
		MDocType[] doc = MDocType.getOfDocBaseType(getCtx(), docBaseType);	

		if (doc == null || doc.length == 0) {
			throw new DocTypeNotFoundException(docBaseType, "");
		}
		else {
			setC_DocTypeTarget_ID(doc[0].get_ID());
		}
	}

//	@Override
	public void setProcessed (boolean processed)
	{
		super.setProcessed (processed);
		if (get_ID() == 0)
			return;
		final String sql = "UPDATE PP_Cost_Collector SET Processed=? WHERE PP_Cost_Collector_ID=?";
		int noLine = DB.executeUpdateEx(sql, new Object[]{processed, get_ID()}, get_TrxName());
		log.fine("setProcessed - " + processed + " - Lines=" + noLine);
	}	//	setProcessed


//	@Override
	public boolean processIt (String processAction)
	{
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}	//	processIt

	/**	Process Message 			*/
	private String		m_processMsg = null;
	/**	Just Prepared Flag			*/
	private boolean		m_justPrepared = false;
	
	/** Manufacturing Order **/
	private MPPOrder m_order = null;
	
	/** Manufacturing Order Activity **/
	private MPPOrderNode m_orderNode = null;
	
	/** Manufacturing Order BOM Line **/
	private MPPOrderBOMLine m_bomLine = null;

//	@Override
	public boolean unlockIt()
	{
		log.info("unlockIt - " + toString());
		setProcessing(false);
		return true;
	}	//	unlockIt

//	@Override
	public boolean invalidateIt()
	{
		log.info("invalidateIt - " + toString());
		setDocAction(DOCACTION_Prepare);
		return true;
	}	//	invalidateIt

//	@Override
	public String prepareIt()
	{

		// ===== 直接人工成本模块：报工单完成前的业务规则校验 =====
		// 只限制"生产报工"单据(CostCollectorType=160，即 isActivityControl() 为 true)，
		// 不能影响生产入库(100)、领料(110)等其它用途的 PP_Cost_Collector 单据
		if (isActivityControl()) {
			boolean directLaborCostCheckEnabled = HFSysConfigEnum.HF_ENABLE_DIRECT_LABOR_COST_CHECK
					.getBooleanValue(getAD_Client_ID());
			log.warning("PP_Cost_Collector_ID=" + get_ID() + " 直接人工成本校验开关(HF_ENABLE_DIRECT_LABOR_COST_CHECK)读取结果="
					+ (directLaborCostCheckEnabled ? "开启" : "关闭"));
			if (directLaborCostCheckEnabled) {
				checkDirectLaborResourceCost();
			}
			checkPPOrderNotClosed();
			checkMonthlyWorkHours();
		}
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}
		
		MPeriod.testPeriodOpen(getCtx(), getDateAcct(), getC_DocTypeTarget_ID(), getAD_Org_ID());
		//	Convert/Check DocType
		setC_DocType_ID(getC_DocTypeTarget_ID());
		
		//
		// Operation Activity
		if(isActivityControl())
		{
			MPPOrderNode activity = getPP_Order_Node();
			if(MPPOrderNode.DOCACTION_Complete.equals(activity.getDocStatus()))
			{	
				throw new ActivityProcessedException(activity);
			}
			
			if (activity.isSubcontracting())
			{
				if(MPPOrderNode.DOCSTATUS_InProgress.equals(activity.getDocStatus())
						&& MPPCostCollector.DOCSTATUS_InProgress.equals(getDocStatus()))
				{			
					return MPPOrderNode.DOCSTATUS_InProgress;
				}
				else if(MPPOrderNode.DOCSTATUS_InProgress.equals(activity.getDocStatus())
						&& MPPCostCollector.DOCSTATUS_Drafted.equals(getDocStatus()))
				{
					throw new ActivityProcessedException(activity);
				}				
				m_processMsg = createPO(activity);
				m_justPrepared = false;
				activity.setInProgress(this);
				activity.saveEx(get_TrxName());
				return DOCSTATUS_InProgress;
			}
			
			activity.setInProgress(this);
			activity.setQtyDelivered(activity.getQtyDelivered().add(getMovementQty()));
			activity.setQtyScrap(activity.getQtyScrap().add(getScrappedQty()));
			activity.setQtyReject(activity.getQtyReject().add(getQtyReject()));
			activity.setDurationReal(activity.getDurationReal().add(getDurationReal()));
			activity.setSetupTimeReal(activity.getSetupTimeReal().add(getSetupTimeReal()));
			activity.saveEx(get_TrxName());

			// report all activity previews to milestone activity
			if(activity.isMilestone())
			{
				MPPOrderWorkflow order_workflow = activity.getMPPOrderWorkflow();
				order_workflow.closeActivities(activity, getMovementDate(), true);
			}
		}
		// Issue
		else if (isIssue())
		{
			MProduct product = getM_Product();
//			if (getM_AttributeSetInstance_ID() == 0 && product.isASIMandatory(false))
//			{
//				throw new AdempiereException("@M_AttributeSet_ID@ @IsMandatory@ @M_Product_ID@=" + product.getValue());
//			}
		}
		// Receipt
		else if (isReceipt())
		{
			MProduct product = getM_Product();
//			if (getM_AttributeSetInstance_ID() == 0 && product.isASIMandatory(true))
//			{
//				throw new AdempiereException("@M_AttributeSet_ID@ @IsMandatory@ @M_Product_ID@=" + product.getValue());
//			}
		}
		
		m_justPrepared = true;
		setDocAction(DOCACTION_Complete);
		
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
		{
			return DocAction.STATUS_Invalid;
		}

		return DocAction.STATUS_InProgress;
	}	//	prepareIt

	/**
	 * 直接人工成本模块：报工单完成前校验—— 班组内每个"直接人工=Y"的成员，必须： 1) 已绑定人力资源 2)
	 * 该人力资源对应的产品，在"直接资源"成本要素下 已维护 M_Cost.CurrentCostPrice（且 > 0）。
	 * 任一条件不满足，直接拦截报工单完成，并在提示里汇总列出具体是哪些员工、缺哪一项。
	 *若班组成员成本未维护，则企微机器人通知财务
	 */
	private void checkDirectLaborResourceCost() {
		int workTeamId = get_ValueAsInt("C_WorkTeam_ID");
		if (workTeamId <= 0)
			return; // 没有班组信息，不涉及直接人工，跳过

		String hrResourceTypeValue = HFSysConfigEnum.HF_HR_RESOURCE_TYPE_VALUE.getValue(getAD_Client_ID());
		int laborCostElementId = HFSysConfigEnum.HF_DIRECT_LABOR_COST_ELEMENT_ID.getIntValue(getAD_Client_ID());

		// 当前报工单实际使用的会计科目表ID，M_Cost 的查询必须限定在这个维度内
		int acctSchemaId = MClientInfo.get(getCtx(), getAD_Client_ID()).getC_AcctSchema1_ID();

		// 一条SQL: 找出班组内"直接人工=Y"的成员，缺资源 或 缺成本价格(>0) 的
		final String sql = "SELECT u.Name AS UserName, " + "       p.Value AS ProductValue, "
				+ "       r.S_Resource_ID, " + "       COALESCE(mc.CurrentCostPrice, 0) AS CurrentCostPrice "
				+ "FROM C_WorkTeamMember wtm " + "JOIN AD_User u ON u.AD_User_ID = wtm.AD_User_ID "
				+ "LEFT JOIN S_Resource r ON r.AD_User_ID = u.AD_User_ID " + "     AND r.S_ResourceType_ID IN ( "
				+ "         SELECT rt.S_ResourceType_ID FROM S_ResourceType rt "
				+ "         WHERE rt.Value=? AND rt.AD_Client_ID=? " + "     ) "
				+ "LEFT JOIN M_Product p ON p.S_Resource_ID = r.S_Resource_ID "
				+ "LEFT JOIN M_Cost mc ON mc.M_Product_ID = p.M_Product_ID " + "     AND mc.M_CostElement_ID = ? "
				+ "     AND mc.C_AcctSchema_ID = ? " + "WHERE wtm.C_WorkTeam_ID = ? " + "  AND wtm.IsDirectLabor = 'Y' "
				+ "  AND wtm.IsActive = 'Y'";

		// UI报错：只存姓名；企微通知：存"编码 姓名"。两者分开维护，互不兜底覆盖
		Set<String> noResourceUsers = new LinkedHashSet<>();
		Set<String> noCostUserNames = new LinkedHashSet<>(); // 用于UI报错
		Set<String> noCostUsersForRobot = new LinkedHashSet<>(); // 用于企微通知

		List<List<Object>> rows = DB.getSQLArrayObjectsEx(get_TrxName(), sql, hrResourceTypeValue, getAD_Client_ID(),
				laborCostElementId, acctSchemaId, workTeamId);

		if (rows != null) {
			for (List<Object> row : rows) {
				String userName = (String) row.get(0);
				Object resourceIdObj = row.get(2);
				BigDecimal currentCostPrice = (BigDecimal) row.get(3);

				boolean noResource = resourceIdObj == null || ((Number) resourceIdObj).intValue() <= 0;
				boolean noCost = !noResource
						&& (currentCostPrice == null || currentCostPrice.compareTo(BigDecimal.ZERO) <= 0);

				if (noResource) {
					noResourceUsers.add(userName);
				} else if (noCost) {
					noCostUserNames.add(userName);
					noCostUsersForRobot.add(userName);
				}
			}
		}

		StringBuilder errorMsg = new StringBuilder();
		if (!noResourceUsers.isEmpty()) {
			errorMsg.append("班组成员（").append(String.join(",", noResourceUsers)).append("）未绑定人力资源，请联系管理员处理；\n");
		}
		if (!noCostUserNames.isEmpty()) {
			errorMsg.append("班组成员（").append(String.join(",", noCostUserNames)).append("）的人工成本数据未维护，请联系财务部门录入；\n");
		}

		if (errorMsg.length() > 0) {
			log.warning("checkDirectLaborResourceCost: C_WorkTeam_ID=" + workTeamId + " 校验未通过 - " + errorMsg);
			// 发送企微机器人通知财务/管理员
			if (!noCostUsersForRobot.isEmpty()) {
				String robotContent = "组员:（" + String.join(", ", noCostUsersForRobot) + "）无成本，请前往【直接人工】-【价格】维护人工成本。";
				notifyWeChatRobot(robotContent);
			}

			throw new AdempiereException(errorMsg.toString());
		}
	}

	/**
	 * 直接人工成本校验未通过时，发送企微机器人通知（仅通知渠道，不影响主流程）。 Webhook 未配置时跳过；发送异常不应影响报工单校验/拦截的主流程。
	 */
	private void notifyWeChatRobot(String content) {
		try {
			String webhookUrl = HFSysConfigEnum.HF_WECHAT_ROBOT_LABOR_COST_WEBHOOK_URL.getValue(getAD_Client_ID());
			if (webhookUrl == null || webhookUrl.isEmpty()) {
				log.warning(
						"checkDirectLaborResourceCost: 企微机器人webhook未配置(HF_WECHAT_ROBOT_LABOR_COST_WEBHOOK_URL)，跳过通知，PP_Cost_Collector_ID="
								+ get_ID());
				return;
			}

			String fullContent = "【海富ERP】：" + content;

			log.info("准备发送直接人工成本校验未通过企微通知，PP_Cost_Collector_ID=" + get_ID());

			WeChatRobotUtils.sendText(webhookUrl, fullContent);

			log.info("直接人工成本校验未通过企微通知发送成功，PP_Cost_Collector_ID=" + get_ID());
		} catch (Exception e) {
			// 通知失败不应影响报工单被拦截这个主流程行为，只记录日志
			log.log(Level.WARNING, "直接人工成本校验未通过企微通知发送失败，PP_Cost_Collector_ID=" + get_ID(), e.getMessage());
		}
	}


	/**
	 * 直接人工成本校验：生产工单检查。 若报工单对应的生产工单(PP_Order)状态为"关闭"(Orderstatus='Close')，
	 * 不允许完成该报工单——报错提示财务/生产人员先修改报工日期。
	 */
	private void checkPPOrderNotClosed() {
		if (getPP_Order_ID() <= 0)
			return; // 非生产工单相关报工（如非生产报工），不受本条校验约束

		MPPOrder ppOrder = getPP_Order();
		String orderStatus = (String) ppOrder.get_Value("Orderstatus");
		if ("Close".equals(orderStatus)) {
			throw new AdempiereException("当前工单已关闭，请修改报工日期");
		}
	}

	/**
	 * 直接人工成本校验：员工月工作时长检查。
	 * 
	 * 规则： - 统计本次报工单班组下所有成员本月报工时长不能超过260h
	 */
	private void checkMonthlyWorkHours() {
		int workTeamId = get_ValueAsInt("C_WorkTeam_ID");
		if (workTeamId <= 0)
			return; // 没有班组信息，跳过

		Timestamp movementDate = getMovementDate();
		if (movementDate == null)
			return;

		BigDecimal monthlyLimit = HFSysConfigEnum.HF_MONTHLY_WORK_HOURS.getBigDecimalValue(getAD_Client_ID());

		BigDecimal currentDuration = getDurationReal();
		if (currentDuration == null)
			currentDuration = BigDecimal.ZERO;

		// 查班组下所有激活成员（不区分是否直接人工，超时校验是对"人"的工时上限约束，
		// 是否生成分录留到后续生成分录阶段再按 IsDirectLabor='Y' 过滤）
		List<Integer> userIds = new ArrayList<>();
		List<X_C_WorkTeamMember> members = new Query(getCtx(), org.libero.tables.X_C_WorkTeamMember.Table_Name,
				"C_WorkTeam_ID=? AND IsActive='Y'", get_TrxName()).setParameters(workTeamId).list();
		for (X_C_WorkTeamMember m : members) {
			userIds.add(m.getAD_User_ID());
		}

		for (Integer userId : userIds) {
			if (userId == null || userId <= 0)
				continue;

			// 统计该员工当月（按 MovementDate 所在自然月）、已完成(CO)的历史报工工时，排除本单自身
			String sql = "SELECT COALESCE(SUM(cc.DurationReal), 0) " + "FROM PP_Cost_Collector cc "
					+ "JOIN C_WorkTeamMember wtm ON wtm.C_WorkTeam_ID = cc.C_WorkTeam_ID " + "WHERE wtm.AD_User_ID = ? "
					+ "  AND wtm.IsActive = 'Y' " + "  AND cc.DocStatus = ? " + "  AND cc.PP_Cost_Collector_ID <> ? "
					+ "  AND cc.MovementDate >= date_trunc('month', ?::timestamp) "
					+ "  AND cc.MovementDate < date_trunc('month', ?::timestamp) + interval '1 month'";

			BigDecimal reportedHours = DB.getSQLValueBD(get_TrxName(), sql, userId,
					MPPCostCollector.DOCSTATUS_Completed, get_ID(), movementDate, movementDate);
			if (reportedHours == null)
				reportedHours = BigDecimal.ZERO;
			reportedHours = reportedHours.compareTo(monthlyLimit) > 0 ? monthlyLimit : reportedHours;

			BigDecimal totalAfterThisTime = reportedHours.add(currentDuration);
			if (totalAfterThisTime.compareTo(monthlyLimit) > 0) {
				String userName = MUser.getNameOfUser(userId);
				log.warning("@" + userName + "(AD_User_ID=" + userId + ")（已报工" + reportedHours + "小时，当前报工"
						+ currentDuration + "小时），员工当月累计报工时间不可超过" + monthlyLimit + "小时，多余"
						+ (totalAfterThisTime.subtract(monthlyLimit)) + "小时不计入工时");
			}
		}
	}

//	@Override
	public boolean  approveIt()
	{
		log.info("approveIt - " + toString());
		//setIsApproved(true);
		return true;
	}	//	approveIt

//	@Override
	public boolean rejectIt()
	{
		log.info("rejectIt - " + toString());
		//setIsApproved(false);
		return true;
	}	//	rejectIt

	public boolean isSubcontractingIssue() {
		return COSTCOLLECTORTYPE_SUBCONTRACTING_ISSUE.equals(getCostCollectorType());
	}

	public boolean isSubcontractingReturn() {
		return COSTCOLLECTORTYPE_SUBCONTRACTING_RETURN.equals(getCostCollectorType());
	}

	public boolean isSubcontractingReplenishment() {
		return COSTCOLLECTORTYPE_SUBCONTRACTING_REPLENISHMENT.equals(getCostCollectorType());
	}

	
	@Override
	public String completeIt()
	{
	    // 原有的校验逻辑
	    if (!m_justPrepared)
	    {
	        String status = prepareIt();
	        if (!DocAction.STATUS_InProgress.equals(status))
	            return status;
	    }

	    m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
	    if (m_processMsg != null)
	        return DocAction.STATUS_Invalid;
	    
	    //
	    // Material Issue (component issue, method change variance, mix variance)
	    // Material Receipt
		if (isIssue() || isReceipt() || isProductionReturn() || isProductionReplenishment() || isSubcontractingIssue()
				|| isSubcontractingReturn() || isSubcontractingReplenishment())
	    {
	        // 原有的库存移动逻辑
	        MProduct product = getM_Product();
	        if (product != null && product.isStocked() && !isVariance())
	        {
	            StorageEngine.createTransaction(
	                    this,
	                    getMovementType(), 
	                    getMovementDate(), 
	                    getMovementQty(), 
	                    false, // IsReversal=false
	                    getM_Warehouse_ID(), 
	                    getPP_Order().getM_AttributeSetInstance_ID(), // Reservation ASI
	                    getPP_Order().getM_Warehouse_ID(), // Reservation Warehouse
	                    false // IsSOTrx=false
	                    );
	        } // 库存移动
	        
			if (isIssue() || isSubcontractingIssue()) 
	        {
	        	
	            String orderStatus = (String) getPP_Order().get_Value("Orderstatus");
	            
	            if ("InECNChange".equals(orderStatus) || "ChangeExecuted".equals(orderStatus)) {
	            	throw new AdempiereException("处于【ECN变更中】或【已变更】状态下的工单无法领料，领料失败!");
	            }
	            // 生产发料：更新订单BOM行
	            MPPOrderBOMLine obomline = getPP_Order_BOMLine();
	            obomline.setQtyDelivered(obomline.getQtyDelivered().add(getMovementQty()));
	            
//				if (obomline.getQtyDelivered().compareTo(obomline.getQtyRequiered()) > 0) {
//					// throw new AdempiereException("领料数量大于需求数量，领料失败!");
//				}
	            
	            obomline.setQtyScrap(obomline.getQtyScrap().add(getScrappedQty()));
	            obomline.setQtyReject(obomline.getQtyReject().add(getQtyReject()));  
	            obomline.setDateDelivered(getMovementDate()); // 覆盖为最后一次的日期
	            obomline.saveEx(get_TrxName());
	        }
	        if (isReceipt())
	        {
	            // 生产入库：更新工单信息
	            final MPPOrder order = getPP_Order();
	            order.setQtyDelivered(order.getQtyDelivered().add(getMovementQty()));                
	            order.setQtyScrap(order.getQtyScrap().add(getScrappedQty()));
	            order.setQtyReject(order.getQtyReject().add(getQtyReject()));     
	            // 检查并更新Orderstatus  
	            if (!"Stored".equals(order.get_ValueAsString("Orderstatus"))  
	                    && order.getQtyDelivered().compareTo(order.getQtyEntered()) >= 0) {  
	                order.set_ValueOfColumn("Orderstatus", "Stored");  
	            }
	            
	            // 更新PP Order日期
	            order.setDateDelivered(getMovementDate()); // 覆盖为最后一次的日期
	            if (order.getDateStart() == null)
	            {
	                order.setDateStart(getDateStart());
	            }
	            if (order.getQtyOpen().signum() <= 0)
	            {
	                order.setDateFinish(getDateFinish());
	            }
	            order.saveEx(get_TrxName());
	        }
	        
			if (isProductionReturn() || isSubcontractingReturn())
	        {
	            // 生产退料：减少已交付数量
	            MPPOrderBOMLine obomline = getPP_Order_BOMLine();  
	            obomline.setQtyDelivered(obomline.getQtyDelivered().subtract(getMovementQty()));  
				if (obomline.getQtyDelivered().compareTo(BigDecimal.ZERO) < 0) {
					throw new AdempiereException("退料数量大于已领数量，退料失败!");
				}
	            obomline.saveEx(get_TrxName());  
	        }
			if (isProductionReplenishment() || isSubcontractingReplenishment())
	        {
	            String orderStatus = (String) getPP_Order().get_Value("Orderstatus");
	            
	            if ("InECNChange".equals(orderStatus) || "ChangeExecuted".equals(orderStatus)) {
	            	throw new AdempiereException("处于【ECN变更中】或【已变更】状态下的工单无法领料，领料失败!");
	            }
	            // 生产补料：增加已交付数量
	            MPPOrderBOMLine obomline = getPP_Order_BOMLine();  
	            obomline.setQtyDelivered(obomline.getQtyDelivered().add(getMovementQty()));  
	            obomline.saveEx(get_TrxName());  
	        }
	    }
	    // Activity Control
	    else if(isActivityControl())
	    {
	        MPPOrderNode activity = getPP_Order_Node();
	        if(activity.isProcessed())
	        {
	            throw new ActivityProcessedException(activity);
	        }
	        
	        if(isSubcontracting())
	        {    
	            String whereClause = MOrderLine.COLUMNNAME_PP_Cost_Collector_ID+"=?";
	            Collection<MOrderLine> olines = new Query(getCtx(), MOrderLine.Table_Name, whereClause, get_TrxName())
	                                                .setParameters(new Object[]{get_ID()})
	                                                .list();
	            String DocStatus = MPPOrderNode.DOCSTATUS_Completed;
	            StringBuffer msg = new StringBuffer("The quantity do not is complete for next Purchase Order : ");
	            for (MOrderLine oline : olines)
	            {
	                if(oline.getQtyDelivered().compareTo(oline.getQtyOrdered()) < 0)
	                {
	                    DocStatus = MPPOrderNode.DOCSTATUS_InProgress;
	                }
	                msg.append(oline.getParent().getDocumentNo()).append(",");
	            }
	            
	            if(MPPOrderNode.DOCSTATUS_InProgress.equals(DocStatus))
	            {    
	                m_processMsg = msg.toString();
	                return DocStatus;
	            }
	            setProcessed(true);
	            setDocAction(MPPOrderNode.DOCACTION_Close);
	            setDocStatus(MPPOrderNode.DOCSTATUS_Completed);
	            activity.completeIt();
	            activity.saveEx(get_TrxName());
	            m_processMsg = Msg.translate(getCtx(), "PP_Order_ID")
	            +": "+ getPP_Order().getDocumentNo()
	            +" "+ Msg.translate(getCtx(),"PP_Order_Node_ID")
	            +": "+getPP_Order_Node().getValue();
	            return DocStatus;
	        }
	        else
	        {
	            CostEngineFactory.getCostEngine(getAD_Client_ID()).createActivityControl(this);
	            if(activity.getQtyDelivered().compareTo(activity.getQtyRequiered()) >= 0)
	            {
	                activity.closeIt();
	                activity.saveEx(get_TrxName());                                    
	            }
	        }
	    }
	    //
	    // Usage Variance (material)
	    else if (isCostCollectorType(COSTCOLLECTORTYPE_UsegeVariance) && getPP_Order_BOMLine_ID() > 0)
	    {
	        MPPOrderBOMLine obomline = getPP_Order_BOMLine();
	        obomline.setQtyDelivered(obomline.getQtyDelivered().add(getMovementQty()));
	        obomline.setQtyScrap(obomline.getQtyScrap().add(getScrappedQty()));
	        obomline.setQtyReject(obomline.getQtyReject().add(getQtyReject()));  
	        obomline.saveEx(get_TrxName());
	        CostEngineFactory.getCostEngine(getAD_Client_ID()).createUsageVariances(this);
	    }
	    //
	    // Usage Variance (resource)
	    else if (isCostCollectorType(COSTCOLLECTORTYPE_UsegeVariance) && getPP_Order_Node_ID() > 0)
	    {
	        MPPOrderNode activity = getPP_Order_Node();
			activity.setDurationReal(activity.getDurationReal().add(getDurationReal()));
			activity.setSetupTimeReal(activity.getSetupTimeReal().add(getSetupTimeReal()));
	        activity.saveEx(get_TrxName());
	        CostEngineFactory.getCostEngine(getAD_Client_ID()).createUsageVariances(this);
	    }
	    else
	    {
	        // nothing
	    }
	    
	    // 添加逻辑：检查并更新生产工单状态
	    // 当单据完成时，如果工单状态为"已开工(Started)"，则更新为"执行中(InProgress)"
	    // 注意：只需要发一种料就可以变为执行中
	    String costCollectorType = getCostCollectorType();
	    
	    // 检查是否是生产发料、生产补领或生产退料类型
	    boolean isProductionOperation = 
	    	COSTCOLLECTORTYPE_ComponentIssue.equals(costCollectorType) ||
	        COSTCOLLECTORTYPE_ProductionReplenishment.equals(costCollectorType) ||
	        COSTCOLLECTORTYPE_ProductionReturn.equals(costCollectorType);
	    
	    if (isProductionOperation) {
	        MPPOrder order = getPP_Order();
	        if (order != null) {
	            // 获取工单当前状态
	            String orderStatus = order.get_ValueAsString("Orderstatus");
	            
	            // 如果工单状态为"Started"，则更新为"InProgress"
	            if ("Started".equals(orderStatus)) {
	                // 更新工单状态为执行中
	                order.set_ValueOfColumn("Orderstatus", "InProgress");
	                order.saveEx(get_TrxName());
	                
	                log.info("工单状态已从 Started 更新为 InProgress，工单ID: " + order.get_ID());
	            }
	        }
	    }
	    
		if (!MSysConfig.getBooleanValue("SKIP_VARIANCE_PP_COST_COLLECTOR", false, getAD_Client_ID())) {
			// 原有的差异计算
			CostEngineFactory.getCostEngine(getAD_Client_ID()).createRateVariances(this);
			CostEngineFactory.getCostEngine(getAD_Client_ID()).createMethodVariances(this);
		}
	    m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
	    if (m_processMsg != null)
	        return DocAction.STATUS_Invalid;

		// 设置QtyOnHand快照字段
	    setCurrentQtyOnHand();
	    // 设置单据为已完成状态
	    setProcessed(true);
	    setDocAction(DOCACTION_Close);
	    setDocStatus(DOCSTATUS_Completed);
	    
	    return DocAction.STATUS_Completed;
	} // completeIt
//	@Override
//	public boolean voidIt()
//	{
//		return false;
//	}	//	voidIt

//	@Override
	public boolean closeIt()
	{
		log.info("closeIt - " + toString());
		setDocAction(DOCACTION_None);
		return true;
	}	//	closeIt

	public void setCurrentQtyOnHand() {  
	    if (getM_Product_ID() > 0 && getM_Locator_ID() > 0)  
	    {  
	        BigDecimal qtyOnHand = MStorageOnHand.getQtyOnHandForLocator(  
	                getM_Product_ID(),  
	                getM_Locator_ID(),  
	                0,              // M_AttributeSetInstance_ID=0 表示不区分ASI，SUM全部  
	                get_TrxName());  
	        set_ValueOfColumn("QtyOnHand", qtyOnHand);  
	    }  
	}
//	@Override
//	public boolean reverseCorrectIt()
//	{
//		return false;
//	}

//	@Override
//	public boolean reverseAccrualIt()
//	{
//		return false;
//	}

//	@Override
//	public boolean reActivateIt()
//	{
//		return false;
//	}

//	@Override
	public String getSummary()
	{
		StringBuffer sb = new StringBuffer();
		sb.append(getDescription());
		return sb.toString();
	}

//	@Override
	public String getProcessMsg()
	{
		return m_processMsg;
	}

//	@Override
	public int getDoc_User_ID()
	{
		return getCreatedBy();
	}

//	@Override
	public int getC_Currency_ID()
	{
		return 0;
	}

//	@Override
	public BigDecimal getApprovalAmt()
	{
		return Env.ZERO;
	}

//	@Override
	public File createPDF ()
	{
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}
		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}	//	getPDF

	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
		ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.ORDER, getPP_Order_ID());
		if (re == null)
			return null;
		return re.getPDF(file);
	}	//	createPDF

//	@Override
	public String getDocumentInfo()
	{
		MDocType dt = MDocType.get(getCtx(), getC_DocType_ID());
		return dt.getName() + " " + getDocumentNo();
	}	//	getDocumentInfo

	@Override  
	protected boolean afterSave(boolean newRecord, boolean success)  
	{  
	    if (!success)  
	        return success;  
	  
	    if (isNonProduction() && newRecord)  
	    {  
	        if (!processIt(DocAction.ACTION_Complete))  
	        {  
	            log.saveError("Error", "自动完成失败: " + getProcessMsg());  
	            return false;  
	        }  
	        // processIt 只改内存状态，必须显式保存才能把 DocStatus/Processed 落库  
	        if (!save())  
	            return false;  
	    }  
	    return true;  
	}
	
	@Override
	protected boolean beforeSave(boolean newRecord)
	{
		BigDecimal  durationReal = getDurationReal();
		if (isNonProduction() && BigDecimal.ZERO.compareTo(durationReal) == 0) {
			throw new AdempiereException("报工工时不能为0，请检查开始时间和结束时间。");
		}
		
		// 是否使用替代料
		boolean isSubstitute = getIsSubstitute();
		// Set default locator, if not set and we have the warehouse:
		if (getM_Locator_ID() <= 0 && getM_Warehouse_ID() > 0)
		{
			MWarehouse wh = MWarehouse.get(getCtx(), getM_Warehouse_ID());
			MLocator loc = wh.getDefaultLocator();
			if (loc != null)
			{
				setM_Locator_ID(loc.get_ID());
			}
		}
		//
		if (isIssue())
		{
			if (getPP_Order_BOMLine_ID() <= 0)
			{
				throw new FillMandatoryException(COLUMNNAME_PP_Order_BOMLine_ID);
			}
			// If no UOM, use the UOM from BOMLine
			if (getC_UOM_ID() <= 0)
			{
//				setC_UOM_ID(getPP_Order_BOMLine().getC_UOM_ID());
				MProduct product = MProduct.get(getCtx(), getM_Product_ID());
				setC_UOM_ID(isSubstitute ? product.getC_UOM_ID() : getPP_Order_BOMLine().getC_UOM_ID());
			}
			// If Cost Collector UOM differs from BOM Line UOM then throw exception because this conversion is not supported yet
			if (!isSubstitute && getC_UOM_ID() != getPP_Order_BOMLine().getC_UOM_ID())
			{
				throw new AdempiereException("@PP_Cost_Collector_ID@ @C_UOM_ID@ <> @PP_Order_BOMLine_ID@ @C_UOM_ID@");
			}
		}
		//
		if (isActivityControl() && getPP_Order_Node_ID() <= 0)
		{
			throw new FillMandatoryException(COLUMNNAME_PP_Order_Node_ID);
		}

		// 校验：作业员(AD_User_ID) 和 班组(C_WorkTeam_ID) 必须且只能填写一个
		// 仅在生产报工（ActivityControl, CostCollectorType='160'）时生效
//		if (isActivityControl()) {
//			boolean hasUser = getAD_User_ID() > 0;
//			boolean hasWorkTeam = get_ValueAsInt("C_WorkTeam_ID") > 0;
//			if (!hasUser && !hasWorkTeam) {
//				throw new AdempiereException("作业员和班组必须填写其中一个");
//			}
//			if (hasUser && hasWorkTeam) {
//				throw new AdempiereException("作业员和班组不能同时填写，只能选择其中一个");
//			}
//		}
		// 添加工序顺序校验
//		if (!validateWorkflowSequence()) {
//			return false;
//		}
		// 添加机台占用校验
//		if (!validateResourceAvailability()) {
//			return false;
//		}
//		

		// 移动数量必须 > 0
		// 跳过条件1：冲销单（红字更正自动生成），MovementQty 为负数是合法的
		boolean isReversalDoc = getReversal_ID() > 0;

		// 跳过条件2：作废操作，voidIt() 在 save() 前已将 DocStatus 置为 VO，MovementQty 置 0 合法
		boolean isVoidOp = DOCSTATUS_Voided.equals(getDocStatus());

		// 跳过条件3：修改时 MovementQty 字段本身未变更，无需重复校验（含重新激活场景）
		boolean qtyUnchanged = !newRecord && !is_ValueChanged("MovementQty");

		// 跳过条件4：差异类型（系统自动生成），MovementQty 可能为负数（如用量差异）
		boolean isVarianceType = isVariance();

		// 跳过条件5：生产报工草稿状态允许数量为0（创建报工单时默认值为0）
		boolean isActivityControlDraft = isActivityControl() && DOCSTATUS_Drafted.equals(getDocStatus());

		boolean skipQtyCheck = isActivityControl() && !isWorkReportTypeProduce();

		// 跳过条件6：成本归集单类型为以下类型：非生产报工
		boolean skipCostCollectorType = isNonProduction();

		if (!skipQtyCheck) {
			if (!isReversalDoc && !isVoidOp && !qtyUnchanged && !isVarianceType && !isActivityControlDraft && !skipCostCollectorType) {

				BigDecimal qty = getMovementQty();
				if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
					log.saveError("ValidationError", "移动数量必须大于0（当前值：" + (qty != null ? qty.toPlainString() : "null") + "）");
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * 校验工序报工顺序
	 */
	private boolean validateWorkflowSequence() {
		// 只对活动控制类型的报工单进行校验
		if (!isActivityControl()) {
			return true;
		}


		MPPOrderNode currentNode = getPP_Order_Node();
		if (currentNode == null) {
			return true;
		}

		// 获取工单的所有工序（按顺序）
		MPPOrderWorkflow workflow = getPP_Order().getMPPOrderWorkflow();
		MPPOrderNode[] allNodes = workflow.getNodes(true, getAD_Client_ID());
		// 按 Value 字段排序（从小到大）
		Arrays.sort(allNodes, new Comparator<MPPOrderNode>() {
		    @Override
		    public int compare(MPPOrderNode node1, MPPOrderNode node2) {
		        return node1.getValue().compareTo(node2.getValue());
		    }
		});
		// 找到当前工序在序列中的位置
		int currentIndex = -1;
		for (int i = 0; i < allNodes.length; i++) {
			if (allNodes[i].getPP_Order_Node_ID() == currentNode.getPP_Order_Node_ID()) {
				currentIndex = i;
				break;
			}
		}

		// 检查前面工序是否都已完成（跳过委外工序）
		for (int i = 0; i < currentIndex; i++) {
			MPPOrderNode prevNode = allNodes[i];
			// 如果前置工序是委外工序，跳过完成状态检查
			if (prevNode.isSubcontracting()) {
				continue;
			}
			if (!isNodeCompleted(prevNode)) {
				log.saveError("Error", "上工序[" + prevNode.getName() + "]未完成，不能报工当前工序");
				return false;
			}
		}

		return true;
	}

	/**
	 * 检查工序是否已完成
	 */
	private boolean isNodeCompleted(MPPOrderNode node) {
		String sql = "SELECT COUNT(*) FROM PP_Cost_Collector " + "WHERE PP_Order_Node_ID = ? " + "AND DocStatus = ? "
				+ "AND CostCollectorType = ?";

		int count = DB.getSQLValue(get_TrxName(), sql, node.getPP_Order_Node_ID(), MPPCostCollector.DOCSTATUS_Completed,
				MPPCostCollector.COSTCOLLECTORTYPE_ActivityControl);

		return count > 0;
	}

	@Override
	public MPPOrderNode getPP_Order_Node()
	{
		int node_id = getPP_Order_Node_ID();
		if (node_id <= 0)
		{
			m_orderNode = null;
			return null;
		}
		if (m_orderNode == null || m_orderNode.get_ID() != node_id)
		{
			m_orderNode = new MPPOrderNode(getCtx(), node_id, get_TrxName());
		}
		return m_orderNode;
	}

	@Override
	public MPPOrderBOMLine getPP_Order_BOMLine()
	{
		int id = getPP_Order_BOMLine_ID();
		if (id <= 0)
		{
			m_bomLine = null;
			return null;
		}
		if (m_bomLine == null || m_bomLine.get_ID() != id)
		{
			m_bomLine = new MPPOrderBOMLine(getCtx(), id, get_TrxName());
		}
		m_bomLine.set_TrxName(get_TrxName());
		return m_bomLine;
	}
	
	@Override
	public MPPOrder getPP_Order()
	{
		int id = getPP_Order_ID();
		if (id <= 0)
		{
			m_order = null;
			return null;
		}
		if (m_order == null || m_order.get_ID() != id)
		{
			m_order = new MPPOrder(getCtx(), id, get_TrxName());
		}
		return m_order;
	}
	
	/**
	 * Get Duration Base in Seconds
	 * @return duration unit in seconds
	 * @see MPPOrderWorkflow#getDurationBaseSec()
	 */
	public long getDurationBaseSec()
	{
		return getPP_Order().getMPPOrderWorkflow().getDurationBaseSec();
	}

	/**
	 * @return Activity Control Report Start Date
	 */
	public Timestamp getDateStart()
	{
		double duration = getDurationReal().doubleValue();
		if (duration != 0)
		{
			long durationMillis = (long)(getDurationReal().doubleValue() * getDurationBaseSec() * 1000.0);
			return new Timestamp(getMovementDate().getTime() - durationMillis);
		}
		else
		{
			return getMovementDate();
		}
	}
	
	/**
	 * @return Activity Control Report End Date
	 */
	public Timestamp getDateFinish()
	{
		return getMovementDate();
	}

	
	/**
	 * Create Purchase Order (in case of Subcontracting)
	 * @param activity
	 */
	private String createPO(MPPOrderNode activity)
	{
		String msg = "";
		HashMap<Integer,MOrder> orders = new HashMap<Integer,MOrder>();
		//
		String whereClause = MPPOrderNodeProduct.COLUMNNAME_PP_Order_Node_ID+"=?"
							+" AND "+MPPOrderNodeProduct.COLUMNNAME_IsSubcontracting+"=?";
		Collection<MPPOrderNodeProduct> subcontracts = new Query(getCtx(), MPPOrderNodeProduct.Table_Name, whereClause, get_TrxName())
				.setParameters(new Object[]{activity.get_ID(), true})
				.setOnlyActiveRecords(true)
				.list();
		
		for (MPPOrderNodeProduct subcontract : subcontracts)
		{
			//
			// If Product is not Purchased or is not Service, then it is not a subcontracting candidate [SKIP]
			MProduct product = MProduct.get(getCtx(), subcontract.getM_Product_ID());
			if(!product.isPurchased() || !MProduct.PRODUCTTYPE_Service.equals(product.getProductType()))
				throw new AdempiereException("The Product: " + product.getName() + " Do not is Purchase or Service Type");

			//
			// Find Vendor and Product PO data
			int C_BPartner_ID = activity.getC_BPartner_ID();
			MProductPO product_po = null;
			for (MProductPO ppo : MProductPO.getOfProduct(getCtx(), product.get_ID(), null))
			{
				if(C_BPartner_ID == ppo.getC_BPartner_ID())
				{
					C_BPartner_ID = ppo.getC_BPartner_ID();
					product_po = ppo;
					break;
				}
				if (ppo.isCurrentVendor() && ppo.getC_BPartner_ID() != 0)
				{
					C_BPartner_ID = ppo.getC_BPartner_ID();
					product_po = ppo;
					break;
				}
			}
			if(C_BPartner_ID <= 0 || product_po == null)
			{
				throw new NoVendorForProductException(product.getName());
			}
			//
			// Calculate Lead Time
			Timestamp today = new Timestamp(System.currentTimeMillis());
			Timestamp datePromised = TimeUtil.addDays(today, product_po.getDeliveryTime_Promised()); 
			//
			// Get/Create Purchase Order Header
			MOrder order = orders.get(C_BPartner_ID);
			if(order == null)
			{
				order = new MOrder(getCtx(), 0, get_TrxName());
				MBPartner vendor = MBPartner.get(getCtx(), C_BPartner_ID);
				order.setAD_Org_ID(getAD_Org_ID());
				order.setBPartner(vendor);
				order.setIsSOTrx(false);
				order.setC_DocTypeTarget_ID();
				order.setDatePromised(datePromised);
				order.setDescription(Msg.translate(getCtx(), MPPOrder.COLUMNNAME_PP_Order_ID) +":"+getPP_Order().getDocumentNo());
				order.setDocStatus(MOrder.DOCSTATUS_Drafted);
				order.setDocAction(MOrder.DOCACTION_Complete);
				order.setAD_User_ID(getAD_User_ID());
				order.setM_Warehouse_ID(getM_Warehouse_ID());
				//order.setSalesRep_ID(getAD_User_ID());
				order.saveEx(get_TrxName());
				addDescription(Msg.translate(getCtx(), "C_Order_ID")+": "+order.getDocumentNo());
				orders.put(C_BPartner_ID, order);
				msg = msg +  Msg.translate(getCtx(), "C_Order_ID")
				+" : "+ order.getDocumentNo() 
				+" - "
				+Msg.translate(getCtx(),"C_BPartner_ID")
				+" : "+vendor.getName()+" , ";
			}
			//
			// Create Order Line: 
			BigDecimal QtyOrdered = getMovementQty().multiply(subcontract.getQty());
			// Check Order Min 
			if(product_po.getOrder_Min().signum() > 0)
			{    
				QtyOrdered = QtyOrdered.max(product_po.getOrder_Min());
			}				
			// Check Order Pack
			if (product_po.getOrder_Pack().signum() > 0 && QtyOrdered.signum() > 0)
			{
				QtyOrdered = product_po.getOrder_Pack().multiply(QtyOrdered.divide(product_po.getOrder_Pack(), 0 , BigDecimal.ROUND_UP));
			}
			MOrderLine oline = new MOrderLine(order);
			oline.setM_Product_ID(product.getM_Product_ID());
			oline.setDescription(activity.getDescription());
			oline.setM_Warehouse_ID(getM_Warehouse_ID());
			oline.setQty(QtyOrdered);
			//line.setPrice(m_product_po.getPricePO());
			//oline.setPriceList(m_product_po.getPriceList());
			oline.setPP_Cost_Collector_ID(get_ID());			
			oline.setDatePromised(datePromised);
			oline.saveEx(get_TrxName());
			//
			// TODO: Mark this as processed? 
			setProcessed(true);
		} // each subcontracting line
		return msg;
	}
	
	@Override
	public MProduct getM_Product()
	{
		return MProduct.get(getCtx(), getM_Product_ID());
	}
	
	@Override
	public I_C_UOM getC_UOM()
	{
		return MUOM.get(getCtx(), getC_UOM_ID());
	}

	public boolean isProductionReturn()  
	{  
	    return isCostCollectorType(COSTCOLLECTORTYPE_ProductionReturn);  
	}
	
	public boolean isProductionReplenishment()  
	{  
	    return isCostCollectorType(COSTCOLLECTORTYPE_ProductionReplenishment);  
	}

	/**    
	 * 生产报工（CostCollectorType=160）实际工时计算：    
	 * 计算 [startDate, finishDate] 区间与当天午休时段 [12:00, 12:45] 的重叠时长，  
	 * 并从总时长中扣除该重叠部分（重叠为0则不扣，即完全不涉及午休时正常计算时差）。  
	 * （保留两位小数）    
	 */    
	public static BigDecimal updateDurationRealFromDates(Timestamp actualStartDate, Timestamp actualFinishDate) {
	return calculateNonProductionDuration(actualStartDate, actualFinishDate);
	}


	// 午休起止（相对当天0点的毫秒偏移量）    
	private static final long NOON_MILLIS      = 12L * 60 * 60 * 1000;         // 12:00    
	private static final long LUNCH_END_MILLIS = 12L * 60 * 60 * 1000 + 45L * 60 * 1000; // 12:45    
	  
	/**    
	 * 非生产报工（CostCollectorType=161）实际工时计算：    
	 * 计算 [startDate, finishDate] 区间与当天午休时段 [12:00, 12:45] 的重叠时长，  
	 * 并从总时长中扣除该重叠部分（重叠为0则不扣，即完全不涉及午休时正常计算时差）。  
	 * （保留两位小数）    
	 */    
	public static BigDecimal calculateNonProductionDuration(Timestamp startDate, Timestamp finishDate)    
	{    
	    if (startDate == null || finishDate == null)    
	        return BigDecimal.ZERO;    
	  
	    long dayStartMillis = TimeUtil.getDay(finishDate).getTime();    
	    long noonMillis = dayStartMillis + NOON_MILLIS;    
	    long lunchEndMillis = dayStartMillis + LUNCH_END_MILLIS;    
	  
	    long startMillis = startDate.getTime();    
	    long finishMillis = finishDate.getTime();    
	  
	    // 计算报工区间与午休区间 [noonMillis, lunchEndMillis] 的重叠时长    
	    long overlapStart = Math.max(startMillis, noonMillis);    
	    long overlapEnd = Math.min(finishMillis, lunchEndMillis);    
	    long overlapMillis = Math.max(0L, overlapEnd - overlapStart);    
	  
	    long durationMillis = (finishMillis - startMillis) - overlapMillis;    
	  
	    if (durationMillis < 0)    
	        durationMillis = 0;    
	  
	    // 精确小时数，直接保留两位小数，不再按0.5小时步长向下取整    
	    BigDecimal roundedHours = BigDecimal.valueOf(durationMillis)    
	            .divide(BigDecimal.valueOf(3600000L), 2, RoundingMode.HALF_UP);    
	  
	    return roundedHours;    
	}
	
	public boolean isIssue()
	{
		return
		isCostCollectorType(COSTCOLLECTORTYPE_ComponentIssue)
		|| (isCostCollectorType(COSTCOLLECTORTYPE_MethodChangeVariance) && getPP_Order_BOMLine_ID() > 0) // need inventory adjustment
		|| (isCostCollectorType(COSTCOLLECTORTYPE_MixVariance) && getPP_Order_BOMLine_ID() > 0)  // need inventory adjustment
		;
	}
	
	public boolean isReceipt()
	{
		return isCostCollectorType(COSTCOLLECTORTYPE_MaterialReceipt);
	}
	
	public boolean isActivityControl()
	{
		return isCostCollectorType(COSTCOLLECTORTYPE_ActivityControl);
	}
	
	public boolean isNonProduction()
	{
		return isCostCollectorType(COSTCOLLECTORTYPE_NonProduction);
	}

	public boolean isWorkReportTypeProduce()
	{
		String hfWorkReportType = getHF_WorkReportType();
		return HF_WORKREPORTTYPE_Produce.equals(hfWorkReportType);
	}


	public boolean isVariance()
	{
		return isCostCollectorType(COSTCOLLECTORTYPE_MethodChangeVariance
				, COSTCOLLECTORTYPE_UsegeVariance
				, COSTCOLLECTORTYPE_RateVariance
				, COSTCOLLECTORTYPE_MixVariance);
	}
	
	public String getMovementType() {
		if (isReceipt())// 生产入库
			return MTransaction.MOVEMENTTYPE_WorkOrderPlus;
		else if (isIssue())// 发料
			return MTransaction.MOVEMENTTYPE_WorkOrder_;
		else if (isProductionReturn()) // 退料
			return MTransaction.MOVEMENTTYPE_WorkOrderPlus; // 增加库存
		else if (isProductionReplenishment()) // 补料
			return MTransaction.MOVEMENTTYPE_WorkOrder_; // 减少库存
		// 添加委外类型处理
		else if (isSubcontractingIssue()) // 委外发料
			return MTransaction.MOVEMENTTYPE_WorkOrder_; // 减少库存
		else if (isSubcontractingReturn()) // 委外退料
	        return MTransaction.MOVEMENTTYPE_WorkOrderPlus; // 增加库存  
		else if (isSubcontractingReplenishment()) // 委外补领
	        return MTransaction.MOVEMENTTYPE_WorkOrder_; // 减少库存  
		else
			return null;
	}
	
	/**
	 * Check if CostCollectorType is equal with any of provided types
	 * @param types
	 * @return 
	 */
	public boolean isCostCollectorType(String ... types)
	{
		String type = getCostCollectorType();
		for (String t : types)
		{
			if (type.equals(t))
				return true;
		}
		return false;
	}
	
	
	public boolean isFloorStock()
	{
		final String whereClause = MPPOrderBOMLine.COLUMNNAME_PP_Order_BOMLine_ID+"=?"
									+" AND "+MPPOrderBOMLine.COLUMNNAME_IssueMethod+"=?";
		boolean isFloorStock = new Query(getCtx(), MPPOrderBOMLine.Table_Name, whereClause, get_TrxName())
						.setOnlyActiveRecords(true)
						.setParameters(new Object[]{getPP_Order_BOMLine_ID(), MPPOrderBOMLine.ISSUEMETHOD_FloorStock})
						.match();
		return isFloorStock;
	}
	
	/**
	 * set Is SubContracting
	 * @param PP_Order_Node_ID
	 **/
	public void setIsSubcontracting(int PP_Order_Node_ID)
	{
		
		setIsSubcontracting(MPPOrderNode.get(getCtx(), PP_Order_Node_ID, get_TrxName()).isSubcontracting());
	}

	/**
	 * 单据作废
	 */
	@Override
	public boolean voidIt() {
		log.info("voidIt - " + toString());

		// Before Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_VOID);
		if (m_processMsg != null)
			return false;

		// 检查是否可以作废
		if (!DOCSTATUS_Completed.equals(getDocStatus()) && !DOCSTATUS_Closed.equals(getDocStatus())) {
			m_processMsg = "单据状态已完成或已关闭才能作废!";
			return false;
		}

		// 0. 处理回滚库存事务
		if (!reverseStock()) {
			return false;
		}

//		// 1. 回滚成本差异记录
//		if (!reverseCostVariances()) {
//			return false;
//		}

		// 2. 删除成本明细记录
		if (!deleteCostDetails()) {
			return false;
		}

		// 4. 反向冲减相关数量
		if (!reverseRelatedQuantities()) {
			return false;
		}

		// 5. 更新活动控制（包含状态回滚）
		if (isActivityControl()) {
			updateActivityControl();
		}
		
		// 3. 设置单据数量为零
		BigDecimal oldMovementQty = getMovementQty();
		BigDecimal oldScrappedQty = getScrappedQty();
		BigDecimal oldQtyReject = getQtyReject();

		if (oldMovementQty.signum() != 0 || oldScrappedQty.signum() != 0 || oldQtyReject.signum() != 0) {
			addDescription(Msg.parseTranslation(getCtx(), "@Voided@ @MovementQty@ : (" + oldMovementQty
					+ ") @ScrappedQty@ : (" + oldScrappedQty + ") @QtyReject@ : (" + oldQtyReject + ")"));
			setMovementQty(Env.ZERO);
			setScrappedQty(Env.ZERO);
			setQtyReject(Env.ZERO);
			setDurationReal(Env.ZERO);
			setSetupTimeReal(Env.ZERO);
		}

		// 7. 设置文档状态
		setDocStatus(DOCSTATUS_Voided);
		setDocAction(DOCACTION_None);
		setProcessed(true);

		// After Void
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_VOID);
		if (m_processMsg != null)
			return false;

		return true;
	}

	/**
	 * 红字更正
	 */
	@Override
	public boolean reverseCorrectIt() {
		log.info("reverseCorrectIt - " + toString());

		// Before Reverse Correct
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REVERSECORRECT);
		if (m_processMsg != null)
			return false;

		// 检查是否可以冲销
		if (!DOCSTATUS_Completed.equals(getDocStatus()) && !DOCSTATUS_Closed.equals(getDocStatus())) {
			m_processMsg = "单据状态已完成或已关闭才能进行红字冲正!";
			return false;
		}

		// 检查是否已有冲销记录
		if (getReversal_ID() > 0) {
			m_processMsg = "当前单据已存在红字冲正单据!";
			return false;
		}

		// 1. 创建红字冲正单据
		MPPCostCollector reversal = createReversal();
		if (reversal == null) {
			m_processMsg = "创建红字冲正单据失败!";
			return false;
		}

		// 2. 设置关联关系
		setReversal_ID(reversal.get_ID());
		reversal.setReversal_ID(get_ID());

		// 3. 处理冲销单据的库存事务
		if (!processReversalStock(reversal)) {
			m_processMsg = "处理红字冲正单据的库存事务失败!";
			return false;
		}

		// 调用现有的数量回滚方法( voidIt() 的 reverseRelatedQuantities)
		if (!reverseRelatedQuantities()) {
			m_processMsg = "回滚数量失败!";
			return false;
		}

		// 添加活动状态重置
		if (isActivityControl()) {
			MPPOrderNode activity = getPP_Order_Node();
			activity.setQtyDelivered(activity.getQtyDelivered().add(reversal.getMovementQty()));
			activity.setDocStatus(MPPOrderNode.DOCSTATUS_InProgress);
			activity.saveEx(get_TrxName());
		}

		// 4. 创建冲销的成本明细
		if (!createReversalCostDetails(reversal)) {
			m_processMsg = "创建冲销成本明细失败!";
			return false;
		}

		// After Reverse Correct
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REVERSECORRECT);
		if (m_processMsg != null)
			return false;

		return true;
	}

	@Override
	public boolean reverseAccrualIt() {
		log.info("reverseAccrualIt - " + toString());

		// 冲销应计与冲销更正逻辑相同
		return reverseCorrectIt();
	}

	@Override
	public boolean reActivateIt() {
		log.info("reActivateIt - " + toString());

		// Before ReActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;

		// 只有已作废的单据才能重新激活
		if (!DOCSTATUS_Voided.equals(getDocStatus())) {
			m_processMsg = "已作废的单据才能重新激活!";
			return false;
		}

		// 恢复文档状态
		setDocStatus(DOCSTATUS_InProgress);
		setDocAction(DOCACTION_Complete);
		setProcessed(false);

		// After ReActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;

		return true;
	}

	/**
	 * 回滚库存事务
	 */
	private boolean reverseStock() {
		if (!isReceipt() && !isIssue()) {
			return true;
		}

		MProduct product = getM_Product();
		if (product != null && product.isStocked() && !isVariance()) {
			// 创建负数库存事务来回滚原始事务
			StorageEngine.createTransaction(this, getMovementType(), // 保持原始类型
					getMovementDate(), getMovementQty().negate(), // 使用负数
					true, // IsReversal=true
					getM_Warehouse_ID(), getPP_Order().getM_AttributeSetInstance_ID(),
					getPP_Order().getM_Warehouse_ID(), false);
		}
		return true;
	}


	/**
	 * 创建冲销记录
	 */
	private MPPCostCollector createReversal() {
		MPPCostCollector reversal = new MPPCostCollector(getCtx(), 0, get_TrxName());

		// 复制基本信息
		copyValues(this, reversal);
		reversal.setAD_Org_ID(this.getAD_Org_ID());
		reversal.setPP_Order_ID(getPP_Order_ID());
		reversal.setPP_Order_BOMLine_ID(getPP_Order_BOMLine_ID());
		reversal.setPP_Order_Node_ID(getPP_Order_Node_ID());
		reversal.setCostCollectorType(getCostCollectorType());

		// 设置冲销信息
		reversal.setReversal_ID(get_ID());
		reversal.setMovementQty(getMovementQty().negate());
		reversal.setScrappedQty(getScrappedQty().negate());
		reversal.setQtyReject(getQtyReject().negate());
		reversal.setDateAcct(getDateAcct());
		reversal.setMovementDate(getMovementDate());

		// 设置文档状态
		reversal.setDocStatus(DOCSTATUS_Completed);
		reversal.setDocAction(DOCACTION_Close);
		reversal.setProcessed(true);

		// 添加描述
		reversal.setDescription("Reversal of: " + getDocumentNo());

		if (!reversal.save()) {
			return null;
		}

		// 设置当前文档的冲销ID
		setReversal_ID(reversal.get_ID());

		return reversal;
	}

	/**
	 * 处理冲销单据的库存事务
	 */
	private boolean processReversalStock(MPPCostCollector reversal) {
		// c
		if (!reversal.isIssue() && !reversal.isReceipt())
			return true;

		MProduct product = reversal.getM_Product();
		if (product != null && product.isStocked() && !reversal.isVariance()) {
			// 创建相反的库存交易
			String movementType = reversal.getMovementType();

			StorageEngine.createTransaction(reversal, movementType, reversal.getMovementDate(),
					reversal.getMovementQty(), true, // IsReversal=true
					reversal.getM_Warehouse_ID(), getPP_Order().getM_AttributeSetInstance_ID(),
					getPP_Order().getM_Warehouse_ID(), false);
		}

		return true;
	}

	/**
	 * 反向冲减相关数量
	 */
	private boolean reverseRelatedQuantities() {
		try {
			if (isReceipt()) {
				// 减去生产订单的接收数量
				MPPOrder order = getPP_Order();
				order.setQtyDelivered(order.getQtyDelivered().subtract(getMovementQty()));
				order.setQtyScrap(order.getQtyScrap().subtract(getScrappedQty()));
				order.setQtyReject(order.getQtyReject().subtract(getQtyReject()));
				order.saveEx(get_TrxName());
			} else if (isIssue()) {
				// 减去BOM行的发料数量
				MPPOrderBOMLine bomLine = getPP_Order_BOMLine();
				bomLine.setQtyDelivered(bomLine.getQtyDelivered().subtract(getMovementQty()));
				bomLine.setQtyScrap(bomLine.getQtyScrap().subtract(getScrappedQty()));
				bomLine.setQtyReject(bomLine.getQtyReject().subtract(getQtyReject()));
                // 预留数量清零
				bomLine.setQtyReserved(Env.ZERO);
				bomLine.saveEx(get_TrxName());
			}

			return true;
		} catch (Exception e) {
			log.severe("Failed to reverse related quantities" + e.getMessage());
			m_processMsg = "反向冲减相关数量异常: " + e.getMessage();
			return false;
		}
	}

	/**
	 * 回滚成本差异记录
	 */
	private boolean reverseCostVariances() {
		try {
			// 查找相关的差异成本收集器
			String whereClause = "PP_Order_ID = ? " + "AND (PP_Order_BOMLine_ID = ? OR PP_Order_Node_ID = ?) "
					+ "AND CostCollectorType IN ('120', '130', '140', '150') " + "AND DocStatus IN ('CO', 'CL') "
					+ "AND Created >= ?";

			Collection<MPPCostCollector> varianceCollectors = new Query(getCtx(), MPPCostCollector.Table_Name,
					whereClause, get_TrxName())
					.setParameters(new Object[] { getPP_Order_ID(), getPP_Order_BOMLine_ID(), getPP_Order_Node_ID(),
							getCreated() })
					.list();

			// 按创建时间倒序处理（后创建的先回滚）
			for (MPPCostCollector variance : varianceCollectors) {
				if (!variance.voidIt()) {
					m_processMsg = "回滚差异成本收集器失败: " + variance.getDocumentNo();
					return false;
				}
			}

			return true;
		} catch (Exception e) {
			log.severe("回滚成本差异失败: " + e.getMessage());
			m_processMsg = "回滚成本差异异常: " + e.getMessage();
			return false;
		}
	}

	/**
	 * 删除成本明细记录
	 */
	private boolean deleteCostDetails() {
		try {
			// 查找当前成本收集器的所有成本明细
			String whereClause = "PP_Cost_Collector_ID = ?";
			Collection<MCostDetail> costDetails = new Query(getCtx(), MCostDetail.Table_Name, whereClause,
					get_TrxName()).setParameters(new Object[] { get_ID() }).list();

			for (MCostDetail cd : costDetails) {
				if (!cd.isProcessed()) {
					// 未处理的成本明细可以直接删除
					if (!cd.delete(true)) {
						m_processMsg = "删除成本明细失败: " + cd.getM_CostDetail_ID();
						return false;
					}
				} else {
					// 已处理的成本明细需要创建冲销记录
					MCostDetail reversal = new MCostDetail(getCtx(), 0, get_TrxName());
					MCostDetail.copyValues(cd, reversal);
					reversal.setProcessed(false);
					reversal.setAmt(cd.getAmt().negate());
					reversal.setQty(cd.getQty().negate());
					reversal.setPP_Cost_Collector_ID(get_ID());
					reversal.setDescription("Reversal of: " + cd.getM_CostDetail_ID());
					if (!reversal.save()) {
						m_processMsg = "创建成本明细冲销失败";
						return false;
					}
					reversal.process();
				}
			}

			return true;
		} catch (Exception e) {
			log.severe("删除成本明细失败: " + e.getMessage());
			m_processMsg = "删除成本明细异常: " + e.getMessage();
			return false;
		}
	}

	/**
	 * 创建冲销的成本明细
	 */
	private boolean createReversalCostDetails(MPPCostCollector reversal) {
		try {
			// 查找原始成本收集器的成本明细
			String whereClause = "PP_Cost_Collector_ID = ?";
			Collection<MCostDetail> originalDetails = new Query(getCtx(), MCostDetail.Table_Name, whereClause,
					get_TrxName()).setParameters(new Object[] { get_ID() }).list();

			for (MCostDetail original : originalDetails) {
				// 为冲销单据创建相反的成本明细
				MCostDetail reversalDetail = new MCostDetail(getCtx(), 0, get_TrxName());
				MCostDetail.copyValues(original, reversalDetail);
				
	            // 先设置未处理状态  
	            reversalDetail.setProcessed(false);  
				reversalDetail.setPP_Cost_Collector_ID(reversal.get_ID());
				reversalDetail.setAmt(original.getAmt().negate());
				reversalDetail.setQty(original.getQty().negate());
				reversalDetail.setProcessed(false);
				reversalDetail.setDescription("Reversal of CC: " + getDocumentNo());

				if (!reversalDetail.save()) {
					m_processMsg = "保存冲销成本明细失败";
					return false;
				}

				// 处理成本明细
//				reversalDetail.process();
				// 优化：直接标记为已处理，跳过成本计算验证
				reversalDetail.setProcessed(true);
				reversalDetail.saveEx();
			}

			return true;
		} catch (Exception e) {
			log.severe("创建冲销成本明细失败: " + e.getMessage());
			m_processMsg = "创建冲销成本明细异常: " + e.getMessage();
			return true;
		}
	}

	/**
	 * 更新活动控制 - 增强版本，包含状态回滚
	 */
	private void updateActivityControl() {
		MPPOrderNode activity = getPP_Order_Node();
		if (activity != null) {
			// 回滚活动控制的数量和时间
			activity.setQtyDelivered(activity.getQtyDelivered().subtract(getMovementQty()));
			activity.setQtyScrap(activity.getQtyScrap().subtract(getScrappedQty()));
			activity.setQtyReject(activity.getQtyReject().subtract(getQtyReject()));

			// 回滚实际时间
			if (getDurationReal().signum() != 0) {
				activity.setDurationReal(activity.getDurationReal().subtract(getDurationReal()));
			}
			if (getSetupTimeReal().signum() != 0) {
				activity.setSetupTimeReal(activity.getSetupTimeReal().subtract(getSetupTimeReal()));
			}

			// 关键：回滚活动状态
			if (activity.getQtyDelivered().compareTo(activity.getQtyRequiered()) < 0) {
				// 如果回滚后数量小于需求数量，重新激活活动
				activity.setDocStatus(MPPOrderNode.DOCSTATUS_InProgress);
				activity.setDocAction(MPPOrderNode.DOCACTION_Complete);
			}

			activity.saveEx(get_TrxName());
		}
	}
    public void setIsReversal(boolean isReversal){
        this.isReversal = isReversal;
    }

    public boolean isReversal(){
        return this.isReversal;
    }

	/**
	 * 校验机台占用情况
	 */
	private boolean validateResourceAvailability() {
		// 只对活动控制类型的报工单进行校验
		if (!isActivityControl()) {
			return true;
		}
		// 如果报工单不为草稿，跳过校验
		if (!DOCSTATUS_Drafted.equals(getDocStatus())) {
			return true;
		}

		// 检查是否为打样工单，如果是则跳过机台校验
		MPPOrder order = getPP_Order();
		if (order != null) {
			MDocType docType = MDocType.get(getCtx(), order.getC_DocTypeTarget_ID());
			if (docType != null && docType.getName().contains("打样")) {
				return true; // 打样工单跳过机台校验
			}
		}

		// 获取当前机台
		int resourceId = getS_Resource_ID();
		if (resourceId <= 0) {
			return true; // 没有机台则跳过校验
		}

		// 查询同一机台的非已完成报工单
		String sql = "SELECT COUNT(*) FROM PP_Cost_Collector " + "WHERE S_Resource_ID = ? "
				+ "AND DocStatus ='DR' " + // 只查找草稿
				"AND PP_Cost_Collector_ID != ? " + // 排除当前记录
				"AND CostCollectorType = '160'"; // 只检查活动控制类型

		int count = DB.getSQLValue(get_TrxName(), sql, resourceId, getPP_Cost_Collector_ID());

		if (count > 0) {
			// 获取机台名称
			String resourceName = MResource.get(getCtx(), resourceId).getName();
			log.saveError("Error", "机台[" + resourceName + "]还有其他未完成的报工单，请先完成后再创建新的报工单");
			return false;
		}

		return true;
	}

	/**
	 * 校验报工数量不能超过上工序报工数量
	 */
	private boolean validateReportingQuantity() {
		// 只对活动控制类型的报工单进行校验
		if (!isActivityControl()) {
			return true;
		}

		// 检查是否为打样工单，如果是则跳过所有校验
		MPPOrder order = getPP_Order();
		if (order != null) {
			MDocType docType = MDocType.get(getCtx(), order.getC_DocTypeTarget_ID());
			if (docType != null && docType.getName().contains("打样工单")) {
				return true; // 打样工单跳过所有数量校验
			}
		}
		
		// 获取当前工序
		int currentNodeId = getPP_Order_Node_ID();
		if (currentNodeId <= 0) {
			return true;
		}

		MPPOrderNode currentNode = new MPPOrderNode(getCtx(), currentNodeId, get_TrxName());
		MPPOrderWorkflow workflow = getPP_Order().getMPPOrderWorkflow();
		BigDecimal firstmaxAllowedQty;
		String errorMsg;

		// 判断是否为第一个工序
		if (workflow.isFirst(currentNodeId, getAD_Client_ID())) {
			order = getPP_Order();
			if (order != null) {
				// 获取目标文档类型
				MDocType docType = MDocType.get(getCtx(), order.getC_DocTypeTarget_ID());
				// 通过名称判断是否为打样工单
				if (docType != null && docType.getName().contains("打样工单")) {
					return true; // 打样工单跳过数量校验
				}
			}

			// 第一个工序：使用标准加工数作为上限
			firstmaxAllowedQty = currentNode.getQtyRequiered();

			errorMsg = "报工数量(" + getMovementQty() + ")不能超过标准加工数(" + firstmaxAllowedQty + ")";

			// 只有当报工数量超过标准加工数时才抛出异常
			if (getMovementQty().compareTo(firstmaxAllowedQty) > 0) {
				log.saveError("Error", errorMsg);
				throw new AdempiereException(errorMsg);
			}
		}

		// 查找上一个工序的报工数量
		BigDecimal previousNodeQty = getPreviousNodeReportingQuantity(currentNode);
		if (previousNodeQty == null) {
			return true; // 没有上工序，跳过校验
		}

		// 获取当前工序的按批计算设置
		boolean isBatchCalculation = isBatchCalculationNode(currentNode);
		BigDecimal maxAllowedQty = previousNodeQty;

		// 如果当前工序按批计算，需要乘以拼版数
		if (isBatchCalculation) {
			BigDecimal batchSize = getBatchSize(currentNode);
			if (batchSize != null && batchSize.signum() > 0) {
				maxAllowedQty = previousNodeQty.multiply(batchSize);
			}
		}

		// 校验当前报工数量
		if (getMovementQty().compareTo(maxAllowedQty) > 0) {
			String errorMsg1 = "报工数量(" + getMovementQty() + ")不能超过";
			if (isBatchCalculation) {
				errorMsg1 += "上工序报工数量(" + previousNodeQty + ")×拼版数(" + getBatchSize(currentNode) + ")=" + maxAllowedQty;
			} else {
				errorMsg1 += "上工序报工数量(" + previousNodeQty + ")";
			}
			log.saveError("Error", errorMsg1);

			throw new AdempiereException(errorMsg1);

		}

		return true;
	}

	/**
	 * 获取上一个工序的报工数量
	 */
	private BigDecimal getPreviousNodeReportingQuantity(MPPOrderNode currentNode) {
		// 获取工作流中上一个工序
		MPPOrderWorkflow workflow = currentNode.getMPPOrderWorkflow();
		MPPOrderNode[] allNodes = workflow.getNodes(true, getAD_Client_ID());
		MPPOrderNode previousNode = getPreviousNode(allNodes, currentNode);
		if (previousNode == null) {
			return null; // 没有上一个工序
		}
		int previousNodeId = previousNode.getPP_Order_Node_ID();

		if (previousNodeId <= 0) {
			return null; // 没有上一个工序
		}



		return previousNode.getQtyDelivered();
	}

	/**
	 * 获取按value字段排序后的上一个节点
	 */
	public MPPOrderNode getPreviousNode(MPPOrderNode[] allNodes, MPPOrderNode currentNode) {
		if (allNodes == null || allNodes.length == 0 || currentNode == null) {
			return null;
		}

		// 1. 先按value字段从小到大排序
		List<MPPOrderNode> sortedList = new ArrayList<>(Arrays.asList(allNodes));

		sortedList.sort((node1, node2) -> {
			Object value1 = node1.getValue();
			Object value2 = node2.getValue();

			// 处理null值
			if (value1 == null && value2 == null) {
				return 0;
			} else if (value1 == null) {
				return -1; // null值放在前面
			} else if (value2 == null) {
				return 1;
			}

			// 假设value是Comparable类型
			if (value1 instanceof Comparable && value2 instanceof Comparable) {
				return ((Comparable) value1).compareTo(value2);
			}

			// 如果不是Comparable，则转换为字符串比较
			return value1.toString().compareTo(value2.toString());
		});

		// 2. 在排序后的列表中查找当前节点
		int currentIndex = -1;
		for (int i = 0; i < sortedList.size(); i++) {
			MPPOrderNode node = sortedList.get(i);
			if (node.getPP_Order_Node_ID() == currentNode.getPP_Order_Node_ID()) { // 比较对象引用
				currentIndex = i;
				break;
			}
		}

		// 3. 返回上一个节点
		if (currentIndex > 0) {
			return sortedList.get(currentIndex - 1);
		}
		return null; // 当前节点是第一个节点，或者没找到
	}

	/**
	 * 判断工序是否按批计算
	 */
	private boolean isBatchCalculationNode(MPPOrderNode node) {
		if (node.getAD_Routing_Node_ID() <= 0) {
			return false;
		}

		String sql = "SELECT IsBatchCalculation " + "FROM AD_Routing_Node " + "WHERE AD_Routing_Node_ID = ?";

		String isBatch = DB.getSQLValueString(get_TrxName(), sql, node.getAD_Routing_Node_ID());
		return "Y".equals(isBatch);
	}

	/**
	 * 获取拼版数
	 */
	private BigDecimal getBatchSize(MPPOrderNode node) {
		// 从工作流或工序中获取拼版数
		MPPOrderWorkflow workflow = node.getMPPOrderWorkflow();
		if (workflow != null) {
			return workflow.getQtyBatchSize();
		}
		return Env.ONE;
	}

}	//	MPPCostCollector