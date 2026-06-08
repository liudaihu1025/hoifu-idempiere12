package com.hoifu.validator;  
  
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MClient;
import org.compiere.model.ModelValidationEngine;
import org.compiere.model.ModelValidator;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Msg;  
  
/**
 * 对账单重复验证器 专门在新增对账单时检查 reconperoid, C_BPartner_ID, AD_Org_ID, IsSOTrx组合的唯一性
 * 同时监听对账明细表变化，更新表头汇总数据
 */
public class ReconciliationValidator implements ModelValidator {  
      
    /** 日志记录器 */  
    private static CLogger log = CLogger.getCLogger(ReconciliationValidator.class);  
      
    /** 客户端ID */  
    private int m_AD_Client_ID = -1;  
      
    /** 上下文 */  
    private Properties m_ctx = null;  
      
    @Override  
    public void initialize(ModelValidationEngine engine, MClient client) {  
        m_ctx = client != null ? client.getCtx() : null;  
        if (client != null) {  
            m_AD_Client_ID = client.getAD_Client_ID();  
        }  
          
        // 注册监听 C_Reconciliation 表的模型变更事件  
		engine.addModelChange("C_Reconciliation", this);
		// 注册监听 C_ReconciliationLine 表的模型变更事件
		engine.addModelChange("C_ReconciliationLine", this);
          
        if (log.isLoggable(Level.INFO)) {  
            log.info("ReconciliationValidator Initialized - AD_Client_ID=" + m_AD_Client_ID);  
        }  
    }  
      
    @Override  
    public String modelChange(PO po, int type) throws Exception {  
		if (po.get_TableName().equals("C_Reconciliation")) {
			if (type == ModelValidator.TYPE_BEFORE_NEW) {
				return validateReconciliation(po, 0); // 新增时 currentId=0
			} else if (type == ModelValidator.TYPE_BEFORE_CHANGE) {
				return validateReconciliation(po, po.get_ID()); // 修改时传入当前ID
            }  
		} else if (po.get_TableName().equals("C_ReconciliationLine")) {
			// 监听对账明细表的变化
			if (type == ModelValidator.TYPE_AFTER_NEW || type == ModelValidator.TYPE_AFTER_CHANGE
					|| type == ModelValidator.TYPE_AFTER_DELETE) {
				return updateReconciliationHeader(po);
			}
		}
        return null;  
    }  
      
    @Override  
    public String docValidate(PO po, int timing) {  
        // 文档验证逻辑（如果需要的话）  
        return null;  
    }  
      
    @Override  
    public int getAD_Client_ID() {  
        return m_AD_Client_ID;  
    }  
      
    @Override  
    public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {  
        // 登录时的初始化逻辑（如果需要的话）  
        return null;  
    }  
      
    /**
	 * 验证对账单重复性（新增/修改）
	 * 
	 * @param po        C_Reconciliation 记录
	 * @param currentId 当前记录ID（修改时>0，新增时为0）
	 * @return 错误信息，验证通过返回 null
	 */  
	private String validateReconciliation(PO po, int currentId) {
        try {  
            String reconperoid = (String) po.get_Value("reconperoid");  
            Integer cBPartnerId = (Integer) po.get_Value("C_BPartner_ID");  
            Integer adOrgId = (Integer) po.get_Value("AD_Org_ID");  
			Object isSOTrxObj = po.get_Value("IsSOTrx");

            String isSOTrx = null;  
            if (isSOTrxObj != null) {  
                if (isSOTrxObj instanceof Boolean) {  
                    isSOTrx = ((Boolean) isSOTrxObj) ? "Y" : "N";  
                } else {  
                    isSOTrx = isSOTrxObj.toString();  
                }  
            }  

            if (reconperoid == null || cBPartnerId == null || adOrgId == null) {  
				return null;
			}

			// 验证 ReconPeroid 格式 (YYYYMM)
			String formatError = validateReconPeroidFormat(reconperoid);
			if (formatError != null) {
				return formatError;
			}

			StringBuilder sql = new StringBuilder("SELECT C_Reconciliation_ID FROM C_Reconciliation "
					+ "WHERE reconperoid = ? AND C_BPartner_ID = ? AND AD_Org_ID = ? AND IsSOTrx = ? "
					+ "AND IsActive = 'Y'");
			List<Object> params = new ArrayList<>();
			params.add(reconperoid);
			params.add(cBPartnerId);
			params.add(adOrgId);
			params.add(isSOTrx);

			// 修改时排除自身
			if (currentId > 0) {
				sql.append(" AND C_Reconciliation_ID <> ?");
				params.add(currentId);
            }  

			int duplicateId = DB.getSQLValueEx(po.get_TrxName(), sql.toString(), params.toArray());

			if (duplicateId > 0) {
				String errorMsg = Msg.getMsg(m_ctx, "ReconciliationDuplicate",
						new Object[] { reconperoid, cBPartnerId, adOrgId, isSOTrx });
				if (errorMsg == null || errorMsg.equals("ReconciliationDuplicate")) {
                    String soTrxDesc = "Y".equals(isSOTrx) ? "客户" : "供应商";  
					errorMsg = "对账单编码重复：对账月(" + reconperoid + ")、业务伙伴(" + cBPartnerId + ")、组织(" + adOrgId + ")、销售事务("
							+ soTrxDesc + ")的组合已存在";
				}
                if (log.isLoggable(Level.WARNING)) {  
                    log.warning("Duplicate reconciliation detected: " + errorMsg);  
                }  
                return errorMsg;  
            }  
        } catch (Exception e) {  
            log.log(Level.SEVERE, "Error in validateReconciliation", e);  
            return "验证对账单时发生错误: " + e.getLocalizedMessage();  
        }  
		return null;
	}

