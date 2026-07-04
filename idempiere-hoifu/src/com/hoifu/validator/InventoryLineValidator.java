package com.hoifu.validator;  
  
import java.math.BigDecimal;

import org.compiere.model.MClient;
import org.compiere.model.MDocType;
import org.compiere.model.MInventory;  
import org.compiere.model.MInventoryLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.ModelValidationEngine;  
import org.compiere.model.ModelValidator;  
import org.compiere.model.PO;  
import org.compiere.process.DocAction;  
import org.compiere.process.ProcessInfo;  
import org.compiere.util.DB;  
import org.compiere.wf.MWorkflow;  
import org.compiere.util.Env;  
import org.compiere.util.CLogger;  
  
/**  
 * 监听库存明细行状态变化，自动完成库存单据  
 */  
public class InventoryLineValidator implements ModelValidator {  
  
	private static CLogger log = CLogger.getCLogger(InventoryLineValidator.class);  
	private int m_AD_Client_ID = -1;  
	private boolean isCompleting = false; // 防止递归调用  
	private boolean isUpdatingQty = false; // 添加类成员变量  
	
	@Override  
	public void initialize(ModelValidationEngine engine, MClient client) {  
		log.info("InventoryLineValidator 正在初始化...");  
		if (client != null) {  
			m_AD_Client_ID = client.getAD_Client_ID();  
		} else {  
			m_AD_Client_ID = 0;  
		}  
  
		engine.addModelChange(MInventoryLine.Table_Name, this);  
		log.info("InventoryLineValidator 已注册监听: " + MInventoryLine.Table_Name);  
	}  
  
	@Override  
	public String modelChange(PO po, int type) throws Exception {  
		if (po instanceof MInventoryLine && (type == TYPE_AFTER_NEW || type == TYPE_AFTER_CHANGE)) {  
			MInventoryLine line = (MInventoryLine) po;  
  
			log.info("MInventoryLine changed: ID=" + line.getM_InventoryLine_ID() +   
					", ClaimStatus=" + line.get_Value("ClaimStatus"));  
  
	        // 检查对应的库存单据是否为"行政物料领用单"  
	        MInventory inventory = new MInventory(Env.getCtx(), line.getM_Inventory_ID(), line.get_TrxName());  
	        MDocType docType = MDocType.get(inventory.getC_DocType_ID());  
	          
	        if (docType == null || !"行政物料领用单".equals(docType.getName())) {  
	            log.info("跳过非行政物料领用单: DocType=" + (docType != null ? docType.getName() : "null"));  
	            return null;  
	        }  
	        
			String claimStatus = (String) line.get_Value("ClaimStatus");  
			if (claimStatus != null && "YL".equals(claimStatus)) {  
				checkAndCompleteInventory(line.getM_Inventory_ID(), line.get_TrxName());  
			}  
		}  
		return null;  
	}  
  
	/**  
	 * 检查并完成库存单据  
	 */  
	private void checkAndCompleteInventory(int M_Inventory_ID, String trxName) {  
		if (isCompleting) {  
			log.info("正在完成单据，跳过递归调用");  
			return;  
		}  
  
		try {  
			isCompleting = true;  

			String sql = "SELECT COUNT(*) FROM M_InventoryLine " +  
					"WHERE M_Inventory_ID = ? AND (ClaimStatus != 'YL')";  
			int nonClaimedCount = DB.getSQLValue(trxName, sql, M_Inventory_ID);  

			log.info("Inventory ID: " + M_Inventory_ID + ", Non-claimed count: " + nonClaimedCount);  

			if (nonClaimedCount == 0) {  
				MInventory inventory = new MInventory(Env.getCtx(), M_Inventory_ID, trxName);  
				log.info("Attempting to complete inventory: " + inventory.getDocumentNo() +   
						", Current status: " + inventory.getDocStatus());  
  
				if (DocAction.STATUS_Drafted.equals(inventory.getDocStatus()) ||  
					DocAction.STATUS_InProgress.equals(inventory.getDocStatus())) {  
  
					ProcessInfo info = MWorkflow.runDocumentActionWorkflow(inventory, DocAction.ACTION_Complete);  
					if (info.isError()) {  
//						log.warning("自动完成库存单据失败: " + info.getSummary());  
					} else {  
						log.info("库存单据已自动完成: " + inventory.getDocumentNo());  
					}  
				} else {  
					log.warning("库存单据状态不允许完成: " + inventory.getDocStatus() +   
							", 错误信息: " + inventory.getProcessMsg());  
				}  
			}  
		} finally {  
			isCompleting = false;  
		}  
	}  
  
	@Override  
	public int getAD_Client_ID() {  
		return m_AD_Client_ID;  
	}  
  
	@Override  
	public String login(int AD_Org_ID, int AD_Role_ID, int AD_User_ID) {  
		return null;  
	}  
  
	@Override  
	public String docValidate(PO po, int timing) {  
		return null;  
	}  
}