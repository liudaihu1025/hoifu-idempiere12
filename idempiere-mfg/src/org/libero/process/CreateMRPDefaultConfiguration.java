package org.libero.process;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import java.util.HashSet;  
import java.util.Set;  
  
import org.compiere.model.MBPartner;  
import org.compiere.model.MPriceList;  
import org.compiere.model.MPriceListVersion;  
import org.compiere.model.MProduct;  
import org.compiere.model.MProductPO;  
import org.compiere.model.MProductPrice;  
import org.compiere.model.MReplenish;  
import org.compiere.model.Query;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import org.eevolution.model.MPPProductBOM;  
import org.eevolution.model.MPPProductBOMLine;  
import org.eevolution.model.X_PP_Product_Planning;  
  
/**  
 * 生成产品的MRP默认配置流程  
 * 为指定产品及其BOM明细中的物料自动创建：  
 * - 物料计划 (PP_Product_Planning)  
 * - 补货记录 (M_Replenish)  
 * - 采购供应商 (M_Product_PO)  
 * - 产品价格 (M_ProductPrice)  
 */  
public class CreateMRPDefaultConfiguration extends SvrProcess {  
  
    private int p_AD_Workflow_ID = 0;  
    private int AD_Org_ID = 0;  
    private int AD_User_ID = 0;  
    private int S_Resource_ID = 0;  
    private int M_Warehouse_ID = 0;  
  
    /** 默认供应商ID */  
    private int defaultBPartnerID = 0;  
    /** 采购价格表版本ID */  
    private int purchasePLV_ID = 0;  
  
    /** 已处理的产品ID集合，防止循环递归 */  
    private Set<Integer> processedProducts = new HashSet<>();  
  
    @Override  
    protected void prepare() {  
        ProcessInfoParameter[] para = getParameter();  
        for (int i = 0; i < para.length; i++) {  
            String name = para[i].getParameterName();  
            if (para[i].getParameter() == null)  
                ;  
            else if (name.equals("AD_Workflow_ID"))  
                p_AD_Workflow_ID = para[i].getParameterAsInt();  
            else  
                log.warning("未知参数: " + name);  
        }  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        int M_Product_ID = getRecord_ID();  
        if (M_Product_ID == 0) {  
            throw new Exception("请先选择产品");  
        }  
  
        MProduct product = new MProduct(getCtx(), M_Product_ID, get_TrxName());  
        
        if (!product.isVerified()) {  
            throw new Exception("产品 [" + product.getValue() + "] 的BOM尚未通过校验，请先执行BOM校验流程");  
        }  
  
        AD_Org_ID = Env.getAD_Org_ID(getCtx());  
        AD_User_ID = Env.getAD_User_ID(getCtx());  
  
        // 预先获取资源和仓库信息  
        S_Resource_ID = getResourceByValue("P10");  
        if (S_Resource_ID == 0) {  
            throw new Exception("请先维护'基地一车间'的厂房【P10】（即10号厂房）");  
        }  
        M_Warehouse_ID = getWarehouseFromResource(S_Resource_ID);  
        if (M_Warehouse_ID == 0) {  
            throw new Exception("未找到资源对应的仓库");  
        }  
  
        // 预先获取默认供应商  
        defaultBPartnerID = getDefaultBPartner();  
        if (defaultBPartnerID == 0) {  
            throw new Exception("未找到名为'默认业务伙伴'的供应商，请先维护");  
        }  
  
        // 预先获取采购价格表版本  
        purchasePLV_ID = getPurchasePriceListVersionID();  
        if (purchasePLV_ID == 0) {  
            throw new Exception("未找到有效的采购价格表版本，请检查'采购价格表'是否存在且有有效版本");  
        }  
  
        log.info("开始为产品 [" + product.getValue() + "] 及其BOM物料创建MRP默认配置");  
  
        // 1. 为主产品创建PP_Product_Planning  
        if (!existsProductPlanning(M_Product_ID, AD_Org_ID)) {  
            createProductPlanning(M_Product_ID, AD_Org_ID, AD_User_ID, true);  
            addLog("主产品 [" + product.getValue() + "] 物料计划：已创建");  
        } else {  
            addLog("主产品 [" + product.getValue() + "] 物料计划：已存在，跳过");  
        }  
  
        // 2. 为主产品创建M_Replenish  
        if (!existsReplenish(M_Product_ID, AD_Org_ID)) {  
            createReplenish(M_Product_ID, AD_Org_ID);  
            addLog("主产品 [" + product.getValue() + "] 补货记录：已创建");  
        } else {  
            addLog("主产品 [" + product.getValue() + "] 补货记录：已存在，跳过");  
        }  
  
        // 3. 递归为BOM明细中的物料创建记录  
        processedProducts.add(M_Product_ID);  
        processBOMComponents(M_Product_ID);  
  
        return "MRP默认配置处理完成";  
    }  
  
