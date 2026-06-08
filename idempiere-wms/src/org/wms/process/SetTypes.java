/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.process;

import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;

import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.wms.model.MWM_ProductType;
import org.wms.model.MWM_StorageType;
import org.wms.model.MWM_Type; 

	public class SetTypes extends SvrProcess {

	private int WM_Type_ID = 0;
	private int M_Product_Category_ID = 0;
	private int M_Warehouse_ID = 0;
	private String X = "";
	private String Y = "";
	private String Z = "";
	private boolean DeleteOld = false;
	private String ProductValue = "";

	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
			for (ProcessInfoParameter p:para) {
				String name = p.getParameterName();
				if (p.getParameter() == null)
					;
				else if(name.equals("WM_Type_ID")){
					WM_Type_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Product_Category_ID")){
					M_Product_Category_ID = p.getParameterAsInt();
			}
				else if(name.equals("M_Warehouse_ID")){
					M_Warehouse_ID = p.getParameterAsInt();
			}
				else if(name.equals("X")){
					X = (String)p.getParameter();
			}
				else if(name.equals("Y")){
					Y = (String)p.getParameter();
			}
				else if(name.equals("Z")){
					Z = (String)p.getParameter();
			} 	
				else if(name.equals("ProductValue")){
				ProductValue = (String)p.getParameter();
			}
				else if(name.equals("DeleteOld")){
					DeleteOld = (boolean)p.getParameterAsBoolean();
			}
		}
	}
	MWM_Type type = null;
	int prodtypecnt = 0;
	int stortypecnt = 0;

	protected String doIt() {
		if (WM_Type_ID<1){
			throw new AdempiereException("No Type selected - cannot proceed");
		}
		type = new Query(Env.getCtx(),MWM_Type.Table_Name,MWM_Type.COLUMNNAME_WM_Type_ID+"=?",get_TrxName())
				.setParameters(WM_Type_ID).first();
		
		if (ProductValue!="") {
			List<MProduct>products = new Query(getCtx(),MProduct.Table_Name,MProduct.COLUMNNAME_UPC+" Like '"+ProductValue+"%'",get_TrxName())
					.list();
			if (products!=null)
				setProductTypes(products);
		} 
		
		List<MProductCategory> childcategories = new Query(Env.getCtx(),MProductCategory.Table_Name,MProductCategory.COLUMNNAME_M_Product_Category_Parent_ID+"=?",get_TrxName())
				.setParameters(M_Product_Category_ID)
				.list();
		if (childcategories!=null && childcategories.size()>0){
			for (MProductCategory cate:childcategories){
				createProductType(cate.get_ID());
			}
		} else
			createProductType(M_Product_Category_ID);
		
		if (M_Warehouse_ID>1){
			//if warehouse not specified no storage type will be set under the locators
			MWarehouse wh = new Query(Env.getCtx(),MWarehouse.Table_Name,MWarehouse.COLUMNNAME_M_Warehouse_ID+"=?",get_TrxName())
					.setParameters(M_Warehouse_ID)
					.setClient_ID()
					.first();
			createStorageTypes(wh);
		}
		return "Product Types created: "+prodtypecnt+", StorageTypesCreated: "+stortypecnt;
	}

	private void createProductType(int cateID) {
		if (cateID>0){
			List<MProduct> products = new Query(Env.getCtx(),MProduct.Table_Name,MProduct.COLUMNNAME_M_Product_Category_ID+"=?",get_TrxName())
					.setParameters(M_Product_Category_ID)
					.list();
			setProductTypes(products);
		}
	}

	private void setProductTypes(List<MProduct> products) {
		for (MProduct product:products){
			if (DeleteOld){//delete all product-types within the product first
				List<MWM_ProductType> types = new Query(Env.getCtx(),MWM_ProductType.Table_Name,MWM_ProductType.COLUMNNAME_M_Product_ID+"=?",get_TrxName())
						.setParameters(product.get_ID())
						.list();
				for (MWM_ProductType type:types){
					type.delete(true);
				}
			}
			//do new ones and append to existing (unless deleted all as above) 
			MWM_ProductType	prodtype = new MWM_ProductType(Env.getCtx(),0,get_TrxName());
			
			//Setting Product Type of Product to selected Type
			prodtype.setWM_Type_ID(type.get_ID());
			prodtype.setM_Product_ID(product.get_ID());
			prodtype.setPriority(type.getPriority()); 
			prodtype.saveEx(get_TrxName());
			prodtypecnt++;
		}
	}

	private void createStorageTypes(MWarehouse whse) {
		// TODO X/Y/Z will be maximum limits of process
		if (X==null || X.isEmpty() || X.equals("0"))
			X = "Z";
		if (Y==null || Y.isEmpty() || Y.equals("0"))
			Y = "Z";
		if (Z==null || Z.isEmpty() || Z.equals("0"))
			Z = "Z";
		List<MLocator> locators = new Query(Env.getCtx(),MLocator.Table_Name,MLocator.COLUMNNAME_M_Warehouse_ID+"=? AND "
				+(X.equals("Z")?"X<=? AND ":"X=? AND ")+(Y.equals("Z")?"Y<=? AND ":"Y=? AND ")+(Z.equals("Z")?"Z<=?":"Z=?"),get_TrxName())
				.setParameters(M_Warehouse_ID,X,Y,Z)
				.list();
		if (DeleteOld){
			for (MLocator locator:locators){
				//get possibly store types under each locator, to be deleted
				List<MWM_StorageType> stortypes = new Query(Env.getCtx(),MWM_StorageType.Table_Name,MWM_StorageType.COLUMNNAME_M_Locator_ID+"=?",get_TrxName())
						.setParameters(locator.get_ID())
						.list();
				for (MWM_StorageType stortype:stortypes){
					stortype.delete(true);
				}
			}
		}
		for (MLocator locator:locators){
			MWM_StorageType newStoreType = new MWM_StorageType(Env.getCtx(),0,get_TrxName());
			newStoreType.setM_Locator_ID(locator.get_ID());
			newStoreType.setWM_Type_ID(type.get_ID());
			newStoreType.setPriority(type.getPriority());
			if (M_Warehouse_ID>0)
				newStoreType.setM_Warehouse_ID(M_Warehouse_ID); 
			newStoreType.saveEx(get_TrxName());
			stortypecnt++;
		}
		//create new StorageType set to Type for all locators within 
	}
}
