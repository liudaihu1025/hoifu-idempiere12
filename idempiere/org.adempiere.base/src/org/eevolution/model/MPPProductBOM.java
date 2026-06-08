/******************************************************************************  
 * Product: Adempiere ERP & CRM Smart Business Solution                       *  
 * This program is free software; you can redistribute it and/or modify it    *  
 * under the terms version 2 of the GNU General Public License as published   *  
 * by the Free Software Foundation. This program is distributed in the hope   *  
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *  
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *  
 * See the GNU General Public License for more details.                       *  
 * You should have received a copy of the GNU General Public License along    *  
 * with this program, if not, write to the Free Software Foundation, Inc.,    *  
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *  
 * Copyright (C) 2003-2007 e-Evolution,SC. All Rights Reserved.               *  
 * Contributor(s): Victor Perez www.e-evolution.com                           *  
 *                 Teo Sarca, http://www.arhipac.ro                           *  
 *****************************************************************************/
package org.eevolution.model;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.compiere.model.MProduct;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.Query;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.process.ProcessInfo;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.wf.MWorkflow;
import org.idempiere.cache.ImmutableIntPOCache;
import org.idempiere.cache.ImmutablePOSupport;

/**
 * PP Product BOM Model with Document Status Management
 * 
 * @author Victor Perez www.e-evolution.com
 * @author Teo Sarca, http://www.arhipac.ro
 */
public class MPPProductBOM extends X_PP_Product_BOM implements DocAction, ImmutablePOSupport {
	private static final long serialVersionUID = -5048325803007991296L;

	/** Cache */
	private static ImmutableIntPOCache<Integer, MPPProductBOM> s_cache = new ImmutableIntPOCache<Integer, MPPProductBOM>(
			Table_Name, 40, 5);

	/** BOM Lines */
	private List<MPPProductBOMLine> m_lines = null;

	/** Process Message */
	private String m_processMsg = null;

	/** Just Prepared Flag */
	private boolean m_justPrepared = false;

	// ========== 原有静态方法保持不变 ==========

	public static boolean isProductMakeToOrder(Properties ctx, int productId, String trxName) {
		final String whereClause = MPPProductBOM.COLUMNNAME_BOMType + " IN (?,?)" + " AND "
				+ MPPProductBOM.COLUMNNAME_BOMUse + "=?" + " AND " + MPPProductBOM.COLUMNNAME_M_Product_ID + "=?";
		return new Query(ctx, MPPProductBOM.Table_Name, whereClause, trxName).setClient_ID()
				.setParameters(MPPProductBOM.BOMTYPE_Make_To_Order, MPPProductBOM.BOMTYPE_Make_To_Kit,
						MPPProductBOM.BOMUSE_Manufacturing, productId)
				.match();
	}

	public static List<MPPProductBOM> getProductBOMs(MProduct product) {
		String whereClause = "M_Product_ID=?";
		return new Query(product.getCtx(), X_PP_Product_BOM.Table_Name, whereClause, product.get_TrxName())
				.setClient_ID().setParameters(product.getM_Product_ID()).setOnlyActiveRecords(true).list();
	}

	public static MPPProductBOM get(int PP_Product_BOM_ID) {
		return get(Env.getCtx(), PP_Product_BOM_ID);
	}

	public static MPPProductBOM get(Properties ctx, int PP_Product_BOM_ID) {
		if (PP_Product_BOM_ID <= 0)
			return null;
		MPPProductBOM bom = s_cache.get(ctx, PP_Product_BOM_ID, e -> new MPPProductBOM(ctx, e));
		if (bom != null)
			return bom;
		bom = new MPPProductBOM(ctx, PP_Product_BOM_ID, (String) null);
		if (bom.get_ID() == PP_Product_BOM_ID) {
			s_cache.put(PP_Product_BOM_ID, bom, e -> new MPPProductBOM(Env.getCtx(), e));
			return bom;
		}
		return null;
	}

	public static MPPProductBOM getCopy(Properties ctx, int PP_Product_BOM_ID, String trxName) {
		MPPProductBOM bom = get(PP_Product_BOM_ID);
		if (bom != null)
			bom = new MPPProductBOM(ctx, bom, trxName);
		return bom;
	}

