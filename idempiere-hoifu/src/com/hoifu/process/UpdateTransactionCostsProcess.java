package com.hoifu.process;
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.Timestamp;  
import java.util.ArrayList;  
import java.util.List;  
  
import org.compiere.model.MAcctSchema;  
import org.compiere.model.MClientInfo;  
import org.compiere.model.MTransaction;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
@org.adempiere.base.annotation.Process  
public class UpdateTransactionCostsProcess extends SvrProcess {  
      
    private int p_M_Product_ID = 0;  
    private Timestamp p_DateFrom = null;  
    private Timestamp p_DateTo = null;  
    private int p_AD_Client_ID = 0;  // 新增：租户参数
    private int p_AD_Org_ID = 0;     // 新增：组织参数
      
    @Override  
    protected void prepare() {  
        ProcessInfoParameter[] para = getParameter();  
        for (ProcessInfoParameter p : para) {  
            String name = p.getParameterName();  
            if (p.getParameter() == null)  
                ;  
            else if (name.equals("M_Product_ID"))  
                p_M_Product_ID = p.getParameterAsInt();  
            else if (name.equals("DateFrom"))  
                p_DateFrom = (Timestamp) p.getParameter();  
            else if (name.equals("DateTo"))  
                p_DateTo = (Timestamp) p.getParameter();
        }  
    }  
      
    @Override  
    protected String doIt() throws Exception {  
        // 获取当前租户和组织ID（如果参数未指定，则使用当前上下文）
        int adClientId = p_AD_Client_ID > 0 ? p_AD_Client_ID : Env.getAD_Client_ID(getCtx());
        int adOrgId = p_AD_Org_ID > 0 ? p_AD_Org_ID : Env.getAD_Org_ID(getCtx());
        
        log.info("开始处理事务成本更新...");
        log.info("租户ID: " + adClientId + ", 组织ID: " + adOrgId + 
                 ", 产品ID: " + p_M_Product_ID + ", 开始日期: " + p_DateFrom + 
                 ", 结束日期: " + p_DateTo);
        
        // 构建SQL查询，添加组织和租户过滤
        StringBuilder sql = new StringBuilder(  
            "SELECT M_Transaction_ID FROM M_Transaction " +  
            "WHERE CurrentQtyOnHand IS NULL");
        
        // 添加租户和组织过滤
        sql.append(" AND AD_Client_ID = ?");
//        sql.append(" AND AD_Org_ID = ?");
        
        List<Object> params = new ArrayList<>();
        // 添加租户ID参数
        params.add(adClientId);
        // 添加组织ID参数
//        params.add(adOrgId);
          
        if (p_M_Product_ID > 0) {  
            sql.append(" AND M_Product_ID = ?");  
            params.add(p_M_Product_ID);  
        }  
        if (p_DateFrom != null) {  
            sql.append(" AND Created >= ?");  
            params.add(p_DateFrom);  
        }  
        if (p_DateTo != null) {  
            sql.append(" AND Created <= ?");  
            params.add(p_DateTo);  
        }  
          
        sql.append(" ORDER BY Created, M_Transaction_ID");  
          
        // 记录执行的SQL和参数
        log.info("执行SQL: " + sql.toString());
        log.info("参数列表: " + params.toString());
        
        // 修复：统计查询要去掉 ORDER BY 子句
        // 原来的代码会报错：column "m_transaction.Created" must appear in the GROUP BY clause
        String countSql = "SELECT COUNT(*) FROM M_Transaction WHERE " + 
                          sql.substring(sql.indexOf("WHERE") + 5, sql.indexOf("ORDER BY")).trim();
        
        log.info("统计SQL: " + countSql);
        int totalCount = DB.getSQLValueEx(get_TrxName(), countSql, params.toArray());
        log.info("符合条件的记录总数: " + totalCount);
        
        
        
        PreparedStatement pstmt = null;  
        ResultSet rs = null;  
        int updated = 0;  
        int total = 0;  
          
        try {  
            pstmt = DB.prepareStatement(sql.toString(), get_TrxName());  
            int index = 1;  
            for (Object param : params) {  
                pstmt.setObject(index++, param);  
            }  
            rs = pstmt.executeQuery();  
              
            while (rs.next()) {  
                total++;  
                int M_Transaction_ID = rs.getInt(1);  
                
                // 显示处理进度
                if (total % 100 == 0) {
                    log.info("正在处理第 " + total + " 条记录，M_Transaction_ID: " + M_Transaction_ID);
                }
                
                if (updateTransactionCosts(M_Transaction_ID)) {  
                    updated++;  
                }  
                  
                // 每500条记录提交一次（可根据实际情况调整）
                if (updated > 0 && updated % 500 == 0) {  
                    commitEx();  
                    log.info("已提交 " + updated + " 条记录");
                }  
            }  
        } finally {  
            DB.close(rs, pstmt);  
        }  
        
        // 提交剩余未提交的记录
        if (updated > 0 && updated % 500 != 0) {
            commitEx();
        }
        
        log.info("处理完成: 查询到记录数 " + total + ", 成功更新记录数 " + updated);
        return "处理完成: 总记录数 " + totalCount + ", 查询到记录 " + total + ", 成功更新记录 " + updated;  
    }
      
