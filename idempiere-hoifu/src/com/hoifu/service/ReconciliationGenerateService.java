package com.hoifu.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;

import org.compiere.model.MBPartner;
import org.compiere.model.MInOutLine;
import org.compiere.model.MSequence;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;


public class ReconciliationGenerateService {  
	
	public static final int DOCTYPE_CUSTOMER_RETURN = 1000720;  //客户退货类型
	public static final int DOCTYPE_VENDOR_RETURN = 1000718;    //供应商退货类型
	
	private Properties ctx;  
    private String trxName; 
    private int clientID;
    private int userID;
    private List<String> logMessages = new ArrayList<>();
    private static CLogger log = CLogger.getCLogger(ReconciliationGenerateService.class);
    
    public ReconciliationGenerateService(Properties ctx, String trxName, int clientID, int userID) {  
        this.ctx = ctx;  
        this.trxName = trxName;  
        this.clientID = clientID;
        this.userID = userID;
    }  
    
	public GenerateResult generate(int p_C_BPartner_ID, String p_ReconPeroid, int p_AD_Org_ID, boolean p_IsSOTrx)
			throws Exception {
		String originalDisableRecentItems = Env.getContext(ctx, "#DisableRecentItems");
		Env.setContext(ctx, "#DisableRecentItems", "Y");

		try {
			int totalBPartners = 0;
			int successBPartners = 0;
			int errorBPartners = 0;
			int totalLinesCreated = 0;
			int totalDocumentsCreated = 0;

			List<String> successMessages = new ArrayList<>();
			List<String> errorMessages = new ArrayList<>();

			// 从M_InOutLine直接查出需要处理的(业务伙伴, 组织)对
			// 根据参数动态拼SQL，组织始终取收发单的组织
			StringBuilder sqlBuilder = new StringBuilder("SELECT DISTINCT io.C_BPartner_ID, bp.Name, io.AD_Org_ID "
					+ "FROM M_InOut io " + "INNER JOIN M_InOutLine iol ON iol.M_InOut_ID = io.M_InOut_ID "
					+ "INNER JOIN C_BPartner bp ON io.C_BPartner_ID = bp.C_BPartner_ID "
					+ "WHERE io.DocStatus IN ('CO','CL') " + "AND io.AD_Client_ID = ? " + "AND bp.IsActive = 'Y' "
					+ "AND iol.ReconciliationMonth = ? " + "AND (io.IsSOTrx = ? OR io.C_DocType_ID = ?) ");

			if (p_C_BPartner_ID > 0) {
				// 选了具体业务伙伴：过滤该业务伙伴
				sqlBuilder.append("AND io.C_BPartner_ID = ? ");
			} else {
				// 未选业务伙伴：按客户/供应商类型过滤
				sqlBuilder.append("AND ").append(p_IsSOTrx ? "bp.IsCustomer='Y'" : "bp.IsVendor='Y'").append(" ");
			}

			if (p_AD_Org_ID > 0) {
				// 选了具体组织：过滤该组织
				sqlBuilder.append("AND io.AD_Org_ID = ? ");
			}
			// 选了所有组织(p_AD_Org_ID=0)：不加组织过滤，查所有组织

			sqlBuilder.append("ORDER BY bp.Name, io.AD_Org_ID");

			// 收集(业务伙伴, 组织)处理列表
			List<int[]> bpartnerOrgList = new ArrayList<>(); // [bpartnerId, orgId]
			List<String> bpartnerNames = new ArrayList<>();
			Set<String> processedKeys = new HashSet<>(); // key: "bpartnerId_orgId"

			PreparedStatement pstmt = DB.prepareStatement(sqlBuilder.toString(), trxName);
			int paramIdx = 1;
			pstmt.setInt(paramIdx++, clientID);
			pstmt.setString(paramIdx++, p_ReconPeroid);
			pstmt.setString(paramIdx++, p_IsSOTrx ? "Y" : "N");
			pstmt.setInt(paramIdx++, p_IsSOTrx ? DOCTYPE_CUSTOMER_RETURN : DOCTYPE_VENDOR_RETURN);
			if (p_C_BPartner_ID > 0) {
				pstmt.setInt(paramIdx++, p_C_BPartner_ID);
			}
			if (p_AD_Org_ID > 0) {
				pstmt.setInt(paramIdx++, p_AD_Org_ID);
			}

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				int bpartnerId = rs.getInt("C_BPartner_ID");
				String bpartnerName = rs.getString("Name");
				int orgId = rs.getInt("AD_Org_ID");
				String key = bpartnerId + "_" + orgId;
				if (!processedKeys.contains(key)) {
					bpartnerOrgList.add(new int[] { bpartnerId, orgId });
					bpartnerNames.add(bpartnerName);
					processedKeys.add(key);
				}
			}
			DB.close(rs, pstmt);

			totalBPartners = bpartnerOrgList.size();
			if (totalBPartners == 0) {
				String msg = p_C_BPartner_ID > 0
						? "业务伙伴(ID=" + p_C_BPartner_ID + ")在期间 " + p_ReconPeroid + " 没有符合条件的收发单明细"
						: "期间 " + p_ReconPeroid + " 没有找到符合条件的收发单明细";
				return new GenerateResult(msg, new ArrayList<>(logMessages));
			}

			// 处理每个(业务伙伴, 组织)对
			for (int i = 0; i < bpartnerOrgList.size(); i++) {
				int bpartnerId = bpartnerOrgList.get(i)[0];
				int orgId = bpartnerOrgList.get(i)[1];
				String bpartnerName = bpartnerNames.get(i);

				try {
					ProcessResult result = processSingleBPartner(bpartnerId, bpartnerName, p_ReconPeroid, orgId,
							p_IsSOTrx);

					if (result.isSuccess()) {
						successBPartners++;
						totalDocumentsCreated++;
						totalLinesCreated += result.getLinesCreated();
						String successMsg = result.getProcessMessage();
						successMessages.add(successMsg);
						addLog(0, null, null, successMsg);
					} else {
						errorBPartners++;
						String errorMsg = result.getErrorMessage();
						errorMessages.add(errorMsg);
						addLog(0, null, null, errorMsg);
					}

				} catch (Exception e) {
					errorBPartners++;
					String errorMsg = "业务伙伴【" + bpartnerName + "】[组织:" + orgId + "]处理异常: " + e.getMessage();
					errorMessages.add(errorMsg);
					addLog(0, null, null, errorMsg);
					log.log(Level.SEVERE, "处理业务伙伴异常: " + bpartnerName, e);
				}
			}

			StringBuilder resultMsg = new StringBuilder();
			resultMsg.append("<b>批量生成对账单完成！</b><br/>");
			resultMsg.append("【统计信息】<br/>");
			resultMsg.append("处理总数: ").append(totalBPartners).append(" 个<br/>");
			resultMsg.append("成功处理: ").append(successBPartners).append(" 个<br/>");
			resultMsg.append("跳过/失败: ").append(errorBPartners).append(" 个<br/>");
			resultMsg.append("生成对账单: ").append(totalDocumentsCreated).append(" 个<br/>");
			resultMsg.append("生成明细行数: ").append(totalLinesCreated).append(" 行<br/>");

			return new GenerateResult(resultMsg.toString(), new ArrayList<>(logMessages));

		} finally {
			if (originalDisableRecentItems != null) {
				Env.setContext(ctx, "#DisableRecentItems", originalDisableRecentItems);
			} else {
				Env.setContext(ctx, "#DisableRecentItems", "");
			}
		}
	}
    
    /**
     * 创建对账单表头
     */
    private int createReconciliationHeader(int bpartnerId, String reconPeroid, int orgId, boolean isSOTrx) {
        int clientId = clientID;
        
        // 1. 先检查是否已存在对账单（增加 IsSOTrx 条件）  
        String checkSql = "SELECT C_Reconciliation_ID, DocumentNo, DocStatus " +  
            "FROM C_Reconciliation " +  
            "WHERE C_BPartner_ID = ? " +  
            "AND ReconPeroid = ? " +  
            "AND AD_Org_ID = ? " +  
            "AND IsSOTrx = ? " +  // 新增  
            "AND AD_Client_ID = ? " +  
            "ORDER BY Created DESC LIMIT 1";  
        
        int existingReconId = 0;  
        String existingDocNo = "";  
        String existingDocStatus = "";  
        
        PreparedStatement checkStmt = null;  
        ResultSet checkRs = null; 
        
        try {
        	checkStmt = DB.prepareStatement(checkSql, trxName);  
            checkStmt.setInt(1, bpartnerId);  
            checkStmt.setString(2, reconPeroid);  
            checkStmt.setInt(3, orgId);  
            checkStmt.setString(4, isSOTrx ? "Y" : "N"); // 新增参数  
            checkStmt.setInt(5, clientId);  
            checkRs = checkStmt.executeQuery();  
            
            if (checkRs.next()) {
                existingReconId = checkRs.getInt("C_Reconciliation_ID");
                existingDocNo = checkRs.getString("DocumentNo");
                existingDocStatus = checkRs.getString("DocStatus");
                
                log.info("对账单已存在: ID=" + existingReconId + 
                        ", 单据号=" + existingDocNo + 
                        ", 状态=" + existingDocStatus);
                
                // 如果状态是"已完成"或"已关闭"，直接返回
                if ("CO".equals(existingDocStatus) || "CL".equals(existingDocStatus)) {
                    log.info("对账单状态为" + existingDocStatus + "，跳过重新生成");
                    return existingReconId;
                }
                
                // 如果不是完成状态，更新这个对账单
                log.info("对账单状态为" + existingDocStatus + "，将更新现有对账单");
                return existingReconId;
            }
        } catch (SQLException e) {
            log.warning("检查对账单存在性失败: " + e.getMessage());
        } finally {
            DB.close(checkRs, checkStmt);
        }
        
        // 2. 如果不存在，创建新的对账单
        if (existingReconId == 0) {
            return createNewReconciliationHeader(bpartnerId, reconPeroid, orgId, isSOTrx);  // ✅ 改为调用新方法
        }
        
        return existingReconId;
    }

    /**
     * 创建新的对账单表头（实际执行插入操作）
     */
    private int createNewReconciliationHeader(int bpartnerId, String reconPeroid, int orgId, boolean isSOTrx) {
        int clientId = clientID;
        int userId = userID;
        
        try {
            // 生成新ID
            int newReconId = DB.getNextID(clientId, "C_Reconciliation", trxName);
            
            // 生成单据号
            String documentNo = MSequence.getDocumentNo(clientId, "C_Reconciliation", trxName);
            
            // 1. 获取供应商的cutOffday
            int cutoffDay = getBPartnerCutoffDay(bpartnerId, clientId);
            log.info("供应商 " + bpartnerId + " 的cutoffday=" + cutoffDay);
            
            // 2. 计算对账开始日期和结束日期
            Map<String, java.sql.Date> dates = calculateReconciliationDates(reconPeroid, cutoffDay);
            java.sql.Date reconciliationDay = dates.get("reconciliationDay");
            java.sql.Date reconciliationCutoff = dates.get("reconciliationCutoff");
            
            // 对账日期使用当前日期
            java.sql.Date reconciliationDate = new java.sql.Date(System.currentTimeMillis());
            
            // 3. 获取货币ID
            int currencyId = Env.getContextAsInt(ctx, "$C_Currency_ID");
            
			// 获取业务伙伴主地址和主联系人
			MBPartner bp = new MBPartner(ctx, bpartnerId, trxName);
			int locationId = bp.getPrimaryC_BPartner_Location_ID(); // 若无则为0
			int adUserId = bp.getPrimaryAD_User_ID(); // 若无则为-1

            // 4. 完整的插入语句（增加 IsSOTrx 字段）  
            String insertSql = "INSERT INTO C_Reconciliation (" +  
					"C_Reconciliation_ID, AD_Client_ID, AD_Org_ID, IsActive, "
					+ "Created, CreatedBy, Updated, UpdatedBy, "
					+ "DocumentNo, C_BPartner_ID, C_BPartner_Location_ID, AD_User_ID, IsSOTrx, "
					+ "ReconciliationDay, ReconciliationCutoff, ReconciliationDate, "
					+ "BeginningBalance, TotalInvoiceAmt, TotalPaymentAmt, "
					+ "TotalAdjustmentAmt, EndingBalance, C_Currency_ID, "
					+ "DocStatus, DocAction, Processed, Processing, " + "IsApproved, IsConfirmed, ReconPeroid) "
					+ "VALUES (?, ?, ?, 'Y', " + "NOW(), ?, NOW(), ?, " + "?, ?, ?, ?, ?, " + "?, ?, ?, " + "0, 0, 0, "
					+ "0, 0, ?, " + "'DR', 'CO', 'N', 'N', " + "'N', 'N', ?)";
            
			PreparedStatement pstmt = DB.prepareStatement(insertSql, trxName);
			int paramIndex = 1;

			pstmt.setInt(paramIndex++, newReconId);
			pstmt.setInt(paramIndex++, clientId);
			pstmt.setInt(paramIndex++, orgId);
			pstmt.setInt(paramIndex++, userId);
			pstmt.setInt(paramIndex++, userId);
			pstmt.setString(paramIndex++, documentNo);
			pstmt.setInt(paramIndex++, bpartnerId);
			pstmt.setInt(paramIndex++, locationId); // 主地址

			// 仅当有联系人时设置值，否则设为 NULL
			if (adUserId > 0) {
				pstmt.setInt(paramIndex++, adUserId);
			} else {
				pstmt.setNull(paramIndex++, Types.INTEGER);
			}

			pstmt.setString(paramIndex++, isSOTrx ? "Y" : "N");
			pstmt.setDate(paramIndex++, reconciliationDay);
			pstmt.setDate(paramIndex++, reconciliationCutoff);
			pstmt.setDate(paramIndex++, reconciliationDate);
			pstmt.setInt(paramIndex++, currencyId);
			pstmt.setString(paramIndex++, reconPeroid);

			int result = pstmt.executeUpdate();
			DB.close(pstmt);

			if (result > 0) {
				log.info("创建新对账单成功: ID=" + newReconId + ", 单据号=" + documentNo + ", 期间=" + reconPeroid + ", cutoffday="
						+ cutoffDay +
                        ", IsSOTrx=" + (isSOTrx ? "Y" : "N") +  
						", 开始日期=" + reconciliationDay + ", 结束日期=" + reconciliationCutoff + ", 地址ID=" + locationId
						+ ", 联系人ID=" + adUserId);
				return newReconId;
			} else {
				log.warning("创建新对账单失败");
				return 0;
			}
		} catch (SQLException e) {
			log.severe("创建对账单异常: " + e.getMessage());
			e.printStackTrace();
			return 0;
		} catch (Exception e) {
			log.severe("创建对账单时发生未知异常: " + e.getMessage());
			e.printStackTrace();
			return 0;
		}
    }
    
    private ProcessResult processSingleBPartner(int bpartnerId, String bpartnerName, String reconPeroid, int orgId, boolean isSOTrx) {
        ProcessResult result = new ProcessResult();
        int clientId = clientID;
        
        try {
            // 1. 检查是否已存在对账单（增加 IsSOTrx 条件）  
            String checkSql = "SELECT C_Reconciliation_ID, DocumentNo, DocStatus " +  
                "FROM C_Reconciliation " +  
                "WHERE C_BPartner_ID = ? " +  
                "AND ReconPeroid = ? " +  
                "AND AD_Org_ID = ? " +  
                "AND IsSOTrx = ? " +  // 新增  
                "AND AD_Client_ID = ?"; 
            
            int existingReconId = 0;
            String existingDocNo = "";
            String existingDocStatus = "";
            
            PreparedStatement checkStmt = DB.prepareStatement(checkSql, trxName);  
            checkStmt.setInt(1, bpartnerId);  
            checkStmt.setString(2, reconPeroid);  
            checkStmt.setInt(3, orgId);  
            checkStmt.setString(4, isSOTrx ? "Y" : "N"); // 新增参数  
            checkStmt.setInt(5, clientId);  
            ResultSet checkRs = checkStmt.executeQuery();  
            
            if (checkRs.next()) {
                existingReconId = checkRs.getInt("C_Reconciliation_ID");
                existingDocNo = checkRs.getString("DocumentNo");
                existingDocStatus = checkRs.getString("DocStatus");
                
                log.info("找到已存在的对账单: ID=" + existingReconId + 
                        ", 单据号=" + existingDocNo + 
                        ", 状态=" + existingDocStatus);
            }
            DB.close(checkRs, checkStmt);
            
            // 2. 检查状态，如果是完成或关闭，跳过
            if (existingReconId > 0 && ("CO".equals(existingDocStatus) || "CL".equals(existingDocStatus))) {
                String msg = "业务伙伴【" + bpartnerName + "】在期间 " + reconPeroid + 
                            " 已有完成的对账单（单据号：" + existingDocNo + 
                            "，状态：" + existingDocStatus + "），已跳过";
                log.info(msg);
                result.setSuccess(false);
                result.setErrorMessage(msg);
                result.setDuplicate(true);
                return result;
            }
            
            // 3. 获取或创建对账单ID
            int reconId;
            boolean isNew = false;
            
            if (existingReconId > 0) {
                // 使用现有的对账单
                reconId = existingReconId;
                result.setDocumentNo(existingDocNo);
                log.info("使用现有对账单: ID=" + reconId + ", 单据号=" + existingDocNo);
            } else {
                // 创建新的对账单
                reconId = createReconciliationHeader(bpartnerId, reconPeroid, orgId, isSOTrx);
                if (reconId <= 0) {
                    result.setSuccess(false);
                    result.setErrorMessage("创建对账单失败");
                    return result;
                }
                isNew = true;
                log.info("创建新对账单: ID=" + reconId + ", 业务伙伴=" + bpartnerName);
            }
            
            // 4. 清理旧明细行
            int deletedLines = deleteExistingLines(reconId, clientId);
            log.info("删除 " + deletedLines + " 条旧明细行, 对账单ID=" + reconId);
            
            // 5. 获取入库单明细并生成明细行（按业务伙伴类型过滤）  
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
            
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            int lineCount = 0;
            int successCount = 0;
            
            try {
                pstmt = DB.prepareStatement(sql, trxName);
                pstmt.setInt(1, bpartnerId);  
                pstmt.setString(2, reconPeroid);  
                pstmt.setInt(3, clientId);  
                pstmt.setInt(4, clientId);  
                pstmt.setInt(5, orgId);  
                pstmt.setInt(6, orgId);  
                pstmt.setString(7, isSOTrx ? "Y" : "N"); // 主方向  
                pstmt.setInt(8, isSOTrx ? DOCTYPE_CUSTOMER_RETURN : DOCTYPE_VENDOR_RETURN); // 退货单类型（客户退货/供应商退货）
                
                rs = pstmt.executeQuery();  
                
                while (rs.next()) {
                    MInOutLine iol = new MInOutLine(ctx, rs, trxName);
                    boolean success = createReconciliationLineDirectly(reconId, iol, clientId, userID, orgId, reconPeroid);
                    lineCount++;
                    if (success) {
                        successCount++;
                    }
                }
                
                if (lineCount > 0) {
                    // 6. 重新计算汇总
                	calculateHeaderTotalsDirectly(reconId, bpartnerId, clientId, reconPeroid, orgId);
                    
                    // 7. 更新状态
                    String updateSql = "UPDATE C_Reconciliation SET DocStatus='DR' " +
                                     "WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
                    DB.executeUpdate(updateSql, new Object[]{reconId, clientId}, false, trxName);
                    
                    result.setSuccess(true);
                    result.setLinesCreated(successCount);
                    result.setLinesTotal(lineCount);
                    
                    if (isNew) {
                        // 获取新的单据号
                        String newDocNo = DB.getSQLValueStringEx(trxName, 
                            "SELECT DocumentNo FROM C_Reconciliation WHERE C_Reconciliation_ID=?", reconId);
                        result.setDocumentNo(newDocNo != null ? newDocNo : "");
                    }
                    
                    String processMsg = (isNew ? "创建" : "更新") + "对账单成功: " + 
                                      result.getDocumentNo() + " - 业务伙伴【" + bpartnerName + "】" +
                                      " (" + successCount + "/" + lineCount + " 行明细)";
                    if (deletedLines > 0) {
                        processMsg += " (删除旧明细" + deletedLines + "行)";
                    }
                    result.setProcessMessage(processMsg);
                    
                } else {
                    // 如果没有明细，设置状态为草稿
                    String updateSql = "UPDATE C_Reconciliation SET DocStatus='DR' " +
                                      "WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
                    DB.executeUpdate(updateSql, new Object[]{reconId, clientId}, false, trxName);
                    
                    result.setSuccess(true);
                    result.setLinesCreated(0);
                    result.setLinesTotal(0);
                    result.setProcessMessage("无符合条件的入库单明细");
                }
                
            } catch (SQLException e) {
                log.log(Level.SEVERE, "查询入库单明细失败", e);
                result.setSuccess(false);
                result.setErrorMessage("查询入库单明细失败: " + e.getMessage());
            } finally {
                DB.close(rs, pstmt);
            }
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("处理异常: " + e.getMessage());
            log.severe("处理业务伙伴【" + bpartnerName + "】异常: " + e.getMessage());
        }
        
        return result;
    }   
    
    /**
     * 获取期初余额
     */
	private BigDecimal getBeginningBalance(int bpartnerId, int clientId, String reconPeroid, int orgId) {
		try {
			String prevPeriod = getPreviousPeriod(reconPeroid);
			if (prevPeriod == null) {
				return BigDecimal.ZERO;
			}
			String sql = "SELECT EndingBalance FROM C_Reconciliation "
					+ "WHERE C_BPartner_ID=? AND ReconPeroid=? AND AD_Client_ID=? " + "AND AD_Org_ID=? "
					+ "AND DocStatus IN ('CO','CL') " + "ORDER BY Created DESC LIMIT 1";
			BigDecimal balance = DB.getSQLValueBD(trxName, sql, bpartnerId, prevPeriod, clientId, orgId);
			return (balance != null) ? balance : BigDecimal.ZERO;
		} catch (Exception e) {
			log.warning("获取期初余额失败: " + e.getMessage());
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
            
            month--;
            if (month <= 0) {
                month = 12;
                year--;
            }
            
            return String.format("%04d%02d", year, month);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 删除现有明细行
     */
    private int deleteExistingLines(int reconId, int clientId) {
        String deleteSql = "DELETE FROM C_ReconciliationLine WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
        int deleted = DB.executeUpdate(deleteSql, new Object[]{reconId, clientId}, false, trxName);
        if (deleted > 0) {
            log.info("删除 " + deleted + " 条旧明细行, 对账单ID=" + reconId);
        }
        return deleted;
    }
    
    /**
     * 创建对账明细行
     */
    private boolean createReconciliationLineDirectly(int reconId, MInOutLine iol, int clientId, int currentUserId, int orgId, String reconPeroid) {
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
            int currencyId = Env.getContextAsInt(ctx, "$C_Currency_ID");
            Integer taxId = null;
            Integer orderLineId = null;
            Integer invoiceLineId = null;

            // 获取C_DocType_ID作为LineType
            Integer lineType = iol.getParent().getC_DocType_ID();
            
            // 判断是否为退货单（供应商退货单1000718 或 客户退货单1000720）
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
                    
                    pstmt = DB.prepareStatement(rmaSql, trxName);
                    pstmt.setInt(1, rmaLineId);
                    rs = pstmt.executeQuery();
                    
                    if (rs.next()) {
                        // 获取数量
                        qtyOrdered = getBigDecimal(rs, "Qty");
                        qtyDelivered = getBigDecimal(rs, "QtyDelivered");
                        qtyInvoiced = getBigDecimal(rs, "QtyInvoiced");
                        
                        // 获取金额
                        BigDecimal rmaAmt = getBigDecimal(rs, "Amt");
                        BigDecimal rmaLineNetAmt = getBigDecimal(rs, "LineNetAmt");
                        
                        // 获取税率、货币
                        taxId = rs.getInt("C_Tax_ID");
                        if (rs.wasNull()) {
                            taxId = null;
                        }
                        
                        currencyId = rs.getInt("C_Currency_ID");
                        if (rs.wasNull()) {
                            currencyId = Env.getContextAsInt(ctx, "$C_Currency_ID");
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
                                pstmt2 = DB.prepareStatement(orderLineSql, trxName);
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
                                            pstmt3 = DB.prepareStatement(orderSql, trxName);
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
                                priceLimit = BigDecimal.ZERO;
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
                        
                        pstmt = DB.prepareStatement(orderSql, trxName);
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
                
                pstmt = DB.prepareStatement(orderSql, trxName);
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
                BigDecimal rate = DB.getSQLValueBD(trxName, taxSql, clientId);
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
            
            pstmt = DB.prepareStatement(matchInvSql, trxName);
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
            int lineNo = DB.getSQLValueEx(trxName, lineNoSql, reconId, clientId);
            if (lineNo < 10) {
                lineNo = 10;
            }
            
            // 获取ID和上下文
            int lineId = DB.getNextID(clientId, "C_ReconciliationLine", trxName);
            
            // 执行插入
            String insertSql = buildReconciliationLineInsertSql();
            pstmt = DB.prepareStatement(insertSql, trxName);
            
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
                pstmt.setString(paramIndex++, reconPeroid);  // ReconPeroid
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
				+ "ReconStatus, IsReconciled, Processed, IsApproved) " + "VALUES (?, ?, ?, ?, " + 
				"?, ?, ?, ?, ?, " + // 5
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
	private void calculateHeaderTotalsDirectly(int reconId, int bpartnerId, int clientId, String reconPeroid,
			int orgId) {
		try {
			String sumSql = "SELECT COALESCE(SUM(LineTotalAmt), 0), " + "COALESCE(SUM(MovementQty), 0) "
					+ "FROM C_ReconciliationLine WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
			List<Object> sums = DB.getSQLValueObjectsEx(trxName, sumSql, reconId, clientId);
			if (sums != null && sums.size() >= 2) {
				BigDecimal totalAmt = (BigDecimal) sums.get(0);
				BigDecimal totalQty = (BigDecimal) sums.get(1);
				BigDecimal beginningBalance = getBeginningBalance(bpartnerId, clientId, reconPeroid, orgId);
				BigDecimal endingBalance = beginningBalance.add(totalAmt);
				String updateSql = "UPDATE C_Reconciliation SET " + "TotalReconAmt=?, TotalReconQty=?, "
						+ "BeginningBalance=?, EndingBalance=? " + "WHERE C_Reconciliation_ID=? AND AD_Client_ID=?";
				DB.executeUpdate(updateSql,
						new Object[] { totalAmt, totalQty, beginningBalance, endingBalance, reconId, clientId }, false,
						trxName);
				log.fine("计算汇总完成: 对账单ID=" + reconId + ", 总金额=" + totalAmt);
			}
		} catch (Exception e) {
			log.warning("计算汇总失败, 对账单ID=" + reconId + ": " + e.getMessage());
		}
	}
    
    // 辅助方法：安全地处理BigDecimal
    private BigDecimal getBigDecimal(ResultSet rs, String column) throws SQLException {
        BigDecimal value = rs.getBigDecimal(column);
        return rs.wasNull() ? BigDecimal.ZERO : value;
    }
    
    private void setBigDecimal(PreparedStatement pstmt, int paramIndex, BigDecimal value) throws SQLException {
        if (value != null) {
            pstmt.setBigDecimal(paramIndex, value);
        } else {
            pstmt.setNull(paramIndex, java.sql.Types.DECIMAL);
        }
    }
    
    private void setInteger(PreparedStatement pstmt, int paramIndex, Integer value) throws SQLException {
        if (value != null && value > 0) {
            pstmt.setInt(paramIndex, value);
        } else {
            pstmt.setNull(paramIndex, java.sql.Types.INTEGER);
        }
    }
   
    /**
     * 获取供应商的对账日（cutOffday）
     */
    private int getBPartnerCutoffDay(int bpartnerId, int clientId) {
        try {
            // 假设cutOffday存储在C_BPartner表的某个字段中
            // 这里需要根据您的实际表结构调整字段名
            String sql = "SELECT cutoffday FROM C_BPartner " +
                        "WHERE C_BPartner_ID = ? AND AD_Client_ID = ?";
            
            Integer cutoffDay = DB.getSQLValueEx(trxName, sql, bpartnerId, clientId);
            
            // 如果供应商没有设置cutOffday，使用默认值15
            if (cutoffDay == null || cutoffDay <= 0 || cutoffDay > 31) {
                log.info("供应商 " + bpartnerId + " 没有设置cutoffday，使用默认值15");
                return 15; // 默认值
            }
            
            return cutoffDay;
            
        } catch (Exception e) {
            log.warning("获取供应商cutoffday失败，使用默认值15: " + e.getMessage());
            return 15; // 默认值
        }
    }
    
    /**
     * 根据期间和cutoffday计算对账日期
     * @param period 期间，格式：YYYYMM
     * @param cutoffDay 对账日（1-31）
     * @return 包含开始日期和结束日期的Map
     */
    private Map<String, java.sql.Date> calculateReconciliationDates(String period, int cutoffDay) {
        Map<String, java.sql.Date> dates = new HashMap<>();
        
        try {
            // 解析期间
            int year = Integer.parseInt(period.substring(0, 4));
            int month = Integer.parseInt(period.substring(4, 6));
            
            // 计算上个月
            Calendar previousMonthCal = Calendar.getInstance();
            previousMonthCal.set(Calendar.YEAR, year);
            previousMonthCal.set(Calendar.MONTH, month - 2); // 月份从0开始，所以要减2
            previousMonthCal.set(Calendar.DAY_OF_MONTH, 1);
            
            // 计算当前月
            Calendar currentMonthCal = Calendar.getInstance();
            currentMonthCal.set(Calendar.YEAR, year);
            currentMonthCal.set(Calendar.MONTH, month - 1); // 月份从0开始
            currentMonthCal.set(Calendar.DAY_OF_MONTH, 1);
            
            // 获取上个月和当前月的实际天数
            int previousMonthActualDays = previousMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            int currentMonthActualDays = currentMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH);
            
            // 确定实际的对账日（不能超过当月天数）
            int actualCutoffDay = Math.min(cutoffDay, currentMonthActualDays);
            int previousMonthActualCutoffDay = Math.min(cutoffDay, previousMonthActualDays);
            
            // 设置对账结束日期（当前月的cutoffday）
            currentMonthCal.set(Calendar.DAY_OF_MONTH, actualCutoffDay);
            java.sql.Date reconciliationCutoff = new java.sql.Date(currentMonthCal.getTimeInMillis());
            
            // 设置对账开始日期（上个月的cutoffday）
            previousMonthCal.set(Calendar.DAY_OF_MONTH, previousMonthActualCutoffDay);
            java.sql.Date reconciliationDay = new java.sql.Date(previousMonthCal.getTimeInMillis());
            
            dates.put("reconciliationDay", reconciliationDay);
            dates.put("reconciliationCutoff", reconciliationCutoff);
            
            log.fine("期间 " + period + "，cutoffday=" + cutoffDay + 
                    "，开始日期=" + reconciliationDay + 
                    "，结束日期=" + reconciliationCutoff);
            
        } catch (Exception e) {
            log.severe("计算对账日期失败: " + e.getMessage());
            // 出错时使用默认日期（当前日期）
            java.sql.Date currentDate = new java.sql.Date(System.currentTimeMillis());
            dates.put("reconciliationDay", currentDate);
            dates.put("reconciliationCutoff", currentDate);
        }
        
        return dates;
    }
    
    // 示例：addLog 需要适配，因为 Service 没有 ProcessUI，可改为返回日志或通过回调  
    private void addLog(int id, Timestamp date, BigDecimal number, String msg) {  
        logMessages.add(msg); // 收集到列表  
    }  
    
 // 结果封装类  
    public static class GenerateResult {  
        private String summary;  
        private List<String> logs;  
          
        public GenerateResult(String summary, List<String> logs) {  
            this.summary = summary;  
            this.logs = logs;  
        }  
          
        public String getSummary() { return summary; }  
        public List<String> getLogs() { return logs; }  
    }
    
    /**
     * 处理结果类
     */
    private static class ProcessResult {
        private boolean success = false;
        private boolean duplicate = false;  // 新增：重复标志
        private int linesCreated = 0;
        private int linesTotal = 0;
        private String documentNo = "";
        private String processMessage = "";
        private String errorMessage = "";
        
        // Getters and Setters
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public boolean isDuplicate() { return duplicate; }  // 新增：isDuplicate()方法
        public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
        
        public int getLinesCreated() { return linesCreated; }
        public void setLinesCreated(int linesCreated) { this.linesCreated = linesCreated; }        
       
        public void setLinesTotal(int linesTotal) { this.linesTotal = linesTotal; }
        
        public String getDocumentNo() { return documentNo; }
        public void setDocumentNo(String documentNo) { this.documentNo = documentNo; }        
     
        public String getProcessMessage() { return processMessage; }
        public void setProcessMessage(String processMessage) { this.processMessage = processMessage; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }     
    }
}
