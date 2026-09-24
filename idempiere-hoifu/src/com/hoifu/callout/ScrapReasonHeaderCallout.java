package com.hoifu.callout;  
  
import java.util.Properties;  
  
import org.adempiere.base.IColumnCallout;  
import org.adempiere.base.annotation.Callout;  
import org.compiere.model.GridField;  
import org.compiere.model.GridTab;  
import org.compiere.model.GridWindow;  
import org.compiere.model.MDocType;
import org.compiere.model.MInventoryLine;
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import org.compiere.util.Util;  
  
/**  
 * 表头 M_Inventory.ScrapReason 变化时，联动重算 C_Charge_ID。  
 * 具体映射规则和"当前组织优先、否则回退全组织"的查找逻辑封装在  
 * 数据库函数 get_scrap_charge_id(AD_Org_ID, ScrapReason) 中。  
 *  
 * 只在单据类型为"报废单"（C_DocType_UU = 3c69e54b-296d-4799-b3c5-27137da3ff1c）时生效。  
 */  
@Callout(tableName = "M_Inventory", columnName = { "ScrapReason" })  
public class ScrapReasonHeaderCallout implements IColumnCallout {  
  
	private static final CLogger log = CLogger.getCLogger(ScrapReasonHeaderCallout.class);  
	private static final String SCRAP_DOCTYPE_UU = "3c69e54b-296d-4799-b3c5-27137da3ff1c";  
  
	@Override  
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
  
		// 1. 只在报废单中生效  
		int docTypeId = Env.getContextAsInt(ctx, WindowNo, "C_DocType_ID");  
		if (docTypeId <= 0)  
			return "";  
		MDocType docType = MDocType.get(ctx, docTypeId);  
		if (docType == null || !SCRAP_DOCTYPE_UU.equals(docType.getC_DocType_UU()))  
			return "";  
  
		String scrapReason = value == null ? null : value.toString();  
		if (Util.isEmpty(scrapReason, true))  
			return "";  
  
		// 2. 取当前行/表头的 AD_Org_ID  
		Number orgNum = (Number) mTab.getValue("AD_Org_ID");  
		int adOrgId = orgNum != null ? orgNum.intValue() : Env.getAD_Org_ID(ctx);  
  
		// 3. 调用数据库函数得到 C_Charge_ID  
		Integer chargeId = null;  
		try {  
			int result = DB.getSQLValueEx(null,  
					"SELECT get_scrap_charge_id(?, ?)", adOrgId, scrapReason);  
			if (result > 0)  
				chargeId = result;  
		} catch (Exception e) {  
			log.severe("调用 get_scrap_charge_id 失败: " + e.getMessage());  
			return "调用费用类型查找函数失败：" + e.getMessage();  
		}  
  
		if (chargeId == null)  
			return "未找到报废原因【" + scrapReason + "】对应的费用类型，请先在 C_Charge（费用类型）维护该记录";  
  
		// 4. 联动更新子表 M_InventoryLine 所有已加载行的 C_Charge_ID  
		GridWindow window = mTab.getGridWindow();  
		if (window == null)  
			return "";  
  
		int currentTabLevel = mTab.getTabLevel();  
		int currentTabIndex = window.getTabIndex(mTab);  
		for (int i = currentTabIndex + 1; i < window.getTabCount(); i++) {  
			GridTab childTab = window.getTab(i);  
			if (childTab.getTabLevel() <= currentTabLevel)  
				break;  
			if (childTab.getTabLevel() != currentTabLevel + 1)  
				continue;  
			if (!MInventoryLine.Table_Name.equals(childTab.getTableName()))  
				continue;  
  
			if (!childTab.isLoadComplete()) {  
				childTab.initTab(false);  
				childTab.query(false, 0, 0);  
			}  
  
			int rowCount = childTab.getRowCount();  
			int currentRow = childTab.getCurrentRow();  
			try {  
				for (int row = 0; row < rowCount; row++) {  
					childTab.setCurrentRow(row);  
					childTab.setValue(MInventoryLine.COLUMNNAME_C_Charge_ID, chargeId);  
				}  
			} finally {  
				childTab.setCurrentRow(currentRow);  
			}  
		}  
  
		return "";  
	}  
}