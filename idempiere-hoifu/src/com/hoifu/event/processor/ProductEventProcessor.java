package com.hoifu.event.processor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Set;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.compiere.util.TimeUtil;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.adempiere.model.GenericPO;  
import org.compiere.model.MOrg;  
import org.compiere.model.MRoleOrgAccess;  
import org.compiere.util.Env;
import com.hoifu.model.MValueChangeLog;

import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.sql.SQLException;

public class ProductEventProcessor implements IEventProcessor {

	private static final String COLUMNNAME_Category_ID_L1 = "M_Product_Category_ID_L1";
	private static final String COLUMNNAME_CartonMaterial_ID = "CartonMaterial_ID";
	private static final String COLUMNNAME_Lengbie = "Lengbie";
	private static final String COLUMNNAME_KeyMat = "KeyMat";
	private static final String TABLE_M_Product_Org = "M_Product_Org";
	private static final String COLUMNNAME_ActiveOrg = "ActiveOrg";
	private static final String COLUMNNAME_Category_ID_L2 = "M_Product_Category_ID_L2";

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MProduct;
	}

	@Override
	public void process(PO po, String topic) {
		MProduct product = (MProduct) po;
		
		// 自动生成物料编码（新增前）  
	    autoGenerateProductValue(product, topic);  

		//物料名称校验
		ProductNameCheck(product, topic);

		//自动创建或更新BOM数据
		autoCreateBOM(product, topic);

		// 记录 Value 字段变更日志  
	    logValueChange(product, topic);  
	    
	 // 在 process() 方法末尾添加调用  
	    autoCreateProductOrg(product, topic);
	    
	}

	/**  
	 * 物料新建后，若 ProductType='I' 且 AD_Org_ID=0，  
	 * 则为当前角色的所有组织（排除0）各创建一条 M_Product_Org 记录。  
	 * M_Product_Org.AD_Org_ID 固定为 0，ActiveOrg 存 AD_Org.Value。  
	 */  
	void autoCreateProductOrg(MProduct product, String topic) {  
	    if (!IEventTopics.PO_AFTER_NEW.equals(topic)) {  
	        return;  
	    }  
	    if (!"I".equals(product.getProductType())) {  
	        return;  
	    }  
	    if (product.getAD_Org_ID() != 0) {  
	        return;  
	    }  
	  
	    // 获取当前操作人角色的所有组织访问记录（排除 AD_Org_ID=0）  
	    int roleId = Env.getAD_Role_ID(Env.getCtx());  
	    MRoleOrgAccess[] orgAccesses = MRoleOrgAccess.getOfRole(product.getCtx(), roleId);  
	  
	    for (MRoleOrgAccess oa : orgAccesses) {  
	        int orgId = oa.getAD_Org_ID();  
	        if (orgId == 0) {  
	            continue;  
	        }  
	        MOrg org = MOrg.get(product.getCtx(), orgId);  
	        if (org == null) {  
	            continue;  
	        }  
	        String orgValue = org.getValue(); // AD_Org.Value  
	  
	        GenericPO productOrg = new GenericPO(TABLE_M_Product_Org, product.getCtx(), 0, product.get_TrxName());  
	        productOrg.setAD_Org_ID(0);  // 固定为 0  
	        productOrg.set_ValueOfColumn(MProduct.COLUMNNAME_M_Product_ID, product.getM_Product_ID());  
	        productOrg.set_ValueOfColumn(COLUMNNAME_ActiveOrg, orgValue);  
	        productOrg.saveEx();  
	    }  
	}
	
	/**  
	 * 新增 M_Product 时，根据 M_Product_Category_ID_L2 对应的 Category.Value 作为前缀，  
	 * 自动生成 6 位流水号写入 M_Product.Value。  
	 * 流水号 = 当前库中该前缀下最大数字 + 1，不足 6 位左补零。  
	 * 触发时机：PO_BEFORE_NEW（保存前写入，避免 Value 为空导致唯一性校验失败）。  
	 */  
	void autoGenerateProductValue(MProduct product, String topic) {  
	    if (!IEventTopics.PO_BEFORE_NEW.equals(topic)) {  
	        return;  
	    }  
	    
	    if (!(product.getValue() == null || product.getValue().trim().isEmpty())) {  
	        return;  
	    }
	    //类型为物品
	    if (!"I".equals(product.getProductType())) {
	    	 return;  
	    }
	    
	    // 获取 M_Product_Category_ID_L2  
	    Integer catL2Id = (Integer) product.get_Value(COLUMNNAME_Category_ID_L2);  
	    if (catL2Id == null || catL2Id <= 0) {  
	        return;  
	    }
	  
	    // 查询对应的 Category.Value 作为前缀  
	    String prefix = DB.getSQLValueString(product.get_TrxName(),  
	            "SELECT Value FROM M_Product_Category WHERE M_Product_Category_ID=?", catL2Id);  
	    if (prefix == null || prefix.trim().isEmpty()) {  
	        return;  
	    }  
	  
	    // 遍历所有以 prefix 开头、总长度 = prefix.length()+6 的 Value，找最大流水号  
	    int maxSeq = 0;  
	    String sql = "SELECT Value FROM M_Product WHERE Value LIKE ? AND LENGTH(Value) = ?";  
	    PreparedStatement pstmt = null;  
	    ResultSet rs = null;  
	    try {  
	        pstmt = DB.prepareStatement(sql, product.get_TrxName());  
	        pstmt.setString(1, prefix + "%");  
	        pstmt.setInt(2, prefix.length() + 6);  
	        rs = pstmt.executeQuery();  
	        while (rs.next()) {  
	            String val = rs.getString(1);  
	            String seqPart = val.substring(prefix.length());  
	            try {  
	                int seq = Integer.parseInt(seqPart);  
	                if (seq > maxSeq) {  
	                    maxSeq = seq;  
	                }  
	            } catch (NumberFormatException e) {  
	                // 后缀非纯数字，跳过  
	            }  
	        }  
	    } catch (SQLException e) {  
	        throw new AdempiereException(e);  
	    } finally {  
	        DB.close(rs, pstmt);  
	        rs = null;  
	        pstmt = null;  
	    }  
	  
	    int nextSeq = maxSeq + 1;  
	    String newValue = prefix + String.format("%06d", nextSeq);  
	    product.setValue(newValue);  
	}
	
	/**  
	 * 在 M_Product.Value 字段变更后，写入 AD_ValueChangeLog。  
	 * 触发时机：PO_AFTER_CHANGE（保存成功后，get_ValueOld 仍有效）。  
	 */  
	void logValueChange(MProduct product, String topic) {  

	    if (!IEventTopics.PO_AFTER_CHANGE.equals(topic) || !product.is_ValueChanged(MProduct.COLUMNNAME_Value)) {  
	        return;
	    }
	  
	    String oldValue = (String) product.get_ValueOld(MProduct.COLUMNNAME_Value);  
	    String newValue = product.getValue();
	  
	    MValueChangeLog log = new MValueChangeLog(product.getCtx(), 0, product.get_TrxName());  
	    log.setAD_Org_ID(product.getAD_Org_ID()); 
	    log.setRecord_ID(product.getM_Product_ID());  
	    log.setTableName(MProduct.Table_Name);  
	    log.setColumnName(MProduct.COLUMNNAME_Value);  
	    log.setOldValue(oldValue);  
	    log.setNewValue(newValue);  
	    log.saveEx();  
	}
	
	/**
	 * 若满足条件（IsBOM=Y 且 M_Product_Category_ID_L1 的 Value='ZX'）， 且尚无默认 BOM，则自动创建一条
	 * PP_Product_BOM。
	 * @param topic 
	 */
	void autoCreateBOM(MProduct p, String topic) {
		
		if (!p.isBOM()) {
			return;
		}
		if (!IEventTopics.PO_AFTER_NEW.equals(topic) && !(IEventTopics.PO_AFTER_CHANGE.equals(topic) 
				&& (p.is_ValueChanged(COLUMNNAME_Category_ID_L1) || p.is_ValueChanged(COLUMNNAME_CartonMaterial_ID)))) {
			return;
		}

	    // 校验组织  
//	    String orgValue = DB.getSQLValueString(p.get_TrxName(),  
//	            "SELECT Value FROM AD_Org WHERE AD_Org_ID=?", p.getAD_Org_ID());  
//	    if (!"0213".equals(orgValue)) return;  
	    
		// 检查 M_Product_Category_ID_L1 对应的 Category Value
		String catValue = DB.getSQLValueStringEx(p.get_TrxName(),
				"SELECT Value FROM M_Product_Category WHERE M_Product_Category_ID=?",
				p.get_Value(COLUMNNAME_Category_ID_L1));

		if (!"ZX".equals(catValue))
			return;

		Integer cartonMaterialId = (Integer) p.get_Value(COLUMNNAME_CartonMaterial_ID);  
		// 检查是否已存在默认 BOM，避免重复创建
		MPPProductBOM bom = MPPProductBOM.getDefault(p, p.get_TrxName());  
	    if (bom == null) {  
	        bom = new MPPProductBOM(p.getCtx(), 0, p.get_TrxName());  
	        bom.setAD_Org_ID(p.getAD_Org_ID());  
	        bom.setM_Product_ID(p.getM_Product_ID());
	        bom.setC_UOM_ID(p.getC_UOM_ID());
	        bom.setName(p.getName());  
	        bom.setValue(p.getValue());  
	        bom.setBOMType(MPPProductBOM.BOMTYPE_CurrentActive);  
	        bom.setBOMUse(MPPProductBOM.BOMUSE_Master);  
	        Timestamp yesterday = TimeUtil.addDays(TimeUtil.getDay(null), -1);  
	        bom.setValidFrom(yesterday);
	        bom.saveEx();  
	    }  
	  
	    if (cartonMaterialId != null && cartonMaterialId > 0) {  
	        int oldId = p.get_ValueOldAsInt(COLUMNNAME_CartonMaterial_ID);  
	        MPPProductBOMLine line = findBOMLineByProduct(bom, oldId > 0 ? oldId : cartonMaterialId);  
	        applyCartonBOMLine(bom, line, p, cartonMaterialId);  
	    }  
	}

	private MPPProductBOMLine findBOMLineByProduct(MPPProductBOM bom, int productId) {  
	    if (productId <= 0) return null;  
	    for (MPPProductBOMLine line : bom.getLines()) {  
	        if (line.getM_Product_ID() == productId)  
	            return line;  
	    }  
	    return null;  
	}
	
	private void applyCartonBOMLine(MPPProductBOM bom, MPPProductBOMLine line, MProduct p, int cartonMaterialId) {  
	    MProduct cartonProduct = MProduct.get(p.getCtx(), cartonMaterialId, p.get_TrxName());  
	    if (line == null) {  
	        line = new MPPProductBOMLine(bom);  
	        line.setComponentType(MPPProductBOMLine.COMPONENTTYPE_Component); 
	        line.setValidFrom(TimeUtil.addDays(TimeUtil.getDay(null), -1));
	        line.set_ValueOfColumn(COLUMNNAME_KeyMat, true);  
	    }
        // 计算 1/Imposition，保留2位小数；若 Imposition 为空或0则默认为1  
        BigDecimal qtyBOM = calcQtyByImposition(p);  
        line.setQtyBOM(qtyBOM);  
	    line.setM_Product_ID(cartonMaterialId);  
	    line.setC_UOM_ID(cartonProduct.getC_UOM_ID());  
	    Object lengbie = p.get_Value(COLUMNNAME_Lengbie);  
	    if (lengbie != null)  
	        line.set_ValueOfColumn(COLUMNNAME_Lengbie, lengbie);  
	    line.saveEx();  
	}  
	  
	private BigDecimal calcQtyByImposition(MProduct p) {  
	    Object impositionObj = p.get_Value("Imposition");  
	    if (impositionObj == null) {  
	        return BigDecimal.ONE;  
	    }  
	    // 自定义整型列通过 get_Value 返回 Integer  
	    BigDecimal imposition = new BigDecimal(impositionObj.toString());  
	    if (imposition.compareTo(BigDecimal.ZERO) <= 0) {  
	        return BigDecimal.ONE;  
	    }  
	    return BigDecimal.ONE.divide(imposition, 2, RoundingMode.HALF_UP);  
	}
	
	
	/**
	 * 物料名称校验
	 * @param topic 
	 */
	void ProductNameCheck(MProduct p, String topic) {
		
		if (!IEventTopics.PO_BEFORE_NEW.equals(topic) && !(IEventTopics.PO_BEFORE_CHANGE.equals(topic)
				&& (p.is_ValueChanged(MProduct.COLUMNNAME_Name) || p.is_ValueChanged(COLUMNNAME_Category_ID_L1)))) {
			return;
		}
		
		String catValue = DB.getSQLValueStringEx(p.get_TrxName(),
				"SELECT Value FROM M_Product_Category WHERE M_Product_Category_ID=?",
				p.get_Value(COLUMNNAME_Category_ID_L1));

		if (!"ZB".equals(catValue))
			return;

		int count = DB.getSQLValueEx(p.get_TrxName(),
				"SELECT COUNT(*) FROM M_Product " + "WHERE AD_Client_ID=? AND M_Product_Category_ID_L1=? "
						+ "AND Name=? AND M_Product_ID<>? AND IsActive='Y'",
				p.getAD_Client_ID(), p.get_Value(COLUMNNAME_Category_ID_L1), p.getName(), p.getM_Product_ID());
		if (count > 0)
			throw new AdempiereException("名称 " + p.getName() + " 重复，纸板名称需唯一。");
	}

}