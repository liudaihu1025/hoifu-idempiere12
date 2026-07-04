package com.hoifu.factory;

import org.adempiere.base.IModelValidatorFactory;
import org.compiere.model.ModelValidator;

import com.hoifu.validator.InOutDetailValidator;
import com.hoifu.validator.InventoryLineValidator;
import com.hoifu.validator.InvoicePayAmtValidator;
import com.hoifu.validator.InvoiceReconciliationValidator;
import com.hoifu.validator.PaymentLineValidator;
import com.hoifu.validator.ReconciliationStatusValidator;
import com.hoifu.validator.ReconciliationValidator;
import com.hoifu.validator.SalesOrderLinePriceEnteredValidator;
import com.hoifu.validator.TimeExpenseValidator;
import com.hoifu.validator.TransactionCostValidator;

public class BaseValidatorFactory implements IModelValidatorFactory {

	@Override
	public ModelValidator newModelValidatorInstance(String className) {
		if ("com.hoifu.validator.InOutDetailValidator".equals(className)) {
			return new InOutDetailValidator();
		}
		if ("com.hoifu.validator.InvoicePayAmtValidator".equals(className)) {
			return new InvoicePayAmtValidator();
		}
		if ("com.hoifu.validator.SalesOrderLinePriceEnteredValidator".equals(className)) {
			return new SalesOrderLinePriceEnteredValidator();
		}
		 // 添加 InventoryLineValidator 支持  
		if ("com.hoifu.validator.InventoryLineValidator".equals(className)) {  
	        return new InventoryLineValidator();  
	    }
		if ("com.hoifu.validator.ReconciliationStatusValidator".equals(className)){
			return new ReconciliationStatusValidator();
		}
		//新增对账单友好提示校验器
		if ("com.hoifu.validator.ReconciliationValidator".equals(className)){
			return new ReconciliationValidator();
		}
		if ("com.hoifu.validator.TimeExpenseValidator".equals(className)){
			return new TimeExpenseValidator();
		}
		if ("com.hoifu.validator.InvoiceReconciliationValidator".equals(className)){
			return new InvoiceReconciliationValidator();
		}
		//新增库存明细校验器
		if ("com.hoifu.validator.TransactionCostValidator".equals(className)){
			return new TransactionCostValidator();
		}
		// 新增支付明细校验器
		if ("com.hoifu.validator.PaymentLineValidator".equals(className)) {
			return new PaymentLineValidator();
		}

		return null;
	}
}