    /**  
     * 递归处理BOM明细中的物料  
     * 为每个组件创建PP_Product_Planning、M_Product_PO、M_ProductPrice  
     * 如果组件本身也有BOM（半成品），则继续递归处理其BOM明细  
     */  
    private void processBOMComponents(int M_Product_ID) throws Exception {  
        MProduct product = new MProduct(getCtx(), M_Product_ID, get_TrxName());  
        MPPProductBOM bom = MPPProductBOM.getDefault(product, get_TrxName());  
        if (bom == null) {  
            log.info("产品 [" + product.getValue() + "] 没有默认BOM，跳过BOM明细处理");  
            return;  
        }  
  
        MPPProductBOMLine[] bomLines = bom.getLines();  
        for (MPPProductBOMLine bomLine : bomLines) {  
            if (!bomLine.isActive()) {  
                continue;  
            }  
  
            int componentProductID = bomLine.getM_Product_ID();  
  
            // 防止循环递归  
            if (processedProducts.contains(componentProductID)) {  
                log.info("产品ID " + componentProductID + " 已处理过，跳过（防止循环）");  
                continue;  
            }  
            processedProducts.add(componentProductID);  
  
            MProduct comp = new MProduct(getCtx(), componentProductID, get_TrxName());  
            String compValue = comp.getValue();  
  
            // 为组件创建PP_Product_Planning  
            if (!existsProductPlanning(componentProductID, AD_Org_ID)) {  
                createProductPlanning(componentProductID, AD_Org_ID, AD_User_ID, false);  
                addLog("组件 [" + compValue + "] 物料计划：已创建");  
            } else {  
                addLog("组件 [" + compValue + "] 物料计划：已存在，跳过");  
            }  
  
            // 为组件创建M_Product_PO  
            if (!existsProductPO(componentProductID, AD_Org_ID)) {  
                createProductPO(componentProductID, AD_Org_ID, compValue);  
                addLog("组件 [" + compValue + "] 采购供应商：已创建");  
            } else {  
                addLog("组件 [" + compValue + "] 采购供应商：已存在，跳过");  
            }  
  
            // 为组件创建M_ProductPrice  
            if (!existsProductPrice(componentProductID)) {  
                createProductPrice(componentProductID);  
                addLog("组件 [" + compValue + "] 产品价格：已创建");  
            } else {  
                addLog("组件 [" + compValue + "] 产品价格：已存在，跳过");  
            }  
  
            // 如果组件本身也是BOM产品（半成品），递归处理其BOM明细  
            if (comp.isBOM()) {  
                log.info("组件 [" + compValue + "] 是半成品（BOM产品），继续递归处理其BOM明细");  
                processBOMComponents(componentProductID);  
            }  
        }  
    }  
  
    /**  
     * 创建产品物料计划记录  
     * @param isMainProduct 是否为主产品（主产品需要设置AD_Workflow_ID）  
     */  
    private void createProductPlanning(int M_Product_ID, int AD_Org_ID, int AD_User_ID, boolean isMainProduct) throws Exception {  
        int PP_Product_BOM_ID = getDefaultBOM(M_Product_ID);  
  
        if (isMainProduct && PP_Product_BOM_ID == 0) {  
            throw new Exception("未找到主产品的默认BOM（BOMType='A', BOMUse='A'）");  
        }  
  
        X_PP_Product_Planning planning = new X_PP_Product_Planning(getCtx(), 0, get_TrxName());  
        planning.setAD_Org_ID(AD_Org_ID);  
        planning.setM_Product_ID(M_Product_ID);  
        planning.setPlanner_ID(AD_User_ID);  
        planning.setOrder_Policy("LFL");  
        planning.setS_Resource_ID(S_Resource_ID);  
        planning.setM_Warehouse_ID(M_Warehouse_ID);  
        planning.setIsRequiredMRP(true);  
        planning.setIsCreatePlan(true);  
        planning.setIsMPS(true);  
        planning.setIsPhantom(false);  
  
        if (PP_Product_BOM_ID > 0) {  
            planning.setPP_Product_BOM_ID(PP_Product_BOM_ID);  
        }  
  
        if (isMainProduct && p_AD_Workflow_ID > 0) {  
            planning.setAD_Workflow_ID(p_AD_Workflow_ID);  
        }  
  
        planning.saveEx();  
    }  
  
    /**  
     * 创建产品补货记录  
     */  
    private void createReplenish(int M_Product_ID, int AD_Org_ID) throws Exception {  
        MReplenish replenish = new MReplenish(getCtx(), 0, get_TrxName());  
        replenish.setAD_Org_ID(AD_Org_ID);  
        replenish.setM_Product_ID(M_Product_ID);  
        replenish.setM_Warehouse_ID(M_Warehouse_ID);  
        replenish.setReplenishType("1");  
        replenish.saveEx();  
    }  
  
    /**  
     * 创建产品采购供应商记录  
     * M_Product_PO.C_BPartner_ID = '默认业务伙伴'  
     * M_Product_PO.VendorProductNo = M_Product.Value  
     */  
    private void createProductPO(int M_Product_ID, int AD_Org_ID, String productValue) throws Exception {  
        MProductPO productPO = new MProductPO(getCtx(), 0, get_TrxName());  
        productPO.setAD_Org_ID(AD_Org_ID);  
        productPO.setM_Product_ID(M_Product_ID);  
        productPO.setC_BPartner_ID(defaultBPartnerID);  
        productPO.setVendorProductNo(productValue);  
        productPO.setIsCurrentVendor(true);  
        productPO.saveEx();  
    }  
  