	public static int getBOMSearchKey(MProduct product) {
		int AD_Client_ID = Env.getAD_Client_ID(product.getCtx());
		String sql = "SELECT PP_Product_BOM_ID FROM PP_Product_BOM"
				+ " WHERE Value=? AND M_Product_ID=? AND AD_Client_ID=?";
		return DB.getSQLValueEx(null, sql, product.getValue(), product.get_ID(), AD_Client_ID);
	}

	public static MPPProductBOM getDefault(MProduct product, String trxName) {
		MPPProductBOM bom = null;
		int AD_Org_ID = Env.getAD_Org_ID(Env.getCtx());
		String filter = "M_Product_ID=? AND " + COLUMNNAME_BOMUse + "=? AND " + COLUMNNAME_BOMType + "=? ";
		if (AD_Org_ID > 0) {
			filter += "AND AD_Org_ID IN (0, " + AD_Org_ID + ") ";
		}
		Query query = new Query(product.getCtx(), Table_Name, filter, trxName)
				.setParameters(new Object[] { product.getM_Product_ID(), BOMUSE_Master, BOMTYPE_CurrentActive })
				.setOnlyActiveRecords(true).setClient_ID();
		if (AD_Org_ID > 0)
			query.setOrderBy("AD_Org_ID Desc");

		List<MPPProductBOM> list = query.list();
		if (!list.isEmpty()) {
			if (AD_Org_ID > 0 || list.size() == 1) {
				bom = list.get(0);
			}
		}
		if (bom != null && trxName == null) {
			s_cache.put(bom.get_ID(), bom);
		}
		return bom;
	}

	public static MPPProductBOM get(MProduct product, int ad_org_id, String trxName) {
		MPPProductBOM bom = null;
		Properties ctx = product.getCtx();
		if (ad_org_id > 0) {
			MPPProductPlanning pp = MPPProductPlanning.get(ctx, product.getAD_Client_ID(), ad_org_id,
					product.getM_Product_ID(), trxName);
			if (pp != null && pp.getPP_Product_BOM_ID() > 0) {
				bom = new MPPProductBOM(ctx, pp.getPP_Product_BOM_ID(), trxName);
			}
		}
		if (bom == null) {
			bom = getDefault(product, trxName);
		}
		return bom;
	}

	public static MPPProductBOM get(MProduct product, int ad_org_id, Timestamp valid, String trxName) {
		MPPProductBOM bom = get(product, ad_org_id, trxName);
		if (bom != null && bom.isValidFromTo(valid)) {
			return bom;
		}
		return null;
	}

	// ========== 构造函数 ==========

	public MPPProductBOM(Properties ctx, String PP_Product_BOM_UU, String trxName) {
		super(ctx, PP_Product_BOM_UU, trxName);
	}

	public MPPProductBOM(Properties ctx, int PP_Product_BOM_ID, String trxName) {
		super(ctx, PP_Product_BOM_ID, trxName);
		if (PP_Product_BOM_ID == 0) {
			setDocStatus(DocAction.STATUS_Drafted);
			setDocAction(DocAction.ACTION_Complete);
			setBOMStatus("InProgress");
		}
	}

