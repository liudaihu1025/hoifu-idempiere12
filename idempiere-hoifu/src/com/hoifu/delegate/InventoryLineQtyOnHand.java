package com.hoifu.delegate;  
  
import java.math.BigDecimal;  
  
import org.adempiere.base.annotation.EventTopicDelegate;  
import org.adempiere.base.annotation.ModelEventTopic;  
import org.adempiere.base.event.annotations.ModelEventDelegate;  
import org.adempiere.base.event.annotations.doc.AfterComplete;  
import org.compiere.model.MDocType;  
import org.compiere.model.MInventory;  
import org.compiere.model.MInventoryLine;  
import org.compiere.model.MStorageOnHand;  
import org.osgi.service.event.Event;  
 
/**
 * 领退单完成时，保存当时库存
 */
@EventTopicDelegate  
@ModelEventTopic(modelClass = MInventory.class)  
public class InventoryLineQtyOnHand extends ModelEventDelegate<MInventory> {  
  
	public InventoryLineQtyOnHand(MInventory po, Event event) {  
		super(po, event);  
	}  
  
	@AfterComplete  
	public void onAfterComplete() {  
		MInventory inventory = getModel();  
  
		// 只有"内部使用库存单"(DocSubTypeInv = 'IU') 才更新 QtyOnHand  
		MDocType dt = MDocType.get(inventory.getC_DocType_ID());  
		String docSubTypeInv = dt.getDocSubTypeInv();  
		if (!MDocType.DOCSUBTYPEINV_InternalUseInventory.equals(docSubTypeInv))  
			return;  
  
		MInventoryLine[] lines = inventory.getLines(false);  
		for (MInventoryLine line : lines) {  
			if (!line.isActive())  
				continue;  
  
			// 产品和库位都必须有值才更新  
			if (line.getM_Product_ID() <= 0 || line.getM_Locator_ID() <= 0)  
				continue;  
  
			BigDecimal qtyOnHand = MStorageOnHand.getQtyOnHandForLocator(  
					line.getM_Product_ID(),  
					line.getM_Locator_ID(),  
					line.getM_AttributeSetInstance_ID(),  
					line.get_TrxName());  
  
			line.set_ValueOfColumn("QtyOnHand", qtyOnHand);  
			line.saveEx();  
		}  
	}  
}