package com.hoifu.info;  
  
import java.math.BigDecimal;  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;  
import java.sql.Timestamp;  
import java.util.Properties;  
import java.util.ArrayList;  
  
import org.adempiere.webui.info.InfoWindow;  
import org.adempiere.webui.panel.InfoPanel;  
import org.compiere.model.GridField;  
import org.compiere.minigrid.ColumnInfo;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
  
/**  
 * 时序库存信息窗口 - 支持分组统计  
 */  
public class InfoInventoryWindow extends InfoWindow {  
      
    private static final long serialVersionUID = 1L;  
      
    // 查询参数  
    private Timestamp p_asOfDate = null;  
    private Integer p_m_product_category_id = null;  
    private Integer p_m_product_id = null;  
    private Integer p_m_attributesetinstance_id = null;  
    private Integer p_m_warehouse_id = null;  
    private Integer p_m_locator_id = null;  
    private String p_costingMethod = null;  
      
    public InfoInventoryWindow(int WindowNo, String tableName, String keyColumn,  
            String queryValue, boolean multipleSelection, String whereClause,  
            int AD_InfoWindow_ID, boolean lookup, GridField field, String predefinedContextVariables) {  
        super(WindowNo, tableName, keyColumn, queryValue, multipleSelection,  
                whereClause, AD_InfoWindow_ID, lookup, field, predefinedContextVariables);  
    }  
      
    @Override  
    protected String getSQLWhere() {  
        StringBuilder where = new StringBuilder();  
        where.append("a.isactive = 'Y'");  
          
        if (p_asOfDate != null) {  
            where.append(" AND a.movementdate <= ?");  
        }  
        if (p_m_product_category_id != null && p_m_product_category_id > 0) {  
            where.append(" AND pc.m_product_category_id = ?");  
        }  
        if (p_m_product_id != null && p_m_product_id > 0) {  
            where.append(" AND a.m_product_id = ?");  
        }  
        if (p_m_attributesetinstance_id != null && p_m_attributesetinstance_id > 0) {  
            where.append(" AND a.m_attributesetinstance_id = ?");  
        }  
        if (p_m_warehouse_id != null && p_m_warehouse_id > 0) {  
            where.append(" AND w.m_warehouse_id = ?");  
        }  
        if (p_m_locator_id != null && p_m_locator_id > 0) {  
            where.append(" AND l.m_locator_id = ?");  
        }  
        if (p_costingMethod != null && p_costingMethod.trim().length() > 0) {  
            where.append(" AND ce.costingmethod = ?");  
        }  
          
        return where.toString();  
    }  
      
    @Override  
    protected void setParameters(PreparedStatement pstmt, boolean forCount) throws SQLException {  
        int index = 1;  
          
        if (p_asOfDate != null) {  
            pstmt.setTimestamp(index++, p_asOfDate);  
        }  
        if (p_m_product_category_id != null && p_m_product_category_id > 0) {  
            pstmt.setInt(index++, p_m_product_category_id);  
        }  
        if (p_m_product_id != null && p_m_product_id > 0) {  
            pstmt.setInt(index++, p_m_product_id);  
        }  
        if (p_m_attributesetinstance_id != null && p_m_attributesetinstance_id > 0) {  
            pstmt.setInt(index++, p_m_attributesetinstance_id);  
        }  
        if (p_m_warehouse_id != null && p_m_warehouse_id > 0) {  
            pstmt.setInt(index++, p_m_warehouse_id);  
        }  
        if (p_m_locator_id != null && p_m_locator_id > 0) {  
            pstmt.setInt(index++, p_m_locator_id);  
        }  
        if (p_costingMethod != null && p_costingMethod.trim().length() > 0) {  
            pstmt.setString(index++, p_costingMethod);  
        }  
    }  
      
    @Override  
    protected void prepareTable() {  
        try {  
            // 构建完整的SQL  
            String sql = buildCustomSQL();  
              
            // 获取列布局并初始化 p_layout  
            ColumnInfo[] layout = getColumnInfo();  
            p_layout = layout; // 这行很重要！  
              
            contentPanel.prepareTable(layout, "", "", false, "m_transaction");  
              
            // 执行查询并加载数据  
            PreparedStatement pstmt = DB.prepareStatement(sql, null);  
            setParameters(pstmt, false);  
            ResultSet rs = pstmt.executeQuery();  
              
            contentPanel.loadTable(rs);  
            contentPanel.autoSize();  
              
            rs.close();  
            pstmt.close();  
              
        } catch (Exception e) {  
            log.severe("Error in prepareTable: " + e.getMessage());  
        }  
    }  
      
