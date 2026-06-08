package com.hoifu.process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MBPartner;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProcessPara;
import org.compiere.model.MSequence;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.model.MReconciliation;

@org.adempiere.base.annotation.Process
public class GenerateReconciliationProcess extends SvrProcess {
	
	public static final int DOCTYPE_CUSTOMER_RETURN = 1000720;  //客户退货类型
	public static final int DOCTYPE_VENDOR_RETURN = 1000718;    //供应商退货类型
    
    private int p_C_BPartner_ID = 0;
    private String p_ReconPeroid = null;
    private int p_AD_Org_ID = 0;
    private int reconciliationId = 0; // 当前对账单ID
    
    @Override
    protected void prepare() {
        // 保留原来的参数解析逻辑以便向后兼容
        ProcessInfoParameter[] para = getParameter();
        for (int i = 0; i < para.length; i++) {
            String name = para[i].getParameterName();
            if (name.equals("C_BPartner_ID"))
                p_C_BPartner_ID = para[i].getParameterAsInt();
            else if (name.equals("ReconPeroid"))
                p_ReconPeroid = (String) para[i].getParameter();
            else if (name.equals("AD_Org_ID"))
                p_AD_Org_ID = para[i].getParameterAsInt();
            else
                MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
        }
    }
    
    @Override
    protected String doIt() throws Exception {
        // 1. 获取当前对账单ID
        reconciliationId = getRecord_ID();
        
        if (reconciliationId <= 0) {
            throw new AdempiereException("未找到有效的对账单记录");
        }
        
        // 2. 从数据库加载对账单参数
        if (!loadReconciliationParameters(reconciliationId)) {
            throw new AdempiereException("加载对账单参数失败，ID=" + reconciliationId);
        }
        
        // 3. 验证参数
        if (p_ReconPeroid == null || p_ReconPeroid.length() != 6)
            throw new IllegalArgumentException("@FillMandatory@ @ReconPeroid@");
        
        log.info("生成对账单明细 - 对账单ID=" + reconciliationId + 
                ", 业务伙伴ID=" + (p_C_BPartner_ID > 0 ? p_C_BPartner_ID : "全部") + 
                ", 期间=" + p_ReconPeroid + ", 组织ID=" + p_AD_Org_ID);
        
        // 4. 验证对账单状态 - 只在完成状态时禁止
        String status = validateReconciliationStatus(reconciliationId);
        if ("CO".equals(status) || "CL".equals(status)) { // 完成或关闭状态不允许重新生成
            throw new AdempiereException("当前对账单状态为'" + status + "'，不允许重新生成明细");
        }
        
        // 5. 禁用RecentItems
        String originalDisableRecentItems = Env.getContext(getCtx(), "#DisableRecentItems");
        Env.setContext(getCtx(), "#DisableRecentItems", "Y");
        
        try {
        	int totalReconciliations = 0; // 实际处理的对账单总数  
            int totalLinesCreated = 0;    
            StringBuilder resultMsg = new StringBuilder();  
            List<String> errorMessages = new ArrayList<>();  
            List<String> processMessages = new ArrayList<>();  
          
            if (p_C_BPartner_ID > 0) {  
                ProcessResult result = processReconciliationById(reconciliationId, p_ReconPeroid);  
                if (result.isSuccess()) {  
                    totalReconciliations += result.getProcessedReconciliations(); // 累加实际处理数量  
                    totalLinesCreated = result.getLinesCreated();  

                    // 如果有组合消息（两条对账单），则使用；否则使用单条消息  
                    if (result.getCombinedMessage() != null && !result.getCombinedMessage().isEmpty()) {  
                        processMessages.add(result.getCombinedMessage());  
                    } else {  
                        processMessages.add((result.isSOTrx() ? "客户" : "供应商") + "对账单：" + result.getDocumentNo() + ": " + result.getProcessMessage());  
                    }  
                } else {  
                    errorMessages.add(result.getErrorMessage());  
                }  
            } else {  
                throw new AdempiereException("对账单的业务伙伴ID为空");  
            }  
          
            // 构建结果消息，只出现一次“处理完成”  
            resultMsg.append("成功处理 ").append(totalReconciliations).append("个对账单 (").append(totalLinesCreated).append(" 行明细)");  
            // 添加详细处理信息  
            if (!processMessages.isEmpty()) {  
                resultMsg.append("\n\n处理详情：");  
                for (String msg : processMessages) {  
                    addLog(0, null, null, msg);  
                }  
            }  
            if (!errorMessages.isEmpty()) {  
                resultMsg.append("\n\n错误详情：");  
                for (String msg : errorMessages) {  
                    addLog(0, null, null, msg);  
                }  
            }  
            return resultMsg.toString();  
            
        } finally {
            // 恢复RecentItems设置
            if (originalDisableRecentItems != null) {
                Env.setContext(getCtx(), "#DisableRecentItems", originalDisableRecentItems);
            } else {
                Env.setContext(getCtx(), "#DisableRecentItems", "");
            }
        }
    }
    