	public MPPProductBOM(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	public MPPProductBOM(MPPProductBOM copy) {
		this(Env.getCtx(), copy);
	}

	public MPPProductBOM(Properties ctx, MPPProductBOM copy) {
		this(ctx, copy, (String) null);
	}

	public MPPProductBOM(Properties ctx, MPPProductBOM copy, String trxName) {
		this(ctx, 0, trxName);
		copyPO(copy);
		this.m_lines = copy.m_lines != null ? copy.m_lines.stream().map(e -> new MPPProductBOMLine(ctx, e, trxName))
				.collect(Collectors.toCollection(ArrayList::new)) : null;
	}

	public MPPProductBOM(Properties ctx, int PP_Product_BOM_ID, String trxName, String... virtualColumns) {
		super(ctx, PP_Product_BOM_ID, trxName, virtualColumns);
	}

	// ========== 原有BOM行管理方法 ==========

	public MPPProductBOMLine[] getLines(Timestamp valid) {
		List<MPPProductBOMLine> list = new ArrayList<MPPProductBOMLine>();
		for (MPPProductBOMLine bl : getLines(true)) {
			if (bl.isValidFromTo(valid)) {
				list.add(bl);
			}
		}
		return list.toArray(new MPPProductBOMLine[list.size()]);
	}

	public MPPProductBOMLine[] getLines() {
		return getLines(false);
	}

	public MPPProductBOMLine[] getLines(boolean reload) {
		if (this.m_lines == null || reload) {
			final String whereClause = MPPProductBOMLine.COLUMNNAME_PP_Product_BOM_ID + "=?";
			this.m_lines = new Query(getCtx(), MPPProductBOMLine.Table_Name, whereClause, get_TrxName())
					.setParameters(new Object[] { getPP_Product_BOM_ID() }).setClient_ID().setOnlyActiveRecords(true)
					.setOrderBy(MPPProductBOMLine.COLUMNNAME_Line).list();
			if (m_lines.size() > 0 && is_Immutable())
				m_lines.stream().forEach(e -> e.markImmutable());
		}
		return this.m_lines.toArray(new MPPProductBOMLine[this.m_lines.size()]);
	}

	public boolean isValidFromTo(Timestamp date) {
		Timestamp validFrom = getValidFrom();
		Timestamp validTo = getValidTo();

		if (validFrom != null && date.before(validFrom))
			return false;
		if (validTo != null && date.after(validTo))
			return false;
		return true;
	}

	// ========== 原有验证和保存方法 ==========

	@Override
	protected boolean afterDelete(boolean success) {
		if (!success)
			return false;
		updateProduct();
		return true;
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		boolean b = super.beforeSave(newRecord);
		if (b) {
			if (newRecord && getDocStatus() == null) {
				setDocStatus(DocAction.STATUS_Drafted);
				setDocAction(DocAction.ACTION_Complete);
				setBOMStatus("InProgress");
			}

			if (BOMTYPE_CurrentActive.equals(getBOMType()) && BOMUSE_Master.equals(getBOMUse()) && isActive()) {
				if (newRecord || is_ValueChanged(COLUMNNAME_BOMType) || is_ValueChanged(COLUMNNAME_BOMUse)
						|| is_ValueChanged(COLUMNNAME_IsActive) || is_ValueChanged(COLUMNNAME_M_Product_ID)) {
					int id = DB.getSQLValue(get_TrxName(),
							"SELECT PP_Product_BOM_ID FROM PP_Product_BOM WHERE M_Product_ID=? AND BOMType='A' AND BOMUse='A' AND IsActive='Y'  AND PP_Product_BOM_ID != ? AND AD_Org_ID=?",
							getM_Product_ID(), getPP_Product_BOM_ID(), getAD_Org_ID());
					if (id > 0) {
						b = false;
						CLogger.getCLogger(getClass()).saveError("OnlyOneCurrentActiveMasterBOM", "");
					}
				}
			}
		}
		return b;
	}

	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return false;

		if (newRecord || is_ValueChanged("IsActive")) {
			updateProduct();
		}

		MProduct product = new MProduct(getCtx(), getM_Product_ID(), get_TrxName());
		if (product.isBOM() && product.isVerified()) {
			if ((BOMTYPE_CurrentActive.equals(getBOMType()) && BOMUSE_Master.equals(getBOMUse()))
					|| (BOMTYPE_CurrentActive.equals(get_ValueOld(COLUMNNAME_BOMType))
							&& BOMUSE_Master.equals(get_ValueOld(COLUMNNAME_BOMUse)))) {
				if (is_ValueChanged(COLUMNNAME_IsActive) || is_ValueChanged(COLUMNNAME_BOMType)
						|| is_ValueChanged(COLUMNNAME_BOMUse) || newRecord) {
					product.setIsVerified(false);
					product.saveEx();
				}
			}
		}
		return true;
	}

	private void updateProduct() {
		int count = new Query(getCtx(), Table_Name, COLUMNNAME_M_Product_ID + "=?", get_TrxName())
				.setParameters(new Object[] { getM_Product_ID() }).setClient_ID().setOnlyActiveRecords(true).count();
		MProduct product = new MProduct(getCtx(), getM_Product_ID(), get_TrxName());
		product.setIsBOM(count > 0);
		product.saveEx();
	}

	@Override
	public MPPProductBOM markImmutable() {
		if (is_Immutable())
			return this;

		makeImmutable();
		if (m_lines != null && m_lines.size() > 0)
			m_lines.stream().forEach(e -> e.markImmutable());
		return this;
	}

	// ========== DocAction 接口实现 ==========

