package com.hoifu.event.processor;

import java.math.BigDecimal;
import java.util.Set;
import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.PO;
import org.compiere.util.DB;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;

public class ProductEventProcessor implements IEventProcessor {

	private static final String COLUMNNAME_Category_ID_L1 = "M_Product_Category_ID_L1";
	private static final String COLUMNNAME_CartonMaterial_ID = "CartonMaterial_ID";
	private static final String COLUMNNAME_Lengbie = "Lengbie";
	private static final String COLUMNNAME_KeyMat = "KeyMat";


	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MProduct;
	}

	@Override
	public void process(PO po, String topic) {
		MProduct product = (MProduct) po;

		//物料名称校验
		ProductNameCheck(product, topic);

		//自动创建或更新BOM数据
		autoCreateBOM(product, topic);

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
	    String orgValue = DB.getSQLValueString(p.get_TrxName(),  
	            "SELECT Value FROM AD_Org WHERE AD_Org_ID=?", p.getAD_Org_ID());  
	    if (!"0213".equals(orgValue)) return;  
	    
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
	        bom.setName(p.getName());  
	        bom.setValue(p.getValue());  
	        bom.setBOMType(MPPProductBOM.BOMTYPE_CurrentActive);  
	        bom.setBOMUse(MPPProductBOM.BOMUSE_Master);  
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
	        line.setQtyBOM(BigDecimal.ONE);  
	        line.setComponentType(MPPProductBOMLine.COMPONENTTYPE_Component);  
	        line.set_ValueOfColumn(COLUMNNAME_KeyMat, true);  
	    }  
	    line.setM_Product_ID(cartonMaterialId);  
	    line.setC_UOM_ID(cartonProduct.getC_UOM_ID());  
	    Object lengbie = p.get_Value(COLUMNNAME_Lengbie);  
	    if (lengbie != null)  
	        line.set_ValueOfColumn(COLUMNNAME_Lengbie, lengbie);  
	    line.saveEx();  
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