    /**
     * 从数据库中加载对账单参数
     */
    private boolean loadReconciliationParameters(int reconId) {
        String sql = "SELECT C_BPartner_ID, ReconPeroid, AD_Org_ID, DocStatus, Processed FROM C_Reconciliation " +
                    "WHERE C_Reconciliation_ID=?";
        
        try {
            PreparedStatement pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, reconId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                p_C_BPartner_ID = rs.getInt("C_BPartner_ID");
                p_ReconPeroid = rs.getString("ReconPeroid");
                p_AD_Org_ID = rs.getInt("AD_Org_ID");
                String docStatus = rs.getString("DocStatus");
                String processed = rs.getString("Processed");
                
                if (rs.wasNull()) {
                    p_C_BPartner_ID = 0;
                }
                if (rs.wasNull()) {
                    p_ReconPeroid = null;
                }
                if (rs.wasNull()) {
                    p_AD_Org_ID = 0;
                }
                
                log.info("从对账单加载参数: ID=" + reconId + 
                        ", 业务伙伴ID=" + p_C_BPartner_ID + 
                        ", 期间=" + p_ReconPeroid + 
                        ", 组织ID=" + p_AD_Org_ID +
                        ", 状态=" + docStatus +
                        ", 已处理=" + processed);
                
                DB.close(rs, pstmt);
                return true;
            } else {
                DB.close(rs, pstmt);
                log.severe("未找到对账单，ID=" + reconId);
                return false;
            }
            
        } catch (SQLException e) {
            log.log(Level.SEVERE, "加载对账单参数失败", e);
            return false;
        }
    }
    
    /**
     * 验证对账单状态
     */
    private String validateReconciliationStatus(int reconId) {
        String sql = "SELECT DocStatus FROM C_Reconciliation WHERE C_Reconciliation_ID=?";
        String docStatus = DB.getSQLValueStringEx(get_TrxName(), sql, reconId);
        
        if (docStatus == null) {
            throw new AdempiereException("对账单不存在，ID=" + reconId);
        }
        
        return docStatus;
    }
    
    /**
     * 处理对账单明细生成（针对已存在的对账单）
     */
    private ProcessResult processReconciliationById(int reconId, String reconMonthFormatted) {
        ProcessResult result = new ProcessResult();
        int clientId = getAD_Client_ID();
        int currentUserId = getAD_User_ID();
        int orgId = (p_AD_Org_ID > 0) ? p_AD_Org_ID : getAD_Org_ID();
        
        if (orgId <= 0) {
            result.setErrorMessage("组织字段为必填项");
            return result;
        }
        
        // 获取业务伙伴信息  
        MBPartner bpartner = new MBPartner(getCtx(), p_C_BPartner_ID, get_TrxName());  
        boolean isCustomer = bpartner.isCustomer();  
        boolean isVendor = bpartner.isVendor();  
          
        // 检查两种类型的收发明细是否存在  
        boolean hasCustomerInOut = hasInOutLines(p_C_BPartner_ID, reconMonthFormatted, true, orgId, clientId);  
        boolean hasVendorInOut = hasInOutLines(p_C_BPartner_ID, reconMonthFormatted, false, orgId, clientId);  
          
        // 处理逻辑  
        if (isCustomer && isVendor && hasCustomerInOut && hasVendorInOut) {  
            // 两种类型都存在，需要创建两个对账单  
            result = processBothTypes(reconId, reconMonthFormatted, orgId, clientId, currentUserId);  
        } else {  
            // 只有一种类型或只有一种数据，在当前对账单处理  
            boolean isSOTrx = (isCustomer && hasCustomerInOut) || (!isVendor && hasCustomerInOut);  
            updateReconciliationIsSOTrx(reconId, isSOTrx, clientId);  
            result = processSingleType(reconId, reconMonthFormatted, isSOTrx, orgId, clientId, currentUserId);  
        }  
          
        return result;  
    }
    
    /**  
     * 检查是否存在指定类型的收发明细  
     */  
    private boolean hasInOutLines(int bpartnerId, String reconPeriod, boolean isSOTrx, int orgId, int clientId) {  
        String sql = "SELECT COUNT(*) FROM M_InOutLine iol " +  
            "INNER JOIN M_InOut io ON (iol.M_InOut_ID=io.M_InOut_ID) " +  
            "WHERE io.C_BPartner_ID=? AND iol.reconciliationmonth=? " +  
            "AND io.DocStatus IN ('CO','CL') " +  
            "AND io.AD_Client_ID=? AND io.AD_Org_ID=? " +  
            "AND (io.IsSOTrx = ? OR io.C_DocType_ID IN (?))";  
          
        int docTypeId = isSOTrx ? DOCTYPE_CUSTOMER_RETURN : DOCTYPE_VENDOR_RETURN; // 客户退货或供应商退货  
        int count = DB.getSQLValueEx(get_TrxName(), sql,   
            bpartnerId, reconPeriod, clientId, orgId,   
            isSOTrx ? "Y" : "N", docTypeId);  
          
        return count > 0;  
    }  
      
    /**  
     * 处理两种类型的情况  
     */  
    private ProcessResult processBothTypes(int originalReconId, String reconPeriod, int orgId, int clientId, int userId) {  
    	
        List<String> messages = new ArrayList<>();  
        ProcessResult customerResult = new ProcessResult();  
        ProcessResult vendorResult = new ProcessResult();  
        
        String originalDocNo = "";
        String oppositeDocNo = "";  
        
        try {  
            // 获取当前对账单的销售事务  
            MReconciliation original = new MReconciliation(getCtx(), originalReconId, get_TrxName());  
            
            originalDocNo = original.getDocumentNo();
            
            boolean currentIsSOTrx = original.isSOTrx();  
            boolean oppositeIsSOTrx = !currentIsSOTrx;  
              
            // 检查相反销售事务是否有数据  
            boolean hasOppositeData = hasInOutLines(original.getC_BPartner_ID(), reconPeriod, oppositeIsSOTrx, orgId, clientId);  
            
            // 只有相反方向有数据时才创建新对账单  
            int customerReconId = 0;  
            if (hasOppositeData) {  
                // 创建相反销售事务的对账单  
                customerReconId = createNewReconciliation(originalReconId, oppositeIsSOTrx);  
                if (customerReconId > 0) {  
                    ProcessResult oppositeResult = processSingleType(customerReconId, reconPeriod, oppositeIsSOTrx, orgId, clientId, userId);  
                    // 获取新对账单的单号  
                    MReconciliation opposite = new MReconciliation(getCtx(), customerReconId, get_TrxName());  
                    oppositeDocNo = opposite.getDocumentNo();  
                    messages.add((oppositeIsSOTrx ? "客户" : "供应商") + "对账单:" + oppositeDocNo + ": " + oppositeResult.getProcessMessage());
                    customerResult = oppositeResult;  
                } else {  
                    messages.add("创建" + (oppositeIsSOTrx ? "客户" : "供应商") + "对账单失败");  
                }  
            } else {  
                messages.add("无" + (oppositeIsSOTrx ? "客户" : "供应商") + "数据，跳过创建对账单");  
            }  
              
            // 当前对账单处理原有销售事务  
            vendorResult = processSingleType(originalReconId, reconPeriod, currentIsSOTrx, orgId, clientId, userId);  
            messages.add((currentIsSOTrx ? "客户" : "供应商") + "对账单:" + originalDocNo + ": " + vendorResult.getProcessMessage());  
              
            // 返回综合结果  
            ProcessResult combinedResult = new ProcessResult();  
            combinedResult.setSuccess(customerResult.isSuccess() || vendorResult.isSuccess());  
            combinedResult.setLinesCreated(customerResult.getLinesCreated() + vendorResult.getLinesCreated());  
            combinedResult.setLinesTotal(customerResult.getLinesTotal() + vendorResult.getLinesTotal());  
            combinedResult.setProcessedReconciliations(hasOppositeData ? 2 : 1); // 实际处理的对账单数量  
            combinedResult.setCombinedMessage(String.join("; ", messages));
              
            return combinedResult;  
              
        } catch (Exception e) {  
            log.severe("处理对账单异常: " + e.getMessage());  
            ProcessResult errorResult = new ProcessResult();  
            errorResult.setSuccess(false);  
            errorResult.setErrorMessage("处理对账单异常: " + e.getMessage());  
            return errorResult;  
        }  
    }  
    
    /**  
     * 处理单一类型  
     */  
    private ProcessResult processSingleType(int reconId, String reconPeriod, boolean isSOTrx, int orgId, int clientId, int userId) {  
    	ProcessResult result = new ProcessResult();  
        int lineCount = 0;  
        int successCount = 0;  
          
        try {  
            // 1. 删除现有明细行  
        	deleteExistingLines(reconId, clientId);  
              
            // 2. 查询对应类型的收发明细  
            String sql = "SELECT iol.* FROM M_InOutLine iol " +  
                "INNER JOIN M_InOut io ON (iol.M_InOut_ID=io.M_InOut_ID) " +  
                "WHERE io.C_BPartner_ID=? AND iol.reconciliationmonth=? " +  
                "AND io.DocStatus IN ('CO','CL') " +  
                "AND io.AD_Client_ID=? AND iol.AD_Client_ID=? " +  
                "AND io.AD_Org_ID=? AND iol.AD_Org_ID=? " +  
                "AND (" +  
                "  (io.IsSOTrx = ?) " +  // 主方向：Y=发货，N=收货  
                "  OR (io.C_DocType_ID = ?) " +  // 允许的退货单类型  
                ") " +  
                "ORDER BY io.MovementDate, iol.Line";  
              
            int docTypeId = isSOTrx ? DOCTYPE_CUSTOMER_RETURN : DOCTYPE_VENDOR_RETURN; // 客户退货或供应商退货  
              
            PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName());  
            pstmt.setInt(1, p_C_BPartner_ID);  
            pstmt.setString(2, reconPeriod);  
            pstmt.setInt(3, clientId);  
            pstmt.setInt(4, clientId);  
            pstmt.setInt(5, orgId);  
            pstmt.setInt(6, orgId);  
            pstmt.setString(7, isSOTrx ? "Y" : "N"); // 主方向  
            pstmt.setInt(8, docTypeId); // 退货单类型  
              
            ResultSet rs = pstmt.executeQuery();  
              
            // 3. 创建对账明细行  
            while (rs.next()) {  
                MInOutLine iol = new MInOutLine(getCtx(), rs.getInt("M_InOutLine_ID"), get_TrxName());  
                boolean success = createReconciliationLineDirectly(reconId, iol, clientId, userId, orgId);  
                lineCount++;  
                if (success) {  
                    successCount++;  
                }  
            }  
            DB.close(rs, pstmt);  
              
            // 4. 计算表头汇总  
            if (lineCount > 0) {  
                calculateHeaderTotalsDirectly(reconId, p_C_BPartner_ID, clientId);  
                // 更新对账单状态为处理中  
				String updateSql = "UPDATE C_Reconciliation SET DocStatus='DR' WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
                DB.executeUpdate(updateSql, new Object[]{reconId, clientId}, false, get_TrxName());  
                  
                result.setSuccess(true);  
                result.setLinesCreated(successCount);  
                result.setLinesTotal(lineCount);  
                result.setProcessMessage("成功创建对账明细: " + successCount + "/" + lineCount + " 行");  
            } else {  
                // 无明细行，更新状态为草稿  
                String updateSql = "UPDATE C_Reconciliation SET DocStatus='DR' WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";  
                DB.executeUpdate(updateSql, new Object[]{reconId, clientId}, false, get_TrxName());  
                  
                result.setSuccess(true);  
                result.setLinesCreated(0);  
                result.setLinesTotal(0);  
                result.setProcessMessage("无符合条件的收发明细数据");  
            }  
              
        } catch (Exception e) {  
            result.setSuccess(false);  
            result.setErrorMessage("处理对账明细失败: " + e.getMessage());  
            log.severe("processSingleType 失败: " + e.getMessage());  
        }  
        
        // 在返回前设置单号和销售事务
        MReconciliation recon = new MReconciliation(getCtx(), reconId, get_TrxName());  
        result.setDocumentNo(recon.getDocumentNo()); 
        result.setSOTrx(isSOTrx);
        
        return result;  
    }  
      
    /**  
     * 更新对账单的IsSOTrx字段  
     */  
    private void updateReconciliationIsSOTrx(int reconId, boolean isSOTrx, int clientId) {
    	
    	// 获取当前对账单信息  
        MReconciliation current = new MReconciliation(getCtx(), reconId, get_TrxName());  
        if (current.get_ID() == 0) 
        	return ;  
          
        // 检查是否已存在目标IsSOTrx的对账单  
        String checkSql = "SELECT C_Reconciliation_id, DocumentNo FROM C_Reconciliation " +  
            "WHERE C_BPartner_ID=? AND ReconPeroid=? AND AD_Org_ID=? AND IsSOTrx=? " +  
            "AND AD_Client_ID=?";  
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        try {  
            pstmt = DB.prepareStatement(checkSql, get_TrxName());  
            pstmt.setInt(1, current.getC_BPartner_ID());  
            pstmt.setString(2, current.getReconPeroid());  
            pstmt.setInt(3, current.getAD_Org_ID());  
            pstmt.setString(4, isSOTrx ? "Y" : "N");  
            pstmt.setInt(5, getAD_Client_ID());  
            rs = pstmt.executeQuery();  
              
            if (rs.next()) {  
            	if(rs.getInt(1) != reconId) {
                	String existingDocNo = rs.getString(2);  
                    // 已存在目标记录，抛出异常提示用户  
                    String msg = "已存在" + (isSOTrx ? "客户" : "供应商") + "对账单单号：" + existingDocNo;
                    throw new AdempiereException(msg); 
            	}
            }  
        } catch (SQLException e) {  
            log.severe("检查已存在对账单时出错: " + e.getMessage());  
        } finally {  
            DB.close(rs, pstmt);  
        }    
        
        // 不存在，执行更新
        String sql = "UPDATE C_Reconciliation SET IsSOTrx=? WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";  
        DB.executeUpdate(sql, new Object[]{isSOTrx ? "Y" : "N", reconId, clientId}, false, get_TrxName());  
    }  
      
    /**  
     * 创建新的对账单  
     */  
    private int createNewReconciliation(int originalReconId, boolean isSOTrx) {  
        try {  
            // 加载原始对账单获取基本信息  
            MReconciliation original = new MReconciliation(getCtx(), originalReconId, get_TrxName());  
            if (original.get_ID() == 0) {  
                log.severe("未找到原始对账单，ID=" + originalReconId);  
                return 0;  
            }  
              
            int clientId = getAD_Client_ID();  
            int userId = getAD_User_ID();  
            int orgId = original.getAD_Org_ID();  
            int bpartnerId = original.getC_BPartner_ID();  
            String reconPeroid = original.getReconPeroid();  
            
            // 检查是否已存在相同条件的对账单  
            String checkSql = "SELECT C_Reconciliation_ID FROM C_Reconciliation " +  
                "WHERE C_BPartner_ID=? AND ReconPeroid=? AND AD_Org_ID=? AND IsSOTrx=? " +  
                "AND AD_Client_ID=?";  
            PreparedStatement checkStmt = DB.prepareStatement(checkSql, get_TrxName());  
            checkStmt.setInt(1, bpartnerId);  
            checkStmt.setString(2, reconPeroid);  
            checkStmt.setInt(3, orgId);  
            checkStmt.setString(4, isSOTrx ? "Y" : "N");  
            checkStmt.setInt(5, clientId);  
              
            ResultSet rs = checkStmt.executeQuery();  
            if (rs.next()) {  
                int existingId = rs.getInt(1);  
                log.info("已存在相同条件的对账单，直接使用: ID=" + existingId +   
                    ", 业务伙伴=" + bpartnerId +   
                    ", 期间=" + reconPeroid +   
                    ", IsSOTrx=" + (isSOTrx ? "Y" : "N"));  
                DB.close(rs, checkStmt);  
                return existingId;  
            }  
            DB.close(rs, checkStmt);  
              
            // 生成新ID和单据号  
            int newReconId = DB.getNextID(clientId, "C_Reconciliation", get_TrxName());  
            String documentNo = MSequence.getDocumentNo(clientId, "C_Reconciliation", get_TrxName());  
              
            // 从原对账单获取日期字段  
            Timestamp reconciliationDay = original.getReconciliationDay();  
            Timestamp reconciliationCutoff = original.getReconciliationcutoff();  
            Timestamp reconciliationDate = original.getReconciliationDate();   
            
            //从原对账单获取伙伴地址和用户/联系人
            int bpartnerLocationId = original.getC_BPartner_Location_ID();  
            int userid = original.getAD_User_ID();
              
            // 获取货币ID  
            int currencyId = Env.getContextAsInt(getCtx(), "$C_Currency_ID");  
              
            // 使用简化的插入语句  
            String insertSql = "INSERT INTO C_Reconciliation (" +   
                "C_Reconciliation_ID, AD_Client_ID, AD_Org_ID, IsActive, " +  
                "Created, CreatedBy, Updated, UpdatedBy, " +  
                "DocumentNo, C_BPartner_ID, IsSOTrx, " +  
                "ReconciliationDay, ReconciliationCutoff, ReconciliationDate, " +  
                "BeginningBalance, TotalInvoiceAmt, TotalPaymentAmt, " +  
                "TotalAdjustmentAmt, EndingBalance, C_Currency_ID, " +  
                "DocStatus, DocAction, Processed, Processing, " +  
                "IsApproved, IsConfirmed, ReconPeroid, C_BPartner_Location_ID, AD_User_ID) " +  
                "VALUES (?, ?, ?, 'Y', " +  
                "NOW(), ?, NOW(), ?, " +  
                "?, ?, ?, " +  
                "?, ?, ?, " +  
                "0, 0, 0, " +  
                "0, 0, ?, " +  
                "'DR', 'CO', 'N', 'N', " +  
                "'N', 'N', ?, ?, ?)";  
              
            PreparedStatement pstmt = DB.prepareStatement(insertSql, get_TrxName());  
            int paramIndex = 1;  
              
            // 设置参数  
            pstmt.setInt(paramIndex++, newReconId);  
            pstmt.setInt(paramIndex++, clientId);  
            pstmt.setInt(paramIndex++, orgId);  
            pstmt.setInt(paramIndex++, userId);  
            pstmt.setInt(paramIndex++, userId);  
            pstmt.setString(paramIndex++, documentNo);  
            pstmt.setInt(paramIndex++, bpartnerId);  
            pstmt.setString(paramIndex++, isSOTrx ? "Y" : "N");  
            pstmt.setTimestamp(paramIndex++, reconciliationDay);  
            pstmt.setTimestamp(paramIndex++, reconciliationCutoff);  
            pstmt.setTimestamp(paramIndex++, reconciliationDate);  
            pstmt.setInt(paramIndex++, currencyId);  
            pstmt.setString(paramIndex++, reconPeroid);  
            pstmt.setInt(paramIndex++, bpartnerLocationId);
            pstmt.setInt(paramIndex++, userid);
              
            int result = pstmt.executeUpdate();  
            DB.close(pstmt);  
              
            if (result > 0) {  
                log.info("成功创建对账单副本: ID=" + newReconId +   
                    ", 原单据ID=" + originalReconId +   
                    ", 单据号=" + documentNo +  
                    ", IsSOTrx=" + (isSOTrx ? "Y" : "N"));  
                return newReconId;  
            } else {  
                log.severe("创建对账单副本失败");  
                return 0;  
            }  
              
        } catch (Exception e) {  
            log.severe("创建对账单副本异常: " + e.getMessage());  
            e.printStackTrace();  
            return 0;  
        }  
    }
    
    /**
     * 处理结果类
     */
    private static class ProcessResult {
        private boolean success = false;
        private int linesCreated = 0;
        private int linesTotal = 0;
        private String errorMessage = null;
        private String processMessage = "";
        private int processedReconciliations = 1; // 默认1  
        private String documentNo = ""; // 单号（单条时使用）  
        private String combinedMessage = ""; // 多条时组合消息  
        private boolean isSOTrx = false;  
        
        public boolean isSOTrx() { return isSOTrx; }  
        public void setSOTrx(boolean isSOTrx) { this.isSOTrx = isSOTrx; }  
        
        public int getProcessedReconciliations() { return processedReconciliations; }  
        public void setProcessedReconciliations(int processedReconciliations) { this.processedReconciliations = processedReconciliations; }  
        
        public String getDocumentNo() { return documentNo; }  
        public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }  
        
        public String getCombinedMessage() { return combinedMessage; }  
        public void setCombinedMessage(String combinedMessage) { this.combinedMessage = combinedMessage; }  
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public int getLinesCreated() { return linesCreated; }
        public void setLinesCreated(int linesCreated) { this.linesCreated = linesCreated; }
        
        public int getLinesTotal() { return linesTotal; }
        public void setLinesTotal(int linesTotal) { this.linesTotal = linesTotal; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public String getProcessMessage() { return processMessage; }
        public void setProcessMessage(String processMessage) { this.processMessage = processMessage; }
    }
            
    /**
     * 删除现有明细行
     */
    private int deleteExistingLines(int reconId, int clientId) {
        String deleteSql = "DELETE FROM C_ReconciliationLine WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
        int deleted = DB.executeUpdate(deleteSql, new Object[]{reconId, clientId}, false, get_TrxName());
        if (deleted > 0) {
            log.info("删除 " + deleted + " 条旧明细行, 对账单ID=" + reconId);
        }
        return deleted;
    }
    
    /**
     * 创建对账明细行
     */
    private boolean createReconciliationLineDirectly(int reconId, MInOutLine iol, int clientId, int currentUserId, int orgId) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            // 获取基础数据
            BigDecimal movementQty = iol.getMovementQty();
            int productId = iol.getM_Product_ID();
            int uomId = iol.getC_UOM_ID();

            // 初始化字段
            BigDecimal priceActual = BigDecimal.ZERO;
            BigDecimal priceEntered = BigDecimal.ZERO;
            BigDecimal priceList = BigDecimal.ZERO;
            BigDecimal priceLimit = BigDecimal.ZERO;
            BigDecimal discount = BigDecimal.ZERO;
            BigDecimal qtyOrdered = BigDecimal.ZERO;
            BigDecimal qtyDelivered = BigDecimal.ZERO;
            BigDecimal qtyInvoiced = BigDecimal.ZERO;
            BigDecimal taxRate = BigDecimal.ZERO;
            boolean isTaxIncluded = false;
            int currencyId = Env.getContextAsInt(getCtx(), "$C_Currency_ID");
            Integer taxId = null;
            Integer orderLineId = null;
            Integer invoiceLineId = null;

            // 获取C_DocType_ID作为LineType
            Integer lineType = iol.getParent().getC_DocType_ID();
            
            // 判断是否为退货单（供应商退货单或客户退货单）
            boolean isReturnType = (lineType != null && (lineType == DOCTYPE_VENDOR_RETURN || lineType == DOCTYPE_CUSTOMER_RETURN));
            
            log.fine("处理收发货单行: M_InOutLine_ID=" + iol.get_ID() + 
                    ", 单据类型=" + lineType + 
                    ", 是否退货单=" + isReturnType + 
                    ", 收发货单单号=" + iol.getParent().getDocumentNo());
            
            // 如果是退货单，从M_RMALine获取数据
            if (isReturnType) {
                log.fine("开始处理退货单: M_InOutLine_ID=" + iol.get_ID() + 
                        ", 类型=" + lineType + 
                        ", 原始数量=" + iol.getMovementQty());
                
                // 退货单的移动数量为负数
                movementQty = (movementQty != null && movementQty.signum() > 0) 
                              ? movementQty.negate() 
                              : movementQty;
                
                // 设置默认数量
                qtyOrdered = movementQty.abs();  // 取绝对值
                qtyDelivered = qtyOrdered;
                qtyInvoiced = qtyOrdered;
                
                // 检查是否有退货授权明细ID
                int rmaLineId = iol.getM_RMALine_ID();
                if (rmaLineId > 0) {
                    log.fine("退货单有M_RMALine_ID: " + rmaLineId);
                    
                    // 从M_RMALine表获取数据
                    String rmaSql = "SELECT rl.Qty, rl.QtyDelivered, rl.QtyInvoiced, rl.Amt, rl.LineNetAmt, " +
                                   "rl.C_Tax_ID, r.C_Currency_ID, rl.M_InOutLine_ID " +
                                   "FROM M_RMALine rl " +
                                   "INNER JOIN M_RMA r ON rl.M_RMA_ID = r.M_RMA_ID " +
                                   "WHERE rl.M_RMALine_ID = ? AND rl.IsActive='Y' AND r.DocStatus IN ('CO','CL')";
                    
                    pstmt = DB.prepareStatement(rmaSql, get_TrxName());
                    pstmt.setInt(1, rmaLineId);
                    rs = pstmt.executeQuery();
                    
                    if (rs.next()) {
                        // 获取数量
                        qtyOrdered = getBigDecimal(rs, "Qty");
                        qtyDelivered = getBigDecimal(rs, "QtyDelivered");
                        qtyInvoiced = getBigDecimal(rs, "QtyInvoiced");
                        
                        // 获取金额
                        BigDecimal rmaAmt = getBigDecimal(rs, "Amt");
                        
                        // 获取税率、货币
                        taxId = rs.getInt("C_Tax_ID");
                        if (rs.wasNull()) {
                            taxId = null;
                        }
                        
                        currencyId = rs.getInt("C_Currency_ID");
                        if (rs.wasNull()) {
                            currencyId = Env.getContextAsInt(getCtx(), "$C_Currency_ID");
                        }
                        
                        // 获取原始收发货单行ID
                        int originalInOutLineId = rs.getInt("M_InOutLine_ID");
                        
                        log.fine("从M_RMALine获取数据: 数量=" + qtyOrdered + 
                                ", 金额=" + rmaAmt + 
                                ", 原始M_InOutLine_ID=" + originalInOutLineId);
                        
                        // 关键步骤：通过原始收发货单行获取订单行ID，然后从订单明细获取价格
                        if (originalInOutLineId > 0) {
                            // 通过原始收发货单行获取订单行ID
                            String orderLineSql = "SELECT C_OrderLine_ID FROM M_InOutLine " +
                                                 "WHERE M_InOutLine_ID = ? AND AD_Client_ID = ?";
                            PreparedStatement pstmt2 = null;
                            ResultSet rs2 = null;
                            try {
                                pstmt2 = DB.prepareStatement(orderLineSql, get_TrxName());
                                pstmt2.setInt(1, originalInOutLineId);
                                pstmt2.setInt(2, clientId);
                                rs2 = pstmt2.executeQuery();
                                
                                if (rs2.next()) {
                                    orderLineId = rs2.getInt("C_OrderLine_ID");
                                    if (!rs2.wasNull() && orderLineId > 0) {
                                        log.fine("找到原始收发货单行的订单行ID: " + orderLineId);
                                        
                                        // 从订单明细获取价格信息
                                        String orderSql = "SELECT " +
                                            "ol.PriceActual, ol.PriceEntered, ol.PriceList, ol.PriceLimit, " +
                                            "ol.Discount, ol.QtyOrdered, ol.QtyDelivered, ol.QtyInvoiced, " +
                                            "ol.C_Tax_ID, COALESCE(o.IsTaxIncluded, 'N') as IsTaxIncluded, " +
                                            "COALESCE(o.C_Currency_ID, ?) as C_Currency_ID, " +
                                            "COALESCE(t.Rate, 0) as Rate " +
                                            "FROM C_OrderLine ol " +
                                            "INNER JOIN C_Order o ON ol.C_Order_ID = o.C_Order_ID " +
                                            "LEFT JOIN C_Tax t ON ol.C_Tax_ID = t.C_Tax_ID " +
                                            "WHERE ol.C_OrderLine_ID = ? " +
                                            "AND ol.IsActive = 'Y' " +
                                            "AND ol.AD_Client_ID=? " +
                                            "AND o.AD_Client_ID=?";
                                        
                                        PreparedStatement pstmt3 = null;
                                        ResultSet rs3 = null;
                                        try {
                                            pstmt3 = DB.prepareStatement(orderSql, get_TrxName());
                                            pstmt3.setInt(1, currencyId);
                                            pstmt3.setInt(2, orderLineId);
                                            pstmt3.setInt(3, clientId);
                                            pstmt3.setInt(4, clientId);
                                            rs3 = pstmt3.executeQuery();
                                            
                                            if (rs3.next()) {
                                                // 直接从订单明细获取价格
                                                priceActual = getBigDecimal(rs3, "PriceActual");
                                                priceEntered = getBigDecimal(rs3, "PriceEntered");
                                                priceList = getBigDecimal(rs3, "PriceList");
                                                priceLimit = getBigDecimal(rs3, "PriceLimit");
                                                discount = getBigDecimal(rs3, "Discount");
                                                
                                                // 税率信息也从订单明细获取
                                                taxId = rs3.getInt("C_Tax_ID");
                                                if (rs3.wasNull()) {
                                                    taxId = null;
                                                }
                                                
                                                isTaxIncluded = "Y".equals(rs3.getString("IsTaxIncluded"));
                                                currencyId = rs3.getInt("C_Currency_ID");
                                                taxRate = getBigDecimal(rs3, "Rate");
                                                
                                                log.fine("从订单明细获取退货单价格成功: C_OrderLine_ID=" + orderLineId + 
                                                        ", PriceActual=" + priceActual + 
                                                        ", IsTaxIncluded=" + isTaxIncluded + 
                                                        ", TaxRate=" + taxRate);
                                            } else {
                                                log.warning("未找到订单行信息: C_OrderLine_ID=" + orderLineId);
                                            }
                                        } catch (Exception e) {
                                            log.warning("查询订单明细失败: " + e.getMessage());
                                        } finally {
                                            DB.close(rs3, pstmt3);
                                        }
                                    }
                                } else {
                                    log.fine("原始收发货单行没有对应的订单行ID: M_InOutLine_ID=" + originalInOutLineId);
                                }
                            } catch (Exception e) {
                                log.warning("查询原始收发货单行失败: " + e.getMessage());
                            } finally {
                                DB.close(rs2, pstmt2);
                            }
                        } else {
                            log.fine("退货单明细没有关联的原始收发货单行ID");
                        }
                        
                        // 如果从订单明细获取价格失败，使用退货授权明细的金额计算单价
                        if (priceActual == null || priceActual.compareTo(BigDecimal.ZERO) == 0) {
                            // 计算价格
                            if (qtyOrdered != null && qtyOrdered.compareTo(BigDecimal.ZERO) != 0) {
                                // 从总金额计算单价
                                priceActual = rmaAmt.divide(qtyOrdered, 12, RoundingMode.HALF_UP);
                                priceEntered = priceActual;
                                priceList = priceActual;
                                priceLimit = priceActual;
                                log.fine("从退货授权明细计算价格: 金额=" + rmaAmt + ", 数量=" + qtyOrdered + ", 单价=" + priceActual);
                            } else {
                                // 如果数量为0，使用默认价格
                                priceActual = BigDecimal.ZERO;
                                priceEntered = BigDecimal.ZERO;
                                priceList = BigDecimal.ZERO;
                                priceLimit =BigDecimal.ZERO;
                            }
                        }
                    } else {
                        log.warning("未找到退货授权明细信息: M_RMALine_ID=" + rmaLineId);
                    }
                    
                    DB.close(rs, pstmt);
                } else {
                    // 没有退货授权明细ID，尝试直接从退货单行获取订单行ID
                    log.warning("退货单没有M_RMALine_ID字段: M_InOutLine_ID=" + iol.get_ID());
                    
                    // 尝试直接从当前退货单行获取订单行ID
                    if (iol.getC_OrderLine_ID() > 0) {
                        orderLineId = iol.getC_OrderLine_ID();
                        
                        // 从订单明细获取价格信息
                        String orderSql = "SELECT " +
                            "ol.PriceActual, ol.PriceEntered, ol.PriceList, ol.PriceLimit, " +
                            "ol.Discount, ol.QtyOrdered, ol.QtyDelivered, ol.QtyInvoiced, " +
                            "ol.C_Tax_ID, COALESCE(o.IsTaxIncluded, 'N') as IsTaxIncluded, " +
                            "COALESCE(o.C_Currency_ID, ?) as C_Currency_ID, " +
                            "COALESCE(t.Rate, 0) as Rate " +
                            "FROM C_OrderLine ol " +
                            "INNER JOIN C_Order o ON ol.C_Order_ID = o.C_Order_ID " +
                            "LEFT JOIN C_Tax t ON ol.C_Tax_ID = t.C_Tax_ID " +
                            "WHERE ol.C_OrderLine_ID = ? " +
                            "AND ol.IsActive = 'Y' " +
                            "AND ol.AD_Client_ID=? " +
                            "AND o.AD_Client_ID=?";
                        
                        pstmt = DB.prepareStatement(orderSql, get_TrxName());
                        pstmt.setInt(1, currencyId);
                        pstmt.setInt(2, orderLineId);
                        pstmt.setInt(3, clientId);
                        pstmt.setInt(4, clientId);
                        rs = pstmt.executeQuery();
                        
                        if (rs.next()) {
                            priceActual = getBigDecimal(rs, "PriceActual");
                            priceEntered = getBigDecimal(rs, "PriceEntered");
                            priceList = getBigDecimal(rs, "PriceList");
                            priceLimit = getBigDecimal(rs, "PriceLimit");
                            discount = getBigDecimal(rs, "Discount");
                            
                            qtyOrdered = getBigDecimal(rs, "QtyOrdered");
                            qtyDelivered = getBigDecimal(rs, "QtyDelivered");
                            qtyInvoiced = getBigDecimal(rs, "QtyInvoiced");
                            
                            taxId = rs.getInt("C_Tax_ID");
                            if (rs.wasNull()) {
                                taxId = null;
                            }
                            
                            isTaxIncluded = "Y".equals(rs.getString("IsTaxIncluded"));
                            currencyId = rs.getInt("C_Currency_ID");
                            taxRate = getBigDecimal(rs, "Rate");
                            
                            log.fine("直接从退货单行获取订单价格: C_OrderLine_ID=" + orderLineId + 
                                    ", PriceActual=" + priceActual);
                        } else {
                            log.warning("未找到订单行信息: C_OrderLine_ID=" + orderLineId);
                        }
                        
                        DB.close(rs, pstmt);
                    }
                }
                
                // 如果是退货单，保持原有的LineType值（1000718或1000720）
                log.fine("退货单处理完成: M_InOutLine_ID=" + iol.get_ID() + 
                        ", 类型=" + lineType + 
                        ", 最终数量=" + movementQty + 
                        ", 最终单价=" + priceActual);
                
            } else if (iol.getC_OrderLine_ID() > 0) {
                // 非退货单，从订单行获取数据（原逻辑）
                orderLineId = iol.getC_OrderLine_ID();
                
                log.fine("处理普通收发货单: M_InOutLine_ID=" + iol.get_ID() + 
                        ", C_OrderLine_ID=" + orderLineId);
                
                String orderSql = "SELECT " +
                    "ol.PriceActual, ol.PriceEntered, ol.PriceList, ol.PriceLimit, " +
                    "ol.Discount, ol.QtyOrdered, ol.QtyDelivered, ol.QtyInvoiced, " +
                    "ol.C_Tax_ID, COALESCE(o.IsTaxIncluded, 'N') as IsTaxIncluded, " +
                    "COALESCE(o.C_Currency_ID, ?) as C_Currency_ID, " +
                    "COALESCE(t.Rate, 0) as Rate " +
                    "FROM C_OrderLine ol " +
                    "INNER JOIN C_Order o ON ol.C_Order_ID = o.C_Order_ID " +
                    "LEFT JOIN C_Tax t ON ol.C_Tax_ID = t.C_Tax_ID " +
                    "WHERE ol.C_OrderLine_ID = ? " +
                    "AND ol.IsActive = 'Y' " +
                    "AND ol.AD_Client_ID=? " +
                    "AND o.AD_Client_ID=?";
                
                pstmt = DB.prepareStatement(orderSql, get_TrxName());
                pstmt.setInt(1, currencyId);
                pstmt.setInt(2, orderLineId);
                pstmt.setInt(3, clientId);
                pstmt.setInt(4, clientId);
                rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    priceActual = getBigDecimal(rs, "PriceActual");
                    priceEntered = getBigDecimal(rs, "PriceEntered");
                    priceList = getBigDecimal(rs, "PriceList");
                    priceLimit = getBigDecimal(rs, "PriceLimit");
                    discount = getBigDecimal(rs, "Discount");
                    
                    qtyOrdered = getBigDecimal(rs, "QtyOrdered");
                    qtyDelivered = getBigDecimal(rs, "QtyDelivered");
                    qtyInvoiced = getBigDecimal(rs, "QtyInvoiced");
                    
                    taxId = rs.getInt("C_Tax_ID");
                    if (rs.wasNull()) {
                        taxId = null;
                    }
                    
                    isTaxIncluded = "Y".equals(rs.getString("IsTaxIncluded"));
                    currencyId = rs.getInt("C_Currency_ID");
                    taxRate = getBigDecimal(rs, "Rate");
                    
                    log.fine("普通收发货单获取价格成功: PriceActual=" + priceActual + 
                            ", IsTaxIncluded=" + isTaxIncluded + 
                            ", TaxRate=" + taxRate);
                } else {
                    log.warning("未找到订单行信息: C_OrderLine_ID=" + orderLineId);
                }
                
                DB.close(rs, pstmt);
            } else {
                // 既不是退货单也没有订单行
                log.fine("入库单行既不是退货单也没有订单行: M_InOutLine_ID=" + iol.get_ID());
                
                // 设置默认值
                qtyOrdered = movementQty;
                qtyDelivered = movementQty;
                qtyInvoiced = movementQty;
                
                // 获取默认税率
                String taxSql = "SELECT Rate FROM C_Tax WHERE AD_Client_ID = ? AND IsActive='Y' ORDER BY IsDefault DESC, Created DESC LIMIT 1";
                BigDecimal rate = DB.getSQLValueBD(get_TrxName(), taxSql, clientId);
                taxRate = (rate != null) ? rate : BigDecimal.ZERO;
                
                // 设置默认价格
                priceActual = BigDecimal.ZERO;
                priceEntered = BigDecimal.ZERO;
                priceList = BigDecimal.ZERO;
                priceLimit = BigDecimal.ZERO;
                
                log.fine("使用默认值: 价格=" + priceActual + ", 税率=" + taxRate);
            }

            // 获取发票行信息
            String matchInvSql = "SELECT C_InvoiceLine_ID FROM M_MatchInv " +
                               "WHERE M_InOutLine_ID = ? " +
                               "AND IsActive = 'Y' " +
                               "AND AD_Client_ID=? " +
                               "ORDER BY Created DESC " +
                               "LIMIT 1";
            
            pstmt = DB.prepareStatement(matchInvSql, get_TrxName());
            pstmt.setInt(1, iol.get_ID());
            pstmt.setInt(2, clientId);
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                invoiceLineId = rs.getInt("C_InvoiceLine_ID");
                if (!rs.wasNull() && invoiceLineId > 0) {
                    log.fine("找到发票行关联: C_InvoiceLine_ID=" + invoiceLineId);
                }
            }
            
            DB.close(rs, pstmt);
            
            // 如果价格为空，使用默认价格
            if (priceActual == null || priceActual.compareTo(BigDecimal.ZERO) == 0) {
                priceActual = BigDecimal.ZERO;
                log.warning("价格为空，使用默认价格: 0");
            }
            if (priceEntered == null || priceEntered.compareTo(BigDecimal.ZERO) == 0) {
                priceEntered = priceActual;
            }
            if (priceList == null || priceList.compareTo(BigDecimal.ZERO) == 0) {
                priceList = priceActual;
            }
            if (priceLimit == null || priceLimit.compareTo(BigDecimal.ZERO) == 0) {
                priceLimit = priceActual;
            }
            
            // 计算金额 - 根据是否含税进行计算
            BigDecimal lineNetAmt;
            BigDecimal lineTotalAmt;
            
            if (isTaxIncluded) {
                // 价格已含税
                lineTotalAmt = priceActual.multiply(movementQty);
                if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
                    // 计算不含税金额
                    BigDecimal divisor = BigDecimal.ZERO.add(
                        taxRate.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP));
                    lineNetAmt = lineTotalAmt.divide(divisor, 12, RoundingMode.HALF_UP);
                    log.fine("价格含税计算: 含税总额=" + lineTotalAmt + ", 税率=" + taxRate + "%, 不含税净额=" + lineNetAmt);
                } else {
                    lineNetAmt = lineTotalAmt;
                    log.fine("价格含税但税率为0: 净额=总额=" + lineNetAmt);
                }
            } else {
                // 价格不含税
                lineNetAmt = priceActual.multiply(movementQty);
                if (taxRate != null && taxRate.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal multiplier = BigDecimal.ZERO.add(
                        taxRate.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP));
                    lineTotalAmt = lineNetAmt.multiply(multiplier);
                    log.fine("价格不含税计算: 不含税净额=" + lineNetAmt + ", 税率=" + taxRate + "%, 含税总额=" + lineTotalAmt);
                } else {
                    lineTotalAmt = lineNetAmt;
                    log.fine("价格不含税且税率为0: 净额=总额=" + lineNetAmt);
                }
            }
            
            // 四舍五入到2位小数
            lineNetAmt = lineNetAmt.setScale(2, RoundingMode.HALF_UP);
            lineTotalAmt = lineTotalAmt.setScale(2, RoundingMode.HALF_UP);
            
            log.fine("最终金额计算: 价格=" + priceActual + 
                    ", 数量=" + movementQty + 
                    ", 税率=" + taxRate + 
                    ", 是否含税=" + isTaxIncluded +
                    ", 净额=" + lineNetAmt + 
                    ", 总额=" + lineTotalAmt);
            
            // 生成行号
            String lineNoSql = "SELECT COALESCE(MAX(Line), 0) + 10 FROM C_ReconciliationLine " +
                              "WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
            int lineNo = DB.getSQLValueEx(get_TrxName(), lineNoSql, reconId, clientId);
            if (lineNo < 10) {
                lineNo = 10;
            }
            
            // 获取ID和上下文
            int lineId = DB.getNextID(clientId, "C_ReconciliationLine", get_TrxName());
            
            // 执行插入
            String insertSql = buildReconciliationLineInsertSql();
            pstmt = DB.prepareStatement(insertSql, get_TrxName());
            
            int paramIndex = 1;
            Timestamp now = new Timestamp(System.currentTimeMillis());
            
            try {
                // 设置参数
                pstmt.setInt(paramIndex++, lineId);
                pstmt.setInt(paramIndex++, clientId);
                pstmt.setInt(paramIndex++, orgId);
                pstmt.setString(paramIndex++, "Y");  // IsActive
                pstmt.setTimestamp(paramIndex++, now);  // Created
                pstmt.setInt(paramIndex++, currentUserId);  // CreatedBy
                pstmt.setTimestamp(paramIndex++, now);  // Updated
                pstmt.setInt(paramIndex++, currentUserId);  // UpdatedBy
                pstmt.setInt(paramIndex++, reconId);
                pstmt.setInt(paramIndex++, 320);  // AD_Table_ID
                pstmt.setInt(paramIndex++, iol.get_ID());  // Record_ID
                // 设置LineType
                if (lineType != null && lineType > 0) {
                    pstmt.setInt(paramIndex++, lineType);  // LineType - 存储C_DocType_ID
                } else {
                    pstmt.setInt(paramIndex++, iol.getParent().isSOTrx() ? 1000004 : 1000003);  // 假设默认值
                }
                pstmt.setString(paramIndex++, iol.getParent().getDocumentNo());  // DocumentNo
                pstmt.setTimestamp(paramIndex++, new Timestamp(iol.getParent().getMovementDate().getTime()));  // DateDoc
                pstmt.setInt(paramIndex++, lineNo);
                pstmt.setString(paramIndex++, p_ReconPeroid);  // ReconPeroid
                pstmt.setInt(paramIndex++, productId);
                pstmt.setInt(paramIndex++, uomId);
                
                // 数量字段
                setBigDecimal(pstmt, paramIndex++, qtyOrdered);
                setBigDecimal(pstmt, paramIndex++, qtyDelivered);
                setBigDecimal(pstmt, paramIndex++, qtyInvoiced);
                setBigDecimal(pstmt, paramIndex++, movementQty);  // MovementQty
                setBigDecimal(pstmt, paramIndex++, movementQty);  // QtyToReconcile
                
                // 价格字段
                setBigDecimal(pstmt, paramIndex++, priceEntered);
                setBigDecimal(pstmt, paramIndex++, priceActual);
				setBigDecimal(pstmt, paramIndex++, priceActual);
                setBigDecimal(pstmt, paramIndex++, priceList);
                setBigDecimal(pstmt, paramIndex++, priceLimit);
                setBigDecimal(pstmt, paramIndex++, discount);
                
                // 货币和税务
                pstmt.setInt(paramIndex++, currencyId);
                setInteger(pstmt, paramIndex++, taxId);
                setBigDecimal(pstmt, paramIndex++, taxRate);
                pstmt.setString(paramIndex++, isTaxIncluded ? "Y" : "N");
                
                // 金额
                setBigDecimal(pstmt, paramIndex++, lineNetAmt);
                setBigDecimal(pstmt, paramIndex++, lineTotalAmt);
                setBigDecimal(pstmt, paramIndex++, lineTotalAmt);  // ReconciledAmt
                setBigDecimal(pstmt, paramIndex++, BigDecimal.ZERO);  // DifferenceAmt
                
                // 关联字段
                setInteger(pstmt, paramIndex++, orderLineId);  // C_OrderLine_ID
                setInteger(pstmt, paramIndex++, invoiceLineId);  // C_InvoiceLine_ID
                pstmt.setInt(paramIndex++, iol.get_ID());  // M_InOutLine_ID
                
                // 状态字段
                pstmt.setString(paramIndex++, "PE");  // ReconStatus
                pstmt.setString(paramIndex++, "N");   // IsReconciled
                pstmt.setString(paramIndex++, "N");   // Processed
                pstmt.setString(paramIndex++, "N");   // IsApproved
                
                // 调试：打印参数个数和SQL
                log.fine("准备插入C_ReconciliationLine，参数个数: " + paramIndex);
                
                // 执行插入
                int result = pstmt.executeUpdate();
                
                if (result > 0) {
                    if (isReturnType) {
                        log.info("创建退货单明细行成功: ID=" + lineId + 
                                ", LineType=" + lineType + 
                                ", 移动数量=" + movementQty + 
                                ", 单价=" + priceActual + 
                                ", 净额=" + lineNetAmt + 
                                ", 总额=" + lineTotalAmt +
                                ", 订单行ID=" + (orderLineId != null ? orderLineId : "null"));
                    } else {
                        log.info("创建明细行成功: ID=" + lineId + ", LineType=" + lineType + 
                                ", 产品ID=" + productId + ", 数量=" + movementQty +
                                ", 订单行ID=" + (orderLineId != null ? orderLineId : "null"));
                    }
                    return true;
                } else {
                    log.severe("创建明细行失败: 插入0行");
                    return false;
                }
                
            } catch (SQLException e) {
                log.severe("插入SQL执行失败: " + e.getMessage());
                log.severe("SQL: " + insertSql);
                throw e; // 重新抛出异常，让上层处理
            }
            
        } catch (Exception e) {
            log.severe("创建明细行失败 - M_InOutLine_ID=" + iol.get_ID() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DB.close(rs, pstmt);
        }
    }

    /**
    * 构建插入语句
    */
   private String buildReconciliationLineInsertSql() {
		return "INSERT INTO C_ReconciliationLine (" + "C_ReconciliationLine_ID, AD_Client_ID, AD_Org_ID, IsActive, "
				+ "Created, CreatedBy, Updated, UpdatedBy, C_Reconciliation_ID, "
				+ "AD_Table_ID, Record_ID, LineType, DocumentNo, DateDoc, Line, "
				+ "ReconPeroid, M_Product_ID, C_UOM_ID, "
				+ "QtyOrdered, QtyDelivered, QtyInvoiced, MovementQty, QtyToReconcile, "
				+ "PriceEntered, PriceActual, PriceToReconcile, PriceList, PriceLimit, Discount, "
				+ "C_Currency_ID, C_Tax_ID, TaxRate, IsTaxIncluded, "
				+ "LineNetAmt, LineTotalAmt, ReconciledAmt, DifferenceAmt, "
				+ "C_OrderLine_ID, C_InvoiceLine_ID, M_InOutLine_ID, "
				+ "ReconStatus, IsReconciled, Processed, IsApproved) " + "VALUES (?, ?, ?, ?, " + "?, ?, ?, ?, ?, " + // 5
				"?, ?, ?, ?, ?, ?, " + // 6
				"?, ?, ?, " + // 3
				"?, ?, ?, ?, ?, " + // 5
				"?, ?, ?, ?, ?, ?, " + // 6
				"?, ?, ?, ?, " + // 4
				"?, ?, ?, ?, " + // 4
				"?, ?, ?, " + // 3
				"?, ?, ?, ?)"; // 4
   }    
   
    /**
     * 计算表头汇总
     */
    private void calculateHeaderTotalsDirectly(int reconId, int bpartnerId, int clientId) {
        try {
            String sumSql = "SELECT COALESCE(SUM(LineTotalAmt), 0), " +
                           "COALESCE(SUM(MovementQty), 0) " +
                           "FROM C_ReconciliationLine WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
            
            List<Object> sums = DB.getSQLValueObjectsEx(get_TrxName(), sumSql, reconId, clientId);
            if (sums != null && sums.size() >= 2) {
                BigDecimal totalAmt = (BigDecimal) sums.get(0);
                BigDecimal totalQty = (BigDecimal) sums.get(1);
                
                BigDecimal beginningBalance = getBeginningBalance(bpartnerId, clientId);
                BigDecimal endingBalance = beginningBalance.add(totalAmt);
                
                String updateSql = "UPDATE C_Reconciliation SET " +
                    "TotalReconAmt=?, TotalReconQty=?, " +
                    "BeginningBalance=?, EndingBalance=? " +
                    "WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
                
                DB.executeUpdate(updateSql, 
                    new Object[] {totalAmt, totalQty, beginningBalance, endingBalance, reconId, clientId},
                    false, get_TrxName());
                    
                log.fine("计算汇总完成: 对账单ID=" + reconId + ", 总金额=" + totalAmt);
            }
        } catch (Exception e) {
            log.warning("计算汇总失败, 对账单ID=" + reconId + ": " + e.getMessage());
        }
    }
    
    /**
     * 获取期初余额
     */
    private BigDecimal getBeginningBalance(int bpartnerId, int clientId) {
        try {
            String prevPeriod = getPreviousPeriod(p_ReconPeroid);
            String sql = "SELECT EndingBalance FROM C_Reconciliation " +
                        "WHERE C_BPartner_ID=? AND ReconPeroid=? AND AD_Client_ID=? " +
                        "ORDER BY Created DESC LIMIT 1";
            
            BigDecimal balance = DB.getSQLValueBD(get_TrxName(), sql, bpartnerId, prevPeriod, clientId);
            return balance != null ? balance : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }       
    
    /**
     * 获取上一个期间
     */
    private String getPreviousPeriod(String currentPeriod) {
        try {
            int year = Integer.parseInt(currentPeriod.substring(0, 4));
            int month = Integer.parseInt(currentPeriod.substring(4, 6));
            
            if (month == 1) {
                year--;
                month = 12;
            } else {
                month--;
            }
            
            return String.format("%04d%02d", year, month);
        } catch (Exception e) {
            return currentPeriod;
        }
    }
    
    /**
     * 设置BigDecimal参数，处理null值
     */
    private void setBigDecimal(PreparedStatement pstmt, int index, BigDecimal value) throws SQLException {
        if (value != null) {
            pstmt.setBigDecimal(index, value);
        } else {
            pstmt.setNull(index, Types.DECIMAL);
        }
    }
    
    /**
     * 设置Integer参数，处理null值
     */
    private void setInteger(PreparedStatement pstmt, int index, Integer value) throws SQLException {
        if (value != null && value > 0) {
            pstmt.setInt(index, value);
        } else {
            pstmt.setNull(index, Types.INTEGER);
        }
    }
    
    /**
     * 安全的BigDecimal获取
     */
    private BigDecimal getBigDecimal(ResultSet rs, String columnName) throws SQLException {
        BigDecimal value = rs.getBigDecimal(columnName);
        return rs.wasNull() ? BigDecimal.ZERO : value;
    }
    
    public int getAD_Client_ID() {
        return Env.getAD_Client_ID(getCtx());
    }
    
    public int getAD_Org_ID() {
        int orgId = Env.getAD_Org_ID(getCtx());
        return orgId > 0 ? orgId : 0;
    }
    
    public int getAD_User_ID() {
        int userId = Env.getAD_User_ID(getCtx());
        return userId > 0 ? userId : getProcessInfo().getAD_User_ID();
    }
}