	/**
	 * 验证 ReconPeroid 格式
	 */
	private String validateReconPeroidFormat(String reconperoid) {
		if (reconperoid == null || reconperoid.length() != 6) {
			return "对账月格式错误，应为YYYYMM（如202601）";
		}

		try {
			String monthPart = reconperoid.substring(4, 6);

			int month = Integer.parseInt(monthPart);

			if (month < 1 || month > 12) {
				return "对账月月份必须在01-12之间";
			}

		} catch (NumberFormatException e) {
			return "对账月格式错误，应为数字";
		}

		return null;
	}

	/**
	 * 更新对账单表头的汇总数据 当对账明细新增、修改、删除后调用
	 * 
	 * @param line C_ReconciliationLine 记录
	 * @return 错误信息，成功返回 null
	 */
	private String updateReconciliationHeader(PO line) {
		try {
			Integer cReconciliationId = (Integer) line.get_Value("C_Reconciliation_ID");
			if (cReconciliationId == null || cReconciliationId == 0) {
				return null; // 没有关联的对账单ID，跳过
			}

			// 更新总对账数量
			StringBuilder sqlQty = new StringBuilder("UPDATE C_Reconciliation r " + "SET TotalReconQty = ("
					+ "  SELECT COALESCE(SUM(rl.qtytoreconcile), 0) " + "  FROM C_ReconciliationLine rl "
					+ "  WHERE rl.C_Reconciliation_ID = r.C_Reconciliation_ID " + "  AND rl.IsActive = 'Y'" + ") "
					+ "WHERE r.C_Reconciliation_ID = ?");

			int noQty = DB.executeUpdate(sqlQty.toString(), cReconciliationId, line.get_TrxName());
			if (noQty != 1) {
				log.warning("Failed to update TotalReconQty for C_Reconciliation_ID=" + cReconciliationId
						+ ", rows affected: " + noQty);
			}

			// 更新总对账金额
			StringBuilder sqlAmt = new StringBuilder("UPDATE C_Reconciliation r " + "SET TotalReconAmt = ("
					+ "  SELECT COALESCE(SUM(rl.reconciledamt), 0) " + "  FROM C_ReconciliationLine rl "
					+ "  WHERE rl.C_Reconciliation_ID = r.C_Reconciliation_ID " + "  AND rl.IsActive = 'Y'" + ") "
					+ "WHERE r.C_Reconciliation_ID = ?");

			int noAmt = DB.executeUpdate(sqlAmt.toString(), cReconciliationId, line.get_TrxName());
			if (noAmt != 1) {
				log.warning("Failed to update TotalReconAmt for C_Reconciliation_ID=" + cReconciliationId
						+ ", rows affected: " + noAmt);
			}

			if (log.isLoggable(Level.FINE)) {
				log.fine("Updated reconciliation header totals for C_Reconciliation_ID=" + cReconciliationId);
			}

		} catch (Exception e) {
			log.log(Level.SEVERE, "Error updating reconciliation header", e);
			return "更新对账单汇总数据时发生错误: " + e.getLocalizedMessage();
		}

		return null;
	}
}