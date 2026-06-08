package com.hoifu.factory;  
  
import org.adempiere.webui.factory.IInfoFactory;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.panel.InfoPanel;
import org.compiere.model.GridField;
import org.compiere.model.Lookup;
import org.compiere.model.MInfoWindow;
import org.compiere.model.MTable;
import org.osgi.service.component.annotations.Component;

import com.hoifu.info.BillPoolInfoWindow;
import com.hoifu.info.CInvoiceInfoWindow;
import com.hoifu.info.CreateFromInOutInfoWindow;
import com.hoifu.info.InfoOrderWindowWithTotal;
import com.hoifu.info.InfoPurchaseLineWindow;
import com.hoifu.info.MInventoryInfoWindow;
import com.hoifu.info.PPOrderInfoWindow;
import com.hoifu.info.RVMTransactionDetailInfo;
import com.hoifu.info.RVProductInfoWindow;  
  
/**  
 * 自定义信息窗口工厂-根据表名匹配InfoWindow，实现参考DefaultInfoFactory系统默认实现  
 */  
@Component(service = IInfoFactory.class, property = { "service.ranking:Integer=50" })  
public class BaseInfoWindowFactory implements IInfoFactory {  
  
    @Override  
    public InfoPanel create(int WindowNo, String tableName, String keyColumn, String value, boolean multiSelection,  
            String whereClause, int AD_InfoWindow_ID, boolean lookup) {  
        return create(WindowNo, tableName, keyColumn, value, multiSelection, whereClause, AD_InfoWindow_ID, lookup,  
                null);  
    }  
  
    @Override  
    public InfoPanel create(Lookup lookup, GridField field, String tableName, String keyColumn, String value,  
            boolean multiSelection, String whereClause, int AD_InfoWindow_ID) {  
        if ("PP_Order".equals(tableName)) {  
            return new PPOrderInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection, whereClause,  
                    AD_InfoWindow_ID, true, field);  
        }  
        if ("M_InventoryLine".equals(tableName)) {  
            return new MInventoryInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,  
                    whereClause, AD_InfoWindow_ID, true, field, null);  
        }  
        if ("RV_M_Product".equals(tableName)) {  
            return new RVProductInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,  
                    whereClause, AD_InfoWindow_ID, true, field);  
        }  
        if ("C_OrderLine".equals(tableName)) {  
            return new InfoPurchaseLineWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,  
                    whereClause, AD_InfoWindow_ID, true, field, null);  
		}
		// 添加 C_Order 支持
		if ("C_Order".equals(tableName)) {
			return new InfoOrderWindowWithTotal(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, true, field, null);
		}
		if ("C_Bill_Pool".equals(tableName)) {
			return new BillPoolInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, true, field, null);
		}
		if ("C_Invoice".equals(tableName)) {
			return new CInvoiceInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, true, field, null);
		}
		if ("RV_M_Transaction_Detail".equals(tableName)) {
			return new RVMTransactionDetailInfo(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, true, field, null);
		}
        return null;  
    }  
  
    @Override  
    public InfoWindow create(int AD_InfoWindow_ID) {  
        return create(-1, AD_InfoWindow_ID, null);  
    }  
  
    @Override  
    public InfoWindow create(int windowNo, int AD_InfoWindow_ID, String predefinedContextVariables) {  
        MInfoWindow infoWindow = MInfoWindow.getInfoWindow(AD_InfoWindow_ID);  
        if (infoWindow != null) {  
            String tableName = infoWindow.getAD_Table().getTableName();  
            if ("PP_Order".equals(tableName)) {  
                MTable table = (MTable) infoWindow.getAD_Table();  
                String keyColumn = tableName + "_ID";  
                if (table.isUUIDKeyTable())  
                    keyColumn = tableName + "_UU";  
                return new PPOrderInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,  
                        null, predefinedContextVariables);  
            }  
            if ("M_InventoryLine".equals(tableName)) {  
                String keyColumn = tableName + "_ID";  
                return new MInventoryInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,  
                        null, predefinedContextVariables);  
            }  
            if ("RV_M_Product".equals(tableName)) {  
                MTable table = (MTable) infoWindow.getAD_Table();  
                String keyColumn = tableName + "_ID";  
                if (table.isUUIDKeyTable())  
                    keyColumn = tableName + "_UU";  
                return new RVProductInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,  
                        null, predefinedContextVariables);  
            }  
            if ("C_OrderLine".equals(tableName)) {  
                MTable table = (MTable) infoWindow.getAD_Table();  
                String keyColumn = tableName + "_ID";  
                if (table.isUUIDKeyTable())  
                    keyColumn = tableName + "_UU";  
                return new InfoPurchaseLineWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,  
                        null, predefinedContextVariables);  
			}
			// 添加 C_Order 支持
			if ("C_Order".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				return new InfoOrderWindowWithTotal(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID,
						false, null, predefinedContextVariables);
            }  
			if ("C_Bill_Pool".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				return new BillPoolInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID,
						false, null, predefinedContextVariables);
			}
			if ("C_Invoice".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				return new CInvoiceInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID,
						false, null, predefinedContextVariables);
			}
			if ("RV_M_Transaction_Detail".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				return new RVMTransactionDetailInfo(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID,
						false, null, predefinedContextVariables);
			}
			// 新增：M_InOut_CreateFrom_v 信息窗口
			if ("M_InOut_CreateFrom_v".equals(tableName)) {
				String keyColumn = tableName + "_ID"; // M_InOut_CreateFrom_v_ID
				return new CreateFromInOutInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID,
						false, null, predefinedContextVariables);
			}

        }  
        return null;  
    }  
  
    @Override  
    public InfoPanel create(int WindowNo, String tableName, String keyColumn, String value, boolean multiSelection,  
            String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {  
        if ("PP_Order".equals(tableName)) {  
            return new PPOrderInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,  
                    AD_InfoWindow_ID, lookup, field, null);  
        }  
        if ("M_InventoryLine".equals(tableName)) {  
            return new MInventoryInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,  
                    AD_InfoWindow_ID, lookup, field, null);  
        }  
        if ("RV_M_Product".equals(tableName)) {  
            return new RVProductInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,  
                    AD_InfoWindow_ID, lookup, field, null);  
        }  
        if ("C_OrderLine".equals(tableName)) {  
            return new InfoPurchaseLineWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,  
                    AD_InfoWindow_ID, lookup, field, null);  
		}
		// 添加 C_Order 支持
		if ("C_Order".equals(tableName)) {
			return new InfoOrderWindowWithTotal(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
					AD_InfoWindow_ID, lookup, field, null);
        }  
		if ("C_Bill_Pool".equals(tableName)) {
			return new BillPoolInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
					AD_InfoWindow_ID, lookup, field, null);
		}
		if ("C_Invoice".equals(tableName)) {
			return new CInvoiceInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
					AD_InfoWindow_ID, lookup, field, null);
		}
		if ("RV_M_Transaction_Detail".equals(tableName)) {
			return new RVMTransactionDetailInfo(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
					AD_InfoWindow_ID, lookup, field, null);
		}
		// 新增：M_InOut_CreateFrom_v 信息窗口
		if ("M_InOut_CreateFrom_v".equals(tableName)) {
			return new CreateFromInOutInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
					AD_InfoWindow_ID, lookup, field, null);
		}
        return null;  
    }  
}