	@Override
	public boolean processIt(String processAction) {
		m_processMsg = null;
		DocumentEngine engine = new DocumentEngine(this, getDocStatus());
		return engine.processIt(processAction, getDocAction());
	}

	@Override
	public boolean unlockIt() {
		if (log.isLoggable(Level.INFO))
			log.info("unlockIt - " + toString());
		setProcessing(false);
		return true;
	}

	@Override
	public boolean invalidateIt() {
		if (log.isLoggable(Level.INFO))
			log.info(toString());
		setDocAction(DocAction.ACTION_Prepare);
		return true;
	}

	@Override
	public String prepareIt() {
		if (log.isLoggable(Level.INFO))
			log.info(toString());

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		if (!validateBOMStructure()) {
			return DocAction.STATUS_Invalid;
		}

		// setSubmittedBy(Env.getAD_User_ID(getCtx()));
		setSubmittedTime(new Timestamp(System.currentTimeMillis()));
		setDocStatus(DocAction.STATUS_InProgress);
		setBOMStatus("PendingApproval");

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		m_justPrepared = true;
		if (!DocAction.ACTION_Complete.equals(getDocAction()))
			setDocAction(DocAction.ACTION_Complete);
		// 使用集中状态管理
		syncBOMStatus();

		return DocAction.STATUS_InProgress;
	}

	@Override
	public boolean approveIt() {
		if (log.isLoggable(Level.INFO))
			log.info("approveIt - " + toString());
		setIsApproved(true);
		setApprovedBy(Env.getAD_User_ID(getCtx()));
		setApprovedTime(new Timestamp(System.currentTimeMillis()));
		return true;
	}

	@Override
	public boolean rejectIt() {
		if (log.isLoggable(Level.INFO))
			log.info("rejectIt - " + toString());
		setIsApproved(false);
		setDocStatus(DocAction.STATUS_Drafted);
		setBOMStatus("InProgress");

		// 重新设置状态，覆盖DocumentEngine的设置
		setDocStatus(DocAction.STATUS_Drafted);
		setBOMStatus("InProgress");
		return true;
	}

	@Override
	public String completeIt() {
		if (!m_justPrepared) {
			String status = prepareIt();
			m_justPrepared = false;
			if (!DocAction.STATUS_InProgress.equals(status))
				return status;
		}

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
		if (m_processMsg != null)
			return DocAction.STATUS_Invalid;

		if (!isApproved()) {
			approveIt();
		}

		if (log.isLoggable(Level.INFO)) {
			log.info(toString());
		}
		setBOMStatus("Released");
		// 创建历史记录并更新版本
		createHistoryAndUpdateVersion();

		// 设置完成状态
		setDocStatus(DocAction.STATUS_Completed);
		// 直接设置目标状态，避免被覆盖
		setDocAction(DocAction.ACTION_Close);

		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
		if (m_processMsg != null) {
			return DocAction.STATUS_Invalid;
		}

		return DocAction.STATUS_Completed;
	}

	@Override
	public boolean voidIt() {
		if (log.isLoggable(Level.INFO))
			log.info(toString());
		setDocStatus(DocAction.STATUS_Voided);
		setBOMStatus("Inactive");
		setProcessed(true);
		setDocAction(DocAction.ACTION_None);
		return true;
	}

	@Override
	public boolean closeIt() {
		if (log.isLoggable(Level.INFO))
			log.info("closeIt - " + toString());
		setDocAction(DocAction.ACTION_None);
		return true;
	}

	@Override
	public boolean reverseCorrectIt() {
		if (log.isLoggable(Level.INFO))
			log.info("reverseCorrectIt - " + toString());
		return false;
	}

	@Override
	public boolean reverseAccrualIt() {
		if (log.isLoggable(Level.INFO))
			log.info("reverseAccrualIt - " + toString());
		return false;
	}

	@Override  
	public boolean reActivateIt() {  
	    if (log.isLoggable(Level.INFO))  
	        log.info("reActivateIt - " + toString());  
	  
	    // Before reActivate  
	    m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_REACTIVATE);  
	    if (m_processMsg != null)  
	        return false;  
	  
	    // 验证文档类型是否支持重新激活  
	    if (getC_DocType_ID() > 0 && !DocumentEngine.canReactivateThisDocType(getC_DocType_ID())) {  
	        m_processMsg = Msg.getMsg(getCtx(), "DocTypeCannotBeReactivated");  
	        return false;  
	    }  
	  