    /**  
     * 更新单条事务记录的成本和库存数据  
     */  
    private boolean updateTransactionCosts(int M_Transaction_ID) {  
        try {  
            log.fine("开始更新 M_Transaction_ID: " + M_Transaction_ID);
            
            MTransaction trx = new MTransaction(getCtx(), M_Transaction_ID, get_TrxName());  
            
            // 空指针检查：确保事务对象不为空
            if (trx == null || trx.get_ID() <= 0) {
                log.warning("事务记录不存在或无效: M_Transaction_ID=" + M_Transaction_ID);
                return false;
            }
            
            // 空指针检查：检查必要字段
            if (trx.getM_Product_ID() <= 0) {
                log.warning("产品ID为空: M_Transaction_ID=" + M_Transaction_ID);
            }
            if (trx.getM_Locator_ID() <= 0) {
                log.warning("库位ID为空: M_Transaction_ID=" + M_Transaction_ID);
            }
            
            log.fine("记录信息: Product_ID=" + trx.getM_Product_ID() + 
                    ", Locator_ID=" + trx.getM_Locator_ID() + 
                    ", Created=" + trx.getCreated());
              
            // 计算历史时间点的成本
            BigDecimal currentCost = calculateHistoricalCost(trx);  
            log.fine("计算得到的历史成本: " + currentCost);
            
            // 空指针检查：处理currentCost可能为null的情况
            if (currentCost == null) {
                log.warning("计算得到的历史成本为NULL: M_Transaction_ID=" + M_Transaction_ID);
            }
            // 修改点：如果找不到成本，设置为NULL而不是Env.ZERO
            trx.set_ValueOfColumn("CurrentCostPrice", currentCost);  
              
            // 计算历史时间点的库存数量
            BigDecimal currentQty = calculateHistoricalQty(trx);  
            log.fine("计算得到的历史库存: " + currentQty);
            
            // 空指针检查：处理currentQty可能为null的情况
            if (currentQty == null) {
                log.warning("计算得到的历史库存为NULL: M_Transaction_ID=" + M_Transaction_ID);
            }
            // 修改点：如果找不到库存数量，设置为NULL而不是Env.ZERO
            trx.set_ValueOfColumn("CurrentQtyOnHand", currentQty);  
              
            // 设置成本明细ID
            Integer costDetailID = getCurrentCostDetailID(trx);  // 修改：返回类型改为Integer
            log.fine("获取到的成本明细ID: " + costDetailID);
            
            // 空指针检查：处理costDetailID为null的情况
            if (costDetailID == null) {
                log.warning("获取到的成本明细ID为NULL: M_Transaction_ID=" + M_Transaction_ID);
            }
            // 修改点：如果找不到成本明细ID，设置为NULL而不是0
            trx.set_ValueOfColumn("M_CostDetail_ID", costDetailID);  
              
            // 保存记录
            trx.saveEx();
//            if (!saved) {
//                log.warning("保存失败 M_Transaction_ID: " + M_Transaction_ID);
//            }
           return true;  
        } catch (Exception e) {  
            log.warning("更新事务记录失败: M_Transaction_ID=" + M_Transaction_ID + ", 错误: " + e.getMessage());  
            return false;  
        }  
    }  
      