    /**  
     * 创建产品价格记录  
     * M_ProductPrice.PriceList = 0, PriceStd = 0, PriceLimit = 0  
     */  
    private void createProductPrice(int M_Product_ID) throws Exception {  
        MProductPrice pp = new MProductPrice(getCtx(), 0, get_TrxName());  
        pp.setM_PriceList_Version_ID(purchasePLV_ID);  
        pp.setM_Product_ID(M_Product_ID);  
        pp.setPriceList(BigDecimal.ZERO);  
        pp.setPriceStd(BigDecimal.ZERO);  
        pp.setPriceLimit(BigDecimal.ZERO);  
        pp.saveEx();  
    }  
  
    // ==================== 存在性检查方法 ====================  
  
    private boolean existsProductPlanning(int M_Product_ID, int AD_Org_ID) {  
        X_PP_Product_Planning existing = new Query(getCtx(), X_PP_Product_Planning.Table_Name,  
                "M_Product_ID = ? AND AD_Org_ID = ?", get_TrxName())  
            .setParameters(M_Product_ID, AD_Org_ID)  
            .first();  
        return existing != null;  
    }  
  
    private boolean existsReplenish(int M_Product_ID, int AD_Org_ID) {  
        MReplenish existing = new Query(getCtx(), MReplenish.Table_Name,  
                "M_Product_ID = ? AND AD_Org_ID = ?", get_TrxName())  
            .setParameters(M_Product_ID, AD_Org_ID)  
            .first();  
        return existing != null;  
    }  
  
    /**  
     * 检查产品采购供应商记录是否已存在（按产品+组织）  
     */  
    private boolean existsProductPO(int M_Product_ID, int AD_Org_ID) {  
        MProductPO existing = new Query(getCtx(), MProductPO.Table_Name,  
                "M_Product_ID = ? AND AD_Org_ID = ?", get_TrxName())  
            .setParameters(M_Product_ID, AD_Org_ID)  
            .first();  
        return existing != null;  
    }  
  
    /**  
     * 检查产品价格记录是否已存在（按产品+价格表版本）  
     */  
    private boolean existsProductPrice(int M_Product_ID) {  
        return MProductPrice.get(getCtx(), purchasePLV_ID, M_Product_ID, get_TrxName()) != null;  
    }  
  
    // ==================== 数据查询方法 ====================  
  
    private int getDefaultBOM(int M_Product_ID) {  
        String sql = "SELECT PP_Product_BOM_ID FROM PP_Product_BOM " +  
                     "WHERE M_Product_ID = ? AND BOMType = 'A' AND BOMUse = 'A' AND IsActive = 'Y'";  
        return DB.getSQLValue(get_TrxName(), sql, M_Product_ID);  
    }  
  
    private int getResourceByValue(String value) {  
        String sql = "SELECT S_Resource_ID FROM S_Resource " +  
                     "WHERE Value = ? AND IsActive = 'Y'";  
        return DB.getSQLValue(get_TrxName(), sql, value);  
    }  
  
    private int getWarehouseFromResource(int S_Resource_ID) {  
        String sql = "SELECT M_Warehouse_ID FROM S_Resource " +  
                     "WHERE S_Resource_ID = ?";  
        return DB.getSQLValue(get_TrxName(), sql, S_Resource_ID);  
    }  
  
    /**  
     * 获取名为'默认业务伙伴'的供应商ID  
     */  
    private int getDefaultBPartner() {  
        String sql = "SELECT C_BPartner_ID FROM C_BPartner " +  
                     "WHERE Name = '默认业务伙伴' AND IsVendor = 'Y' AND IsActive = 'Y'";  
        return DB.getSQLValue(get_TrxName(), sql);  
    }  
  
    /**  
     * 获取采购价格表中当前有效的最新版本ID  
     * 条件：M_PriceList.IsSOPriceList='N' AND M_PriceList.Name='采购价格表'  
     *       M_PriceList_Version.IsActive='Y' AND ValidFrom <= 当前日期  
     *       按ValidFrom DESC取第一条  
     */  
    private int getPurchasePriceListVersionID() {  
        String sql = "SELECT plv.M_PriceList_Version_ID " +  
                     "FROM M_PriceList_Version plv " +  
                     "INNER JOIN M_PriceList pl ON (plv.M_PriceList_ID = pl.M_PriceList_ID) " +  
                     "WHERE pl.IsSOPriceList = 'N' " +  
                     "AND pl.Name = '采购价格表' " +  
                     "AND pl.IsActive = 'Y' " +  
                     "AND plv.IsActive = 'Y' " +  
                     "AND TRUNC(plv.ValidFrom) <= TRUNC(getDate()) " +  
                     "ORDER BY plv.ValidFrom DESC";  
        return DB.getSQLValue(get_TrxName(), sql);  
    }  
}