		// BOM变更时设置状态：单据状态为草稿，BOM状态为处理中
		setDocStatus(DocAction.STATUS_Drafted); // 改为草稿状态
		setBOMStatus("InChange"); // 直接设置目标状态，避免被覆盖
		setProcessed(false);
	    setDocAction(DocAction.ACTION_Complete);  
	  
	    // 使用集中状态管理  
		// syncBOMStatus();
	  
	    // After reActivate  
	    m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_REACTIVATE);  
	    if (m_processMsg != null)  
	        return false;  
	  
	    return true;  
	}
	// ========== DocAction Support Methods ==========

	@Override
	public String getSummary() {
		StringBuilder sb = new StringBuilder();
		sb.append(getName());
		if (getRevision() != null)
			sb.append(" - ").append(getRevision());
		sb.append(" - ").append(getBOMStatus());
		return sb.toString();
	}

	@Override
	public String getProcessMsg() {
		return m_processMsg;
	}

	@Override
	public int getDoc_User_ID() {
		return getCreatedBy();
	}

	@Override
	public int getC_Currency_ID() {
		return 0;
	}

	@Override
	public java.math.BigDecimal getApprovalAmt() {
		return Env.ZERO;
	}

	@Override
	public String getDocumentInfo() {
		return "Product BOM " + getName();
	}

	@Override
	public File createPDF() {
		return null;
	}

	// ========== BOM Status Management ==========

	/**
	 * 验证BOM结构
	 */
	public boolean validateBOMStructure() {
		// 检查产品是否设置
		if (getM_Product_ID() == 0) {
			m_processMsg = "产品不能为空";
			return false;
		}

		// 检查BOM行是否存在
		MPPProductBOMLine[] lines = getLines();
		if (lines == null || lines.length == 0) {
			m_processMsg = "BOM行不能为空";
			return false;
		}

		// 验证每个BOM行
		for (MPPProductBOMLine line : lines) {
			if (!line.isActive())
				continue;

			if (line.getM_Product_ID() == 0) {
				m_processMsg = "BOM行产品不能为空";
				return false;
			}
		}

		return true;
	}

	/**
	 * 同步BOM状态
	 */
	private void syncBOMStatus() {
		String docStatus = getDocStatus();
		String bomStatus = null;

		if (DocAction.STATUS_Drafted.equals(docStatus)) {
			bomStatus = "InProgress";
		} else if (DocAction.STATUS_InProgress.equals(docStatus)) {
			bomStatus = "PendingApproval";
		} else if (DocAction.STATUS_Completed.equals(docStatus)) {
			bomStatus = "Released";
		} else if (DocAction.STATUS_Voided.equals(docStatus)) {
			bomStatus = "Inactive";
		}
		setBOMStatus(bomStatus);
	}

	/**
	 * 提交审核
	 */
	public String submitForApproval() {
		// 验证BOM结构
		if (!validateBOMStructure()) {
			return m_processMsg;
		}

		// 运行工作流
		ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(this, DocAction.ACTION_Prepare);
		if (pi.isError()) {
			return pi.getSummary();
		}

		// 检查工作流是否成功执行
		if (!pi.isError()) {
			String sql = "SELECT AD_WF_Process_ID FROM AD_WF_Process WHERE AD_Table_ID=? AND Record_ID=?";
			int wfProcessID = DB.getSQLValue(null, sql, get_Table_ID(), get_ID());
			if (wfProcessID > 0) {
				setAD_WF_Process_ID(wfProcessID);
			}
		}

		saveEx();
		return "提交成功";
	}

	/**
	 * 申请变更
	 */
	public String requestChange() {
		// 运行重新激活工作流
		ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(this, DocAction.ACTION_ReActivate);
		if (pi.isError()) {
			return pi.getSummary();
		}

		// 更新版本号
		String currentRevision = getRevision();
		if (currentRevision == null || currentRevision.isEmpty()) {
			setRevision("V1.0");
		} else {
			// 简单的版本号递增逻辑
			if (currentRevision.startsWith("V")) {
				try {
					String[] parts = currentRevision.substring(1).split("\\.");
					if (parts.length == 2) {
						int major = Integer.parseInt(parts[0]);
						int minor = Integer.parseInt(parts[1]);
						minor++;
						setRevision("V" + major + "." + minor);
					}
				} catch (Exception e) {
					setRevision(currentRevision + ".1");
				}
			} else {
				setRevision(currentRevision + ".1");
			}
		}

		saveEx();
		return "变更申请成功";
	}

	public String getDocAction() {
		return (String) get_Value(COLUMNNAME_DocAction);
	}

	/**
	 * 创建历史记录并更新版本号
	 * 
	 * @return 操作结果信息
	 */
	/**
	 * 创建历史记录并更新版本号
	 * 
	 * @return 操作结果信息
	 */
	public String createHistoryAndUpdateVersion() {
		try {
			// 1. 处理版本号 - 确保没有版本号的BOM也能创建历史记录
			String currentRevision = getRevision();
			String newRevision = null;
			String historyRevision = null;

			if (currentRevision == null || currentRevision.trim().isEmpty()) {
				// 没有版本号的情况：给当前BOM设为V1.0，历史记录也为V1.0
				currentRevision = "V1.0";
				setRevision(currentRevision); // 设置当前BOM版本为V1.0
				newRevision = "V1.0"; // 下一个版本为V1.0
				historyRevision = "V1.0"; // 历史版本为V1.0
			} else {
				// 有版本号的情况：正常计算下一个版本
				newRevision = calculateNextRevision(currentRevision);
				historyRevision = currentRevision; // 历史版本为当前版本
			}

			// 2. 创建历史BOM记录
			MPPProductBOM historyBOM = createHistoryBOM(historyRevision);

			// 3. 复制BOM行到历史记录
			copyBOMLinesToHistory(historyBOM);

			// 4. 更新当前BOM版本号
			setRevision(newRevision);
			saveEx();

			return "历史记录创建成功，新版本号: " + newRevision;

		} catch (Exception e) {
			log.log(Level.SEVERE, "创建历史记录失败", e);
			return "创建历史记录失败: " + e.getMessage();
		}
	}

	/**
	 * 计算下一个版本号
	 */
	private String calculateNextRevision(String currentRevision) {
		if (currentRevision == null || currentRevision.isEmpty()) {
			return "V1.0";
		}

		if (currentRevision.startsWith("V")) {
			try {
				String[] parts = currentRevision.substring(1).split("\\.");
				if (parts.length == 2) {
					int major = Integer.parseInt(parts[0]);
					int minor = Integer.parseInt(parts[1]);
					minor++;
					return "V" + major + "." + minor;
				}
			} catch (Exception e) {
				return currentRevision + ".1";
			}
		}

		return currentRevision + ".1";
	}

	/**
	 * 创建历史BOM记录
	 */
	private MPPProductBOM createHistoryBOM(String currentRevision) {
		MPPProductBOM historyBOM = new MPPProductBOM(getCtx(), 0, get_TrxName());

		// 复制所有必要字段
		historyBOM.setAD_Client_ID(getAD_Client_ID());
		historyBOM.setAD_Org_ID(getAD_Org_ID());
		historyBOM.setM_Product_ID(getM_Product_ID());
		historyBOM.setM_AttributeSetInstance_ID(getM_AttributeSetInstance_ID());

		// 设置历史BOM的Value = 原Value + "_HIST_" + 当前版本号 + 时间戳，确保唯一性
		String originalValue = getValue();
		String timestamp = String.valueOf(System.currentTimeMillis());
		historyBOM.setValue(originalValue + "_HIST_" + currentRevision + "_" + timestamp);

		historyBOM.setBOMStatus("Inactive");
		historyBOM.setName(getName());
		historyBOM.setDescription(getDescription());
		historyBOM.setIsActive(false);
		historyBOM.setRevision(currentRevision);
		historyBOM.setBOMType("Z"); // 历史有效
		historyBOM.setBOMUse(getBOMUse());
		historyBOM.setSubmittedBy(getSubmittedBy());
		historyBOM.setDocStatus(getDocStatus());
		historyBOM.setHelp(getHelp());
		historyBOM.saveEx();
		return historyBOM;
	}

	/**
	 * 复制BOM行到历史记录
	 */
	private void copyBOMLinesToHistory(MPPProductBOM historyBOM) {
		// 使用直接SQL查询获取BOM明细行ID，避免事务问题
		String lineSQL = "SELECT PP_Product_BOMLine_ID FROM PP_Product_BOMLine "
				+ "WHERE PP_Product_BOM_ID = ? AND IsActive = 'Y'";
		int[] lineIds = DB.getIDsEx(get_TrxName(), lineSQL, getPP_Product_BOM_ID());

		for (int lineId : lineIds) {
			MPPProductBOMLine currentLine = new MPPProductBOMLine(getCtx(), lineId, get_TrxName());
			MPPProductBOMLine historyLine = new MPPProductBOMLine(getCtx(), 0, get_TrxName());

			// 复制BOM行的所有字段
			historyLine.setPP_Product_BOM_ID(historyBOM.getPP_Product_BOM_ID());
			historyLine.setM_Product_ID(currentLine.getM_Product_ID());
			historyLine.setQtyBOM(currentLine.getQtyBOM());
			historyLine.setIsCritical(currentLine.isCritical());
			historyLine.setQtyBatch(currentLine.getQtyBatch());
			historyLine.setComponentType(currentLine.getComponentType());
			historyLine.setScrap(currentLine.getScrap());
			historyLine.setValidFrom(currentLine.getValidFrom());
			historyLine.setValidTo(currentLine.getValidTo());
			historyLine.setLeadTimeOffset(currentLine.getLeadTimeOffset());
			historyLine.setAssay(currentLine.getAssay());
			historyLine.setBackflushGroup(currentLine.getBackflushGroup());
			historyLine.setC_UOM_ID(currentLine.getC_UOM_ID());

			historyLine.saveEx();
		}
	}

	/**
	 * 查找当前BOM的最新历史记录
	 * 
	 * @return 历史BOM对象，如果没有找到返回null
	 */
	private MPPProductBOM findLatestHistoryBOM() {
		// 通过产品ID和历史类型查找，按创建时间倒序排列
		String whereClause = "M_Product_ID = ? AND BOMType = 'Z' AND IsActive = 'N'";

		MPPProductBOM historyBOM = new Query(getCtx(), MPPProductBOM.Table_Name, whereClause, get_TrxName())
				.setParameters(getM_Product_ID()).setOrderBy("Created DESC, Updated DESC").first();

		return historyBOM;
	}

	/**
	 * 从历史记录恢复BOM数据
	 * 
	 * @return 操作结果信息
	 */
	public String restoreFromHistory() {
		try {
			// 1. 查找最新的历史BOM记录
			MPPProductBOM historyBOM = findLatestHistoryBOM();
			if (historyBOM == null) {
				return "未找到历史BOM记录";
			}

			// 2. 复制指定字段
			setName(historyBOM.getName());
			setDescription(historyBOM.getDescription());
			setHelp(historyBOM.getHelp());

			// 3. 删除当前BOM行
			deleteCurrentBOMLines();

			// 4. 复制历史BOM行
			copyBOMLinesFromHistory(historyBOM);

			// 5. 更新状态
			setBOMStatus("Released");
			setDocStatus(DocAction.STATUS_Completed);
			setDocAction(DocAction.ACTION_Close);

			saveEx();

			return "恢复成功，已恢复到当前最新版本: " ;

		} catch (Exception e) {
			log.log(Level.SEVERE, "恢复历史记录失败", e);
			return "恢复失败: " + e.getMessage();
		}
	}

	/**
	 * 删除当前BOM的所有行
	 */
	private void deleteCurrentBOMLines() {
		MPPProductBOMLine[] currentLines = getLines();
		for (MPPProductBOMLine line : currentLines) {
			line.deleteEx(true);
		}
	}

	/**
	 * 从历史BOM复制行数据
	 * 
	 * @param historyBOM 历史BOM对象
	 */
	private void copyBOMLinesFromHistory(MPPProductBOM historyBOM) {
		MPPProductBOMLine[] historyLines = historyBOM.getLines();
		for (MPPProductBOMLine historyLine : historyLines) {
			MPPProductBOMLine newLine = new MPPProductBOMLine(getCtx(), 0, get_TrxName());
			MPPProductBOMLine.copyValues(historyLine, newLine);
			newLine.setPP_Product_BOM_ID(getPP_Product_BOM_ID());
			newLine.saveEx();
		}
	}

	public static boolean verifyProduct(Properties ctx, MProduct product, String trxName, List<String> errors) {  
	    List<MProduct> foundproducts = new ArrayList<>();  
	    List<MProduct> validproducts = new ArrayList<>();  
	    List<MProduct> invalidproducts = new ArrayList<>();  
	    List<MProduct> containinvalidproducts = new ArrayList<>();  
	    List<MProduct> checkedproducts = new ArrayList<>();  
	  
	    if (product.isBOM() && !checkedproducts.contains(product)) {  
	        return validateProductInternal(ctx, product, trxName, foundproducts, validproducts, invalidproducts,  
	                containinvalidproducts, checkedproducts, errors);  
	    }  
	    return false;  
	}  
	  
	private static boolean validateProductInternal(Properties ctx, MProduct product, String trxName,  
	        List<MProduct> foundproducts, List<MProduct> validproducts, List<MProduct> invalidproducts,  
	        List<MProduct> containinvalidproducts, List<MProduct> checkedproducts, List<String> errors) {  
	  
	    if (!product.isBOM())  
	        return false;  
	    if (validproducts.contains(product))  
	        return true;  
	  
	    boolean containsinvalid = false;  
	    boolean invalid = false;  
	    boolean hasKeyMaterial = false;  
	    foundproducts.add(product);  
	  
	    List<MPPProductBOM> boms = MPPProductBOM.getProductBOMs(product);  
	    for (MPPProductBOM bom : boms) {  
	        MPPProductBOMLine[] bomLines = bom.getLines();  
	        int lines = 0;  
	        for (MPPProductBOMLine bomLine : bomLines) {  
	            if (!bomLine.isActive())  
	                continue;  
	            lines++;  
	            try {  
	                if (Boolean.TRUE.equals(bomLine.get_Value("KeyMat")))  
	                    hasKeyMaterial = true;  
	            } catch (Exception e) {  
	                /* skip */  
	            }  
	  
	            MProduct pp = new MProduct(ctx, bomLine.getM_Product_ID(), trxName);  
	            if (pp.isBOM()) {  
	                if (validproducts.contains(pp)) {  
	                    continue;  
	                } else if (invalidproducts.contains(pp)) {  
	                    containsinvalid = true;  
	                } else if (foundproducts.contains(pp)) {  
	                    invalid = true;  
	                    errors.add(Msg.getMsg(ctx, "BOMRecursivelyContains",  
	                            new Object[]{product.getValue(), pp.getValue()}));  
	                } else {  
	                    if (!validateProductInternal(ctx, pp, trxName, foundproducts, validproducts,  
	                            invalidproducts, containinvalidproducts, checkedproducts, errors))  
	                        containsinvalid = true;  
	                }  
	            }  
	        }  
	        if (lines == 0) {  
	            invalid = true;  
	            errors.add(Msg.getMsg(ctx, "BOMForProductDoesNotHaveLines",  
	                    new Object[]{bom.getValue(), product.getValue()}));  
	        } else if (!hasKeyMaterial) {  
	            // 警告：没有主物料（不阻断校验，仅记录）  
	            errors.add(product.getValue() + " BOM明细没有定义主物料");  
	        }  
	        if (invalid || containsinvalid)  
	            break;  
	    }  
	  
	    if (boms.isEmpty()) {  
	        invalid = true;  
	        errors.add(Msg.getMsg(ctx, "BOMMissingForProduct",  
	                new Object[]{product.getValue()}));  
	    } else if (MPPProductBOM.getDefault(product, trxName) == null) {  
	        invalid = true;  
	        errors.add(Msg.getMsg(ctx, "BOMNoDefaultBOMForProduct",  
	                new Object[]{product.getValue()}));  
	    }  
	  
	    checkedproducts.add(product);  
	    foundproducts.remove(product);  
	    if (invalid) {  
	        invalidproducts.add(product);  
	        product.setIsVerified(false);  
	        product.saveEx(trxName);  
	        return false;  
	    } else if (containsinvalid) {  
	        containinvalidproducts.add(product);  
	        product.setIsVerified(false);  
	        product.saveEx(trxName);  
	        return false;  
	    } else {  
	        validproducts.add(product);  
	        product.setIsVerified(true);  
	        product.saveEx(trxName);  
	        return true;  
	    }  
	}
	


	// ========== 原有方法保持不变 ==========
	// [所有原有的getLines, isValidFromTo, beforeSave, afterSave等方法保持不变]

} // MPPProductBOM