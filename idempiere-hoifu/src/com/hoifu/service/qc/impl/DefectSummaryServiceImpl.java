package com.hoifu.service.qc.impl;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.hoifu.model.qc.MQC_IPQC;
import com.hoifu.model.qc.MQC_IQC;
import com.hoifu.model.qc.MQC_OQC;
import com.hoifu.model.qc.MQC_RQC;
import com.hoifu.service.qc.IDefectSummaryService;

public class DefectSummaryServiceImpl implements IDefectSummaryService {

	private static final CLogger log = CLogger.getCLogger(DefectSummaryServiceImpl.class);

	@Override
	public void aggregate(PO defectRecord) {
		String qcType = defectRecord.get_ValueAsString("QC_Type");
		int recordId = defectRecord.get_ValueAsInt("QC_Record_ID");
		String trxName = defectRecord.get_TrxName();

		if ("IQC".equals(qcType))
			updateIQC(recordId, trxName);
		else if ("IPQC".equals(qcType))
			updateIPQC(recordId, trxName);
		else if ("OQC".equals(qcType))
			updateOQC(recordId, trxName);
		else if ("RQC".equals(qcType))
			updateRQC(recordId, trxName);
	}

	private void updateIQC(int iqcId, String trxName) {
		String sql = buildSumSQL("IQC");
		try (PreparedStatement pstmt = DB.prepareStatement(sql, trxName)) {
			pstmt.setInt(1, iqcId);
			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				MQC_IQC iqc = new MQC_IQC(null, iqcId, trxName);
				iqc.setCR_Quantity(rs.getBigDecimal("cr"));
				iqc.setMAJ_Quantity(rs.getBigDecimal("maj"));
				iqc.setMIN_Quantity(rs.getBigDecimal("min"));
				iqc.saveEx();
			}
			rs.close();
		} catch (Exception e) {
			log.severe("汇总 IQC 缺陷失败: " + e.getMessage());
		}
	}

	private void updateIPQC(int ipqcId, String trxName) {
		String sql = buildSumSQL("IPQC");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, ipqcId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				MQC_IPQC ipqc = new MQC_IPQC(null, ipqcId, trxName);
				ipqc.setCR_Quantity(BigDecimal.valueOf(rs.getInt("cr")));
				ipqc.setMAJ_Quantity(BigDecimal.valueOf(rs.getInt("maj")));
				ipqc.setMIN_Quantity(BigDecimal.valueOf(rs.getInt("min")));
				ipqc.saveEx();
			}
		} catch (Exception e) {
			log.severe("汇总 IPQC 缺陷失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
	}

	private void updateOQC(int oqcId, String trxName) {
		String sql = buildSumSQL("OQC");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, oqcId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				MQC_OQC oqc = new MQC_OQC(null, oqcId, trxName);
				oqc.setCR_Quantity(BigDecimal.valueOf(rs.getInt("cr")));
				oqc.setMAJ_Quantity(BigDecimal.valueOf(rs.getInt("maj")));
				oqc.setMIN_Quantity(BigDecimal.valueOf(rs.getInt("min")));
				oqc.saveEx();
			}
		} catch (Exception e) {
			log.severe("汇总 OQC 缺陷失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
	}

	private void updateRQC(int rqcId, String trxName) {
		String sql = buildSumSQL("RQC");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, rqcId);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				MQC_RQC rqc = new MQC_RQC(null, rqcId, trxName);
				rqc.setCR_Quantity(rs.getBigDecimal("cr"));
				rqc.setMAJ_Quantity(rs.getBigDecimal("maj"));
				rqc.setMIN_Quantity(rs.getBigDecimal("min"));
				rqc.saveEx();
			}
		} catch (Exception e) {
			log.severe("汇总 RQC 缺陷失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
	}

	private String buildSumSQL(String qcType) {
		return "SELECT " + "COALESCE(SUM(CASE WHEN DefectLevel='CR'  THEN DefectQuantity ELSE 0 END),0) as cr,"
				+ "COALESCE(SUM(CASE WHEN DefectLevel='MAJ' THEN DefectQuantity ELSE 0 END),0) as maj,"
				+ "COALESCE(SUM(CASE WHEN DefectLevel='MIN' THEN DefectQuantity ELSE 0 END),0) as min "
				+ "FROM QC_DefectRecord WHERE QC_Type='" + qcType + "' AND QC_Record_ID=?";
	}
}