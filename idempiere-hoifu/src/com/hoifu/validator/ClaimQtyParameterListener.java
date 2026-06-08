package com.hoifu.validator;
  
import java.math.BigDecimal;
import java.util.List;

import org.adempiere.webui.apps.IProcessParameterListener;
import org.adempiere.webui.apps.ProcessParameterPanel;
import org.adempiere.webui.editor.WEditor;
import org.compiere.model.MInventoryLine;
import org.compiere.model.MStorageOnHand;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;
  
@Component(service = IProcessParameterListener.class, property = {
		"ProcessClass=com.hoifu.process.OfficeRequisitionClaimStatusProcess"
    }  
)
public class ClaimQtyParameterListener implements IProcessParameterListener {

	@Override
	public void onChange(ProcessParameterPanel parameterPanel, String columnName, WEditor editor) {
		// 对于动态默认值的需求，这个方法可以为空
		// 如果需要处理参数值变更逻辑，可以在这里添加代码
	}
      
    @Override  
	public void onInit(ProcessParameterPanel parameterPanel) {
		// 获取ClaimQty参数的编辑器
		WEditor claimQtyEditor = parameterPanel.getEditor("ClaimQty");
		if (claimQtyEditor != null) {
			// 计算动态默认值
			BigDecimal defaultValue = calculateDefaultClaimQty(parameterPanel);
			if (defaultValue != null && defaultValue.signum() > 0) {
				claimQtyEditor.setValue(defaultValue);
            }  
        }  
    }  
      
	private BigDecimal calculateDefaultClaimQty(ProcessParameterPanel parameterPanel) {
		// Get selected record IDs from ProcessInfo
		List<Integer> selectedIds = getSelectedRecordIdsFromInfoWindow(parameterPanel);
		if (selectedIds == null || selectedIds.isEmpty()) {
			return null;
		}
		// Use the first selected record ID
		Integer recordId = selectedIds.get(0);

		// 获取库存明细记录
		MInventoryLine line = new MInventoryLine(Env.getCtx(), recordId, null);

		// 获取需求数量
		BigDecimal demandQty = (BigDecimal) line.get_Value("QtyDemand");
		if (demandQty == null) {
			demandQty = Env.ZERO;
		}

		// 获取已领用数量
		BigDecimal claimedQty = line.getQtyInternalUse();
		if (claimedQty == null) {
			claimedQty = Env.ZERO;
        }  
          
		// 计算建议领用数量
		BigDecimal suggestedQty = demandQty.subtract(claimedQty);
		if (suggestedQty.signum() <= 0) {
			return Env.ZERO;
        }  

		// 获取库存数量
		BigDecimal stockQty = MStorageOnHand.getQtyOnHandForLocator(line.getM_Product_ID(), line.getM_Locator_ID(), 0,
				null);

		// 如果建议数量超过库存，使用库存数量
		if (suggestedQty.compareTo(stockQty) > 0) {
			suggestedQty = stockQty;
        }  

		return suggestedQty;
    }  

	private List<Integer> getSelectedRecordIdsFromInfoWindow(ProcessParameterPanel parameterPanel) {
		// Try to get from ProcessInfo first (might work in some cases)
		List<Integer> recordIds = parameterPanel.getProcessInfo().getRecord_IDs();
		if (recordIds != null && !recordIds.isEmpty()) {
			return recordIds;
		}

		// Alternative: Get from InfoWindow if accessible
		// This requires access to the InfoWindow instance
		// You may need to pass this through the ProcessInfo or find another way

		return null;
	}
}