package com.hoifu.info;

import java.sql.Timestamp;
import java.util.List;

import org.adempiere.webui.component.Button;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.model.GridField;
import org.compiere.model.MProcess;
import org.compiere.util.Env;

import com.hoifu.model.MBillPool;

public class BillPoolInfoWindow extends InfoWindow {  

	// 基础构造函数 - 7个参数
	public BillPoolInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID);
	}

	// 带 lookup 参数的构造函数 - 8个参数
	public BillPoolInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup);
	}

	// 带 GridField 参数的构造函数 - 9个参数
	public BillPoolInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field);
	}

	// 完整构造函数 - 10个参数
	public BillPoolInfoWindow(int WindowNo, String tableName, String keyColumn, String queryValue,
			boolean multipleSelection, String whereClause, int AD_InfoWindow_ID, boolean lookup, GridField field,
			String predefinedContextVariables) {
		super(WindowNo, tableName, keyColumn, queryValue, multipleSelection, whereClause, AD_InfoWindow_ID, lookup,
				field, predefinedContextVariables);
	}
      
    @Override  
	protected void enableButtons(boolean enable) {
        // 获取选中的票据记录  
		List<Integer> selectedKeys = getSelectedRowKeys();
        if (selectedKeys == null || selectedKeys.isEmpty()) {  
			super.enableButtons(false);
            return;  
        }  

		// 先调用父类方法处理标准按钮
		super.enableButtons(enable);
          
        // 检查签收按钮条件  
        boolean canAccept = checkAcceptCondition(selectedKeys);  
        // 检查背书按钮条件    
        boolean canEndorse = checkEndorseCondition(selectedKeys);  
        // 检查到期收款按钮条件  
        boolean canMaturity = checkMaturityCondition(selectedKeys);  
		// 检查签发按钮条件
		boolean canIssue = checkIssueCondition(selectedKeys);
		// 检查到期付款按钮条件
		boolean canPayment = checkPaymentCondition(selectedKeys);
		// 检查贴现按钮条件
		boolean canDiscount = checkDiscountCondition(selectedKeys);
          
        // 设置按钮状态  
        for (Button btn : btProcessList) {  
			Integer processId = (Integer) btn.getAttribute(PROCESS_ID_KEY);
			if (processId != null) {
				MProcess process = MProcess.get(Env.getCtx(), processId);
				if (process.getClassname() != null
						&& process.getClassname().equals("com.hoifu.process.BillReceiptAcceptProcess")) {
					btn.setEnabled(canAccept);
				} else if (process.getClassname() != null
						&& process.getClassname().equals("com.hoifu.process.BillReceiptEndorseProcess")) {
					btn.setEnabled(canEndorse);
				} else if (process.getClassname() != null
						&& process.getClassname().equals("com.hoifu.process.BillReceiptMaturityProcess")) {
					btn.setEnabled(canMaturity);
				} else if (process.getClassname() != null
						&& process.getClassname().equals("com.hoifu.process.BillPaymentIssueProcess")) {
					btn.setEnabled(canIssue);
				} else if (process.getClassname() != null
						&& process.getClassname().equals("com.hoifu.process.BillPaymentMaturityProcess")) {
					btn.setEnabled(canPayment);
				} else if (process.getClassname() != null
						&& process.getClassname().equals("com.hoifu.process.BillDiscountProcess")) {
					btn.setEnabled(canDiscount);
				} else {
					btn.setEnabled(enable);
				}
			}
        }  
    }  
      
    private boolean checkAcceptCondition(List<Integer> selectedKeys) {  
        Timestamp currentDate = new Timestamp(System.currentTimeMillis());  
        for (Integer key : selectedKeys) {  
			// 查询C_Bill_Pool获取BusinessStatus和MaturityDate
			MBillPool bill = new MBillPool(Env.getCtx(), key, null);

			String businessStatus = bill.getBusinessStatus();
			Timestamp maturityDate = bill.getMaturityDate();

			// 业务状态必须为空且当前日期<=到期日期
			if (businessStatus != null || maturityDate == null || currentDate.after(maturityDate)) {
                return false;  
            }  
        }  
        return true;  
    }  
      
    private boolean checkEndorseCondition(List<Integer> selectedKeys) {  
        Timestamp currentDate = new Timestamp(System.currentTimeMillis());  
        for (Integer key : selectedKeys) {  
			MBillPool bill = new MBillPool(Env.getCtx(), key, null);

			// 业务状态必须为"H"且当前日期<=到期日期
			if (!"H".equals(bill.getBusinessStatus()) || bill.getMaturityDate() == null
					|| currentDate.after(bill.getMaturityDate())) {
                return false;  
            }  
        }  
        return true;  
    }  
      
    private boolean checkMaturityCondition(List<Integer> selectedKeys) {  
        Timestamp currentDate = new Timestamp(System.currentTimeMillis());  
        for (Integer key : selectedKeys) {  
			MBillPool bill = new MBillPool(Env.getCtx(), key, null);

			// 业务状态必须为"H"且当前日期>=到期日期
			if (!"H".equals(bill.getBusinessStatus()) || bill.getMaturityDate() == null
					|| currentDate.before(bill.getMaturityDate())) {
                return false;  
            }  
        }  
        return true;  
    }  

	private boolean checkIssueCondition(List<Integer> selectedKeys) {
		Timestamp currentDate = new Timestamp(System.currentTimeMillis());
		for (Integer key : selectedKeys) {
			// 查询C_Bill_Pool获取BusinessStatus和MaturityDate
			MBillPool bill = new MBillPool(Env.getCtx(), key, null);

			String businessStatus = bill.getBusinessStatus();
			Timestamp maturityDate = bill.getMaturityDate();

			// 业务状态必须为空且当前日期<=到期日期
			if (businessStatus != null || maturityDate == null || currentDate.after(maturityDate)) {
				return false;
			}
		}
		return true;
	}

	private boolean checkPaymentCondition(List<Integer> selectedKeys) {
		Timestamp currentDate = new Timestamp(System.currentTimeMillis());
		for (Integer key : selectedKeys) {
			MBillPool bill = new MBillPool(Env.getCtx(), key, null);

			// 业务状态必须为"P"且当前日期>=到期日期
			if (!"P".equals(bill.getBusinessStatus()) || bill.getMaturityDate() == null
					|| currentDate.before(bill.getMaturityDate())) {
				return false;
			}
		}
		return true;
	}

	private boolean checkDiscountCondition(List<Integer> selectedKeys) {
		Timestamp currentDate = new Timestamp(System.currentTimeMillis());
		for (Integer key : selectedKeys) {
			MBillPool bill = new MBillPool(Env.getCtx(), key, null);

			// 业务状态必须为"H"（已签收）当前日期<到期日期
			if (!"H".equals(bill.getBusinessStatus()) || bill.getMaturityDate() == null
					|| !currentDate.before(bill.getMaturityDate())) {
				return false;
			}
		}
		return true;
	}
}