    /**  
     * 计算历史时间点的成本价格  
     */  
    private BigDecimal calculateHistoricalCost(MTransaction trx) {  
        // 空指针检查：确保trx不为空
        if (trx == null) {
            log.warning("calculateHistoricalCost: trx为null");
            return null;
        }
        
        MAcctSchema as = getAcctSchema(trx);  
        
        // 空指针检查：检查会计架构
        if (as == null) {
            log.warning("无法获取会计架构，产品ID: " + trx.getM_Product_ID());
            return null;
		}

		// 使用自定义SQL查询成本明细历史
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT ch.NewCostPrice FROM M_CostHistory ch ");
		sql.append("JOIN M_CostDetail cd ON (cd.M_CostDetail_ID = ch.M_CostDetail_ID AND cd.Processed='Y') ");
		sql.append("JOIN C_AcctSchema cas ON (cas.C_AcctSchema_ID = cd.C_AcctSchema_ID) ");
		sql.append("JOIN M_CostElement ce ON (ce.M_CostElement_ID = ch.M_CostElement_ID) ");
		sql.append("WHERE ch.AD_Client_ID=? AND ch.AD_Org_ID=? ");
		sql.append("AND ch.M_Product_ID=? AND (ch.M_AttributeSetInstance_ID=? OR ch.M_AttributeSetInstance_ID=0) ");
		sql.append("AND ch.M_CostType_ID=? AND cd.C_AcctSchema_ID=? ");
		sql.append("AND (ce.CostingMethod IS NULL OR ce.CostingMethod=cas.CostingMethod) ");
		sql.append("AND ce.CostElementType = 'M' AND ch.Created <= ? ");
		sql.append("ORDER BY ch.Created DESC, ch.M_CostHistory_ID DESC");

		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			pstmt = DB.prepareStatement(sql.toString(), get_TrxName());
			pstmt.setInt(1, trx.getAD_Client_ID());
			pstmt.setInt(2, trx.getAD_Org_ID());
			pstmt.setInt(3, trx.getM_Product_ID());
			pstmt.setInt(4, trx.getM_AttributeSetInstance_ID());
			pstmt.setInt(5, as.getM_CostType_ID());
			pstmt.setInt(6, as.getC_AcctSchema_ID());
			pstmt.setTimestamp(7, trx.getCreated());

			rs = pstmt.executeQuery();
			if (rs.next()) {
				BigDecimal cost = rs.getBigDecimal(1);
				if (!rs.wasNull()) {
					log.fine("从成本历史找到成本: " + cost + " for Product_ID=" + trx.getM_Product_ID());
					return cost;
				}
			}
		} catch (Exception e) {
			log.warning("查询历史成本失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}

		return null;
    }  
      
    /**  
     * 获取成本明细ID  
     */  
    private Integer getCurrentCostDetailID(MTransaction trx) {  // 修改：返回类型改为Integer
        // 空指针检查：确保trx不为空
        if (trx == null) {
            log.warning("getCurrentCostDetailID: trx为null");
            return null;
        }
        
        MAcctSchema as = getAcctSchema(trx);  
        
        // 空指针检查：检查会计架构
        if (as == null) {
            log.warning("无法获取会计架构，产品ID: " + trx.getM_Product_ID());
            return null;
        }
          
        StringBuilder sql = new StringBuilder();  
		sql.append("SELECT ch.M_CostDetail_ID FROM M_CostHistory ch ");
		sql.append("JOIN M_CostDetail cd ON (cd.M_CostDetail_ID = ch.M_CostDetail_ID AND cd.Processed='Y') ");
		sql.append("JOIN C_AcctSchema cas ON (cas.C_AcctSchema_ID = cd.C_AcctSchema_ID) ");
		sql.append("JOIN M_CostElement ce ON (ce.M_CostElement_ID = ch.M_CostElement_ID) ");
		sql.append("WHERE ch.AD_Client_ID=? AND ch.AD_Org_ID=? ");
		sql.append("AND ch.M_Product_ID=? AND (ch.M_AttributeSetInstance_ID=? OR ch.M_AttributeSetInstance_ID=0) ");
		sql.append("AND ch.M_CostType_ID=? AND cd.C_AcctSchema_ID=? ");
		sql.append("AND (ce.CostingMethod IS NULL OR ce.CostingMethod=cas.CostingMethod) ");
		sql.append("AND ce.CostElementType = 'M' AND ch.Created <= ? ");
		sql.append("ORDER BY ch.Created DESC, ch.M_CostHistory_ID DESC LIMIT 1");
          
        try {
            // 获取参数值
            int productId = trx.getM_Product_ID();
            int asiId = trx.getM_AttributeSetInstance_ID();
            int acctSchemaId = as.getC_AcctSchema_ID();
            Timestamp created = trx.getCreated();
            
            // 空指针检查：检查必要参数
            if (created == null) {
                log.warning("Created日期为空，产品ID: " + productId);
                return null;
            }
            
            // 修改点：使用getSQLValueEx，查询无结果时返回null
            Integer result = DB.getSQLValueEx(get_TrxName(), sql.toString(),  
                trx.getAD_Client_ID(),
                trx.getAD_Org_ID(),
                productId,
                asiId, 
                as.getM_CostType_ID(), 
                acctSchemaId, 
                created);
            
            // 空指针检查：DB.getSQLValueEx在查询无结果时返回null
            if (result == null || result <= 0) {
                log.fine("未找到成本明细ID，产品ID: " + productId + ", ASI ID: " + asiId);
                return null;  // 修改点：找不到记录时返回null
            }
            
            return result;
        } catch (Exception e) {
            log.warning("获取成本明细ID失败: " + e.getMessage());
            return null;  // 修改点：异常时返回null
        }
    }  
      
    /**  
     * 计算历史时间点的库存数量  
     */  
    private BigDecimal calculateHistoricalQty(MTransaction trx) {  
        // 空指针检查：确保trx不为空
        if (trx == null) {
            log.warning("calculateHistoricalQty: trx为null");
            return null;
        }
        
        StringBuilder sql = new StringBuilder();  
        sql.append("SELECT SUM(MovementQty) FROM M_Transaction ");  // 修改点：去掉COALESCE，允许返回NULL
        sql.append("WHERE M_Product_ID=? AND M_Locator_ID=? ");  
//        sql.append("AND M_AttributeSetInstance_ID=? ");  
//        sql.append("AND AD_Client_ID=? AND AD_Org_ID=? ");  // 添加租户和组织过滤
        sql.append("AND Created <= ? ");  
          
        try {
            // 获取参数值
            int productId = trx.getM_Product_ID();
            int locatorId = trx.getM_Locator_ID();
            Timestamp created = trx.getCreated();
            
            // 空指针检查：检查必要参数
            if (created == null) {
                log.warning("Created日期为空，产品ID: " + productId + ", 库位ID: " + locatorId);
                return null;
            }
            
            // 修改点：使用getSQLValueBDEx，查询无结果时返回null
            BigDecimal qty = DB.getSQLValueBDEx(get_TrxName(), sql.toString(),  
                productId,  
                locatorId,  
//                trx.getM_AttributeSetInstance_ID(),  
//                trx.getAD_Client_ID(),  // 添加租户参数
//                trx.getAD_Org_ID(),     // 添加组织参数
                created);
            
            // 修改点：查询可能返回null，这符合要求
            if (qty == null) {
                log.fine("计算历史库存返回NULL（可能没有历史记录），产品ID: " + productId + ", 库位ID: " + locatorId);
                return null;  // 修改点：找不到记录时返回null
            }
            
            log.fine("计算历史库存: Product_ID=" + productId + 
                    ", Locator_ID=" + locatorId + 
                    ", Qty=" + qty);
            return qty;
        } catch (Exception e) {
            log.warning("计算历史库存失败: " + e.getMessage());
            return null;  // 修改点：异常时返回null
        }
    }  
      
    /**  
     * 获取会计架构  
     */  
    private MAcctSchema getAcctSchema(MTransaction trx) {  
        // 空指针检查：确保trx不为空
        if (trx == null) {
            log.warning("getAcctSchema: trx为null");
            return null;
        }
        
        try {
            MClientInfo info = MClientInfo.get(trx.getCtx(), trx.getAD_Client_ID(), trx.get_TrxName());  
            
            // 空指针检查：检查客户信息
            if (info == null) {
                log.warning("未找到客户信息，Client_ID: " + trx.getAD_Client_ID());
                return null;
            }
            
            return info.getMAcctSchema1();  
        } catch (Exception e) {
            log.warning("获取会计架构失败: " + e.getMessage());
            return null;
        }
    }  
}