    @Override  
    protected boolean testCount(boolean promptError) {  
        // 直接返回true，跳过计数检查  
        return true;  
    }  
      
    private String buildCustomSQL() {  
        StringBuilder sql = new StringBuilder();  
        sql.append("SELECT ");  
        sql.append("p.value AS productvalue, ");  
        sql.append("p.name AS productname, ");  
        sql.append("l.value AS locatorvalue, ");  
        sql.append("w.name AS warehousename, ");  
        sql.append("COALESCE(asi.serno, '') AS serno, ");  
        sql.append("COALESCE(asi.lot, '') AS lot, ");  
        sql.append("SUM(a.movementqty) AS quantity, ");  
        sql.append("MAX(a.movementdate) AS movementdate, ");  
        sql.append("MAX(ch.newcostprice) AS costprice, ");  
        sql.append("MAX(ch.m_costhistory_id) AS costhistory_id, ");  
        sql.append("ce.costingmethod ");  
          
        sql.append("FROM m_transaction a ");  
        sql.append("JOIN m_product p ON a.m_product_id = p.m_product_id ");  
        sql.append("JOIN m_product_category pc ON p.m_product_category_id = pc.m_product_category_id ");  
        sql.append("LEFT JOIN m_attributesetinstance asi ON a.m_attributesetinstance_id = asi.m_attributesetinstance_id ");  
        sql.append("LEFT JOIN m_locator l ON a.m_locator_id = l.m_locator_id ");  
        sql.append("JOIN m_warehouse w ON l.m_warehouse_id = w.m_warehouse_id ");  
        sql.append("LEFT JOIN m_costhistory ch ON a.m_product_id = ch.m_product_id ");  
        sql.append("    AND (a.m_attributesetinstance_id = ch.m_attributesetinstance_id OR ch.m_attributesetinstance_id = 0) ");  
        sql.append("    AND ch.dateacct <= a.movementdate AND ch.isactive = 'Y' ");  
        sql.append("LEFT JOIN m_costelement ce ON ch.m_costelement_id = ce.m_costelement_id AND ce.costelementtype = 'M' ");  
          
        sql.append("WHERE ").append(getSQLWhere());  
          
        // 添加分组子句  
        sql.append(" GROUP BY p.value, p.name, l.value, w.name, ");  
        sql.append("COALESCE(asi.serno, ''), COALESCE(asi.lot, ''), ce.costingmethod ");  
          
        // 添加排序  
        sql.append(" ORDER BY p.value, l.value DESC");  
          
        return sql.toString();  
    }  
      
    private ColumnInfo[] getColumnInfo() {  
        ColumnInfo[] layout = new ColumnInfo[]{  
            new ColumnInfo("物料", "productvalue", String.class),  
            new ColumnInfo("物料名称", "productname", String.class),  
            new ColumnInfo("实例属性", "serno", String.class),  
            new ColumnInfo("批次", "lot", String.class),  
            new ColumnInfo("时间点", "movementdate", Timestamp.class),  
            new ColumnInfo("数量", "quantity", BigDecimal.class),  
            new ColumnInfo("单价", "costprice", BigDecimal.class),  
            new ColumnInfo("金额", "costprice * quantity", BigDecimal.class),  
            new ColumnInfo("计价方式", "costingmethod", String.class),  
            new ColumnInfo("库位", "locatorvalue", String.class),  
            new ColumnInfo("仓库", "warehousename", String.class),  
            new ColumnInfo("事务编号", "a.m_transaction_id", Integer.class),  
            new ColumnInfo("成本历史编号", "costhistory_id", Integer.class)  
        };  
        return layout;  
    }  
      
    // Setter 方法  
    public void setAsOfDate(Timestamp asOfDate) {  
        this.p_asOfDate = asOfDate;  
    }  
      
    public void setM_Product_Category_ID(int m_product_category_id) {  
        this.p_m_product_category_id = m_product_category_id;  
    }  
      
    public void setM_Product_ID(int m_product_id) {  
        this.p_m_product_id = m_product_id;  
    }  
      
    public void setM_AttributeSetInstance_ID(int m_attributesetinstance_id) {  
        this.p_m_attributesetinstance_id = m_attributesetinstance_id;  
    }  
      
    public void setM_Warehouse_ID(int m_warehouse_id) {  
        this.p_m_warehouse_id = m_warehouse_id;  
    }  
      
    public void setM_Locator_ID(int m_locator_id) {  
        this.p_m_locator_id = m_locator_id;  
    }  
      
    public void setCostingMethod(String costingMethod) {  
        this.p_costingMethod = costingMethod;  
    }  
}