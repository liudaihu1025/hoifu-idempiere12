package com.hoifu.factory;  
  
import org.adempiere.webui.factory.IInfoFactory;
import org.adempiere.webui.info.InfoWindow;
import org.adempiere.webui.panel.InfoPanel;
import org.compiere.model.GridField;
import org.compiere.model.Lookup;
import org.compiere.model.MInfoWindow;
import org.compiere.model.MRequisitionLine;
import org.compiere.model.MTable;
import org.osgi.service.component.annotations.Component;

import com.hoifu.info.BillPoolInfoWindow;
import com.hoifu.info.CInvoiceInfoWindow;
import com.hoifu.info.COfficeRequisitionInfoWindow;
import com.hoifu.info.CreateFromInOutInfoWindow;
import com.hoifu.info.DYGraphicDesignReviewInfoWindow;
import com.hoifu.info.DYGraphicDesignTaskInfoWindow;
import com.hoifu.info.DYProcessDesignInfoWindow;
import com.hoifu.info.DYSampleReviewInfoWindow;
import com.hoifu.info.InfoOrderWindowWithTotal;
import com.hoifu.info.InfoPurchaseLineWindow;
import com.hoifu.info.MInOutLineInfoWindow;
import com.hoifu.info.MInventoryInfoWindow;
import com.hoifu.info.MPaymentRequestLineInfoWindow;
import com.hoifu.info.MRequisitionLineInfoWindow;
import com.hoifu.info.PPOrderInfoWindow;
import com.hoifu.info.PPOrderNodeInfoWindow;
import com.hoifu.info.PPOrderShortageInfoWindow;
import com.hoifu.info.PurchaseRequisitionMaterialInfoWindow;
import com.hoifu.info.RVMTransactionDetailInfo;
import com.hoifu.info.RVProductInfoWindow;
import com.hoifu.info.YGProofCirculationInfoWindow;
import com.hoifu.model.MPaymentRequestLine;
  
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
            // 欠数工单信息窗口（通过 AD_InfoWindow 名称区分）
            MInfoWindow infoWin = MInfoWindow.getInfoWindow(AD_InfoWindow_ID);
            if (infoWin != null && "欠数工单信息".equals(infoWin.getName())) {
                return new PPOrderShortageInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
                        whereClause, AD_InfoWindow_ID, true, field);
            }
            return new PPOrderInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection, whereClause,
                    AD_InfoWindow_ID, true, field);
        }
        if ("PP_Order_Node".equals(tableName)) {
            return new PPOrderNodeInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection, whereClause,
                    AD_InfoWindow_ID, true, field);
        }
        if ("M_InventoryLine".equals(tableName)) {  
            return new MInventoryInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,  
                    whereClause, AD_InfoWindow_ID, true, field, null);  
        }  
        if (MPaymentRequestLine.Table_Name.equals(tableName)) {  
            return new MPaymentRequestLineInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,  
                    whereClause, AD_InfoWindow_ID, true, field, null);  
        }  
        if (MRequisitionLine.Table_Name.equals(tableName)) {  
            return new MRequisitionLineInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,  
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
		if ("M_InOutLine".equals(tableName)) {
			return new MInOutLineInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, true, field, null);
		}
		if ("RV_M_Transaction_Detail".equals(tableName)) {
			return new RVMTransactionDetailInfo(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, true, field, null);
		}
		if ("C_OfficeRequisitionLine".equals(tableName)) {
            return new COfficeRequisitionInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection, 
            		whereClause, AD_InfoWindow_ID, true, field, null);
        }
		// 样稿流转信息窗口
		if ("yg_proofborr".equals(tableName)) {
			return new YGProofCirculationInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, true, field, null);
		}
		// 平面设计评审信息窗口
		if ("dy_graphicdesigneffect".equals(tableName)) {
			return new DYGraphicDesignReviewInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, true, field, null);
		}
		// 工艺设计任务列表信息窗口 (由于平面设计任务列表信息窗口也是这个表，所以只能用名称加以区分)
		if ("dy_samplingdemand".equals(tableName)) {
			MInfoWindow infoWindow = MInfoWindow.getInfoWindow(AD_InfoWindow_ID);
			if (infoWindow != null && "工艺设计".equals(infoWindow.getName())) {
				return new DYProcessDesignInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
						whereClause, AD_InfoWindow_ID, true, field, null);
			}
			// 样品评审信息窗口
			if (infoWindow != null && "样品评审".equals(infoWindow.getName())) {
				return new DYSampleReviewInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
						whereClause, AD_InfoWindow_ID, true, field, null);
			}
			// 平面设计任务列表信息窗口
			if (infoWindow != null && "平面设计任务列表".equals(infoWindow.getName())) {
				return new DYGraphicDesignTaskInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value, multiSelection,
						whereClause, AD_InfoWindow_ID, true, field, null);
			}
		}
		// 申购物料信息窗口
		if ("PP_Order_BOMLine".equals(tableName)) {
			return new PurchaseRequisitionMaterialInfoWindow(lookup.getWindowNo(), tableName, keyColumn, value,
					multiSelection, whereClause, AD_InfoWindow_ID, true, field, null);
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
                // 欠数工单信息窗口（通过 AD_InfoWindow 名称区分）
                if ("欠数工单信息".equals(infoWindow.getName())) {
                    return new PPOrderShortageInfoWindow(windowNo, tableName, keyColumn, null, true, null,
                            AD_InfoWindow_ID, false, null, predefinedContextVariables);
                }
                return new PPOrderInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,
                        null, predefinedContextVariables);
            }  
            if ("PP_Order_Node".equals(tableName)) {
                MTable table = (MTable) infoWindow.getAD_Table();
                String keyColumn = tableName + "_ID";
                if (table.isUUIDKeyTable())
                    keyColumn = tableName + "_UU";
                return new PPOrderNodeInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,
                        null, predefinedContextVariables);
            }
            if ("M_InventoryLine".equals(tableName)) {  
                String keyColumn = tableName + "_ID";  
                return new MInventoryInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,  
                        null, predefinedContextVariables);  
            }  
            if (MPaymentRequestLine.Table_Name.equals(tableName)) {  
            	String keyColumn = tableName + "_ID";  
                return new MPaymentRequestLineInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,  
                        null, predefinedContextVariables);  
            }  
            if (MRequisitionLine.Table_Name.equals(tableName)) {  
            	String keyColumn = tableName + "_ID";  
                return new MRequisitionLineInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID, false,  
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
			if ("M_InOutLine".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				return new MInOutLineInfoWindow(windowNo, tableName, keyColumn, null, true, null, AD_InfoWindow_ID,
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
			if ("C_OfficeRequisitionLine".equals(tableName)) {
				String keyColumn = tableName + "_ID";
				return new COfficeRequisitionInfoWindow(windowNo, tableName, keyColumn, null, true, null,
						AD_InfoWindow_ID, false, null, predefinedContextVariables);
			}
			// 样稿流转信息窗口
			if ("yg_proofborr".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				return new YGProofCirculationInfoWindow(windowNo, tableName, keyColumn, null, false, null,
						AD_InfoWindow_ID, false, null, predefinedContextVariables);
			}
			// 平面设计评审信息窗口
			if ("dy_graphicdesigneffect".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				return new DYGraphicDesignReviewInfoWindow(windowNo, tableName, keyColumn, null, false, null,
						AD_InfoWindow_ID, false, null, predefinedContextVariables);
			}
			// 工艺设计任务列表信息窗口 (由于平面设计任务列表信息窗口也是这个表，所以只能用名称加以区分)
			if ("dy_samplingdemand".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				if ("工艺设计".equals(infoWindow.getName())) {
					return new DYProcessDesignInfoWindow(windowNo, tableName, keyColumn, null, false, null,
							AD_InfoWindow_ID, false, null, predefinedContextVariables);
				}
				// 样品评审信息窗口
				if ("样品评审".equals(infoWindow.getName())) {
					return new DYSampleReviewInfoWindow(windowNo, tableName, keyColumn, null, false, null,
							AD_InfoWindow_ID, false, null, predefinedContextVariables);
				}
				//平面设计任务列表信息窗口
				if ("平面设计任务列表".equals(infoWindow.getName())) {
					return new DYGraphicDesignTaskInfoWindow(windowNo, tableName, keyColumn, null, false, null,
							AD_InfoWindow_ID, false, null, predefinedContextVariables);
				}
			}
			// 申购物料信息窗口
			if ("PP_Order_BOMLine".equals(tableName)) {
				MTable table = (MTable) infoWindow.getAD_Table();
				String keyColumn = tableName + "_ID";
				if (table.isUUIDKeyTable())
					keyColumn = tableName + "_UU";
				return new PurchaseRequisitionMaterialInfoWindow(windowNo, tableName, keyColumn, null, false, null,
						AD_InfoWindow_ID, false, null, predefinedContextVariables);
			}
		}
        return null;  
    }  
  
    @Override  
    public InfoPanel create(int WindowNo, String tableName, String keyColumn, String value, boolean multiSelection,  
            String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {  
        if ("PP_Order".equals(tableName)) {
            // 欠数工单信息窗口（通过 AD_InfoWindow 名称区分）
            MInfoWindow infoWin = MInfoWindow.getInfoWindow(AD_InfoWindow_ID);
            if (infoWin != null && "欠数工单信息".equals(infoWin.getName())) {
                return new PPOrderShortageInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
                        AD_InfoWindow_ID, lookup, field, null);
            }
            return new PPOrderInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
                    AD_InfoWindow_ID, lookup, field, null);
        }
        if ("PP_Order_Node".equals(tableName)) {
            return new PPOrderNodeInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
                    AD_InfoWindow_ID, lookup, field, null);
        }
        if ("M_InventoryLine".equals(tableName)) {  
            return new MInventoryInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,  
                    AD_InfoWindow_ID, lookup, field, null);  
        }  
        if (MPaymentRequestLine.Table_Name.equals(tableName)) {  
            return new MPaymentRequestLineInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,  
                    AD_InfoWindow_ID, lookup, field, null);  
        }  
        if (MRequisitionLine.Table_Name.equals(tableName)) {  
            return new MRequisitionLineInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,  
                    AD_InfoWindow_ID, lookup, field, null);  
        }  
        if ("M_InOutLine".equals(tableName)) {  
            return new MInOutLineInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,  
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
		if ("C_OfficeRequisitionLine".equals(tableName)) {
			return new COfficeRequisitionInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
					AD_InfoWindow_ID, lookup, field, null);
		}
		// 样稿流转信息窗口
		if ("yg_proofborr".equals(tableName)) {
			return new YGProofCirculationInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
					AD_InfoWindow_ID, lookup, field, null);
		}
		// 平面设计评审信息窗口
		if ("dy_graphicdesigneffect".equals(tableName)) {
			return new DYGraphicDesignReviewInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
					AD_InfoWindow_ID, lookup, field, null);
		}
		// 工艺设计任务列表信息窗口 (由于平面设计任务列表信息窗口也是这个表，所以只能用名称加以区分)
		if ("dy_samplingdemand".equals(tableName)) {
			MInfoWindow infoWindow = MInfoWindow.getInfoWindow(AD_InfoWindow_ID);
			if (infoWindow != null && "工艺设计".equals(infoWindow.getName())) {
				return new DYProcessDesignInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
						AD_InfoWindow_ID, lookup, field, null);
			}
			// 样品评审信息窗口
			if (infoWindow != null && "样品评审".equals(infoWindow.getName())) {
				return new DYSampleReviewInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
						AD_InfoWindow_ID, lookup, field, null);
			}
			//平面设计任务列表信息窗口
			if (infoWindow != null && "平面设计任务列表".equals(infoWindow.getName())) {
				return new DYGraphicDesignTaskInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection, whereClause,
						AD_InfoWindow_ID, lookup, field, null);
			}
		}
		// 平面设计评审信息窗口
		if ("PP_Order_BOMLine".equals(tableName)) {
			return new PurchaseRequisitionMaterialInfoWindow(WindowNo, tableName, keyColumn, value, multiSelection,
					whereClause, AD_InfoWindow_ID, lookup, field, null);
		}
		return null;  
    }  
}