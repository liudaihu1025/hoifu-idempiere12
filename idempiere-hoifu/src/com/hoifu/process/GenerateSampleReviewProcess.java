package com.hoifu.process;

import java.sql.Timestamp;

import org.adempiere.base.annotation.Process;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MSequence;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
  
/**
 * 需求生成样品评审记录流程
 */
@Process
public class GenerateSampleReviewProcess extends SvrProcess {
  
	// ==================== 参数 ====================
	private int[] p_ReviewerIDs;
	private int p_FinalReviewer_ID;
  
	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if ("reviewers".equals(name)) {
				p_ReviewerIDs = para.getParameterAsIntArray();
			} else if ("finalreviewer_ID".equals(name)) {
				p_FinalReviewer_ID = para.getParameterAsInt();
			}
		}
	}
  
	@Override
	protected String doIt() throws Exception {
  
		// ==================== 1. 获取当前需求单 ID ====================
		int demandId = getRecord_ID();
		if (demandId <= 0)
			throw new AdempiereException("@NotFound@ @dy_samplingdemand_ID@");
  
		// ==================== 2. 参数校验 ====================
		if (p_ReviewerIDs == null || p_ReviewerIDs.length == 0)
			throw new AdempiereException("请至少选择一位评审人");
		if (p_FinalReviewer_ID <= 0)
			throw new AdempiereException("请选择终审人");
  
		// ==================== 3. 重复校验 ====================
		// 同一需求单已存在 isactive='Y' 的评审记录，禁止重复生成
		int existCount = DB.getSQLValue(get_TrxName(),
				"SELECT COUNT(1) FROM dy_samplereview " + "WHERE dy_samplingdemand_id=? AND isactive='Y'", demandId);
		if (existCount > 0)
			throw new AdempiereException("该需求单已存在样品评审记录，不允许重复生成");
  
		// ==================== 4. 获取需求单信息 ====================
		PO demand = MTable.get(getCtx(), "dy_samplingdemand").getPO(demandId, get_TrxName());
		if (demand == null || demand.get_ID() == 0)
			throw new AdempiereException("@NotFound@ @dy_samplingdemand_ID@");
  
		String demandDescription = demand.get_ValueAsString("description");
		int orgId = demand.get_ValueAsInt("ad_org_id");

		// ==================== 4.5. 校验需求单状态 ====================
		// 只有已完成（CO）的打样需求单才能提交样品评审
		String demandStatus = demand.get_ValueAsString("requeststatus");
		if (!"CO".equals(demandStatus))
			throw new AdempiereException("只有已完成的打样需求单才能提交样品评审");
  
		// ==================== 5. 生成单据号 ====================
		// MSequence.getDocumentNo 会自动查找或创建 DocumentNo_dy_samplereview 序列
		String documentNo = MSequence.getDocumentNo(getAD_Client_ID(), "dy_samplereview", get_TrxName());
		if (documentNo == null)
			throw new AdempiereException("无法生成单据号，请检查序列配置（DocumentNo_dy_samplereview）");
  
		// ==================== 6. 创建样品评审主表记录 ====================
		PO review = MTable.get(getCtx(), "dy_samplereview").getPO(0, get_TrxName());
		review.set_ValueNoCheck("ad_client_id", getAD_Client_ID());
		review.set_ValueNoCheck("ad_org_id", orgId);
		review.set_ValueNoCheck("documentno", documentNo);
		review.set_ValueNoCheck("name", demandDescription);
		review.set_ValueNoCheck("dy_samplingdemand_id", demandId);
		review.set_ValueNoCheck("ad_user_id", getAD_User_ID()); // 发起人 = 当前登录用户
		review.set_ValueNoCheck("finalreviewer_id", p_FinalReviewer_ID); // 终审人
		review.set_ValueNoCheck("initiateddate", new Timestamp(System.currentTimeMillis())); // 发起时间
		review.set_ValueNoCheck("reviewstatus", "RV"); // 评审中
		review.saveEx();
  
		int reviewId = review.get_ID();
  
		// ==================== 7. 为每个评审人创建评审明细记录 ====================
		int lineCount = 0;
		for (int reviewerId : p_ReviewerIDs) {
			PO line = MTable.get(getCtx(), "dy_samplereviewline").getPO(0, get_TrxName());
			line.set_ValueNoCheck("ad_client_id", getAD_Client_ID());
			line.set_ValueNoCheck("ad_org_id", orgId);
			line.set_ValueNoCheck("dy_samplereview_id", reviewId);
			line.set_ValueNoCheck("reviewer_id", reviewerId);
			line.set_ValueNoCheck("isfinalreview", "N");
			line.saveEx();
			lineCount++;
        }  
  
		// ==================== 8. 为终审人创建评审明细记录 ====================
		PO finalLine = MTable.get(getCtx(), "dy_samplereviewline").getPO(0, get_TrxName());
		finalLine.set_ValueNoCheck("ad_client_id", getAD_Client_ID());
		finalLine.set_ValueNoCheck("ad_org_id", orgId);
		finalLine.set_ValueNoCheck("dy_samplereview_id", reviewId);
		finalLine.set_ValueNoCheck("reviewer_id", p_FinalReviewer_ID);
		finalLine.set_ValueNoCheck("isfinalreview", "Y");
		finalLine.saveEx();
		lineCount++;

		return "生成成功，共生成 " + lineCount + " 条评审记录" + "（" + p_ReviewerIDs.length
				+ " 位评审人 + 1 位终审人）";
	}
}