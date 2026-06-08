package com.hoifu.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.postgresql.util.PGobject;

public class MCashFlowRule extends PO {

	private static final long serialVersionUID = 2778211205352915279L;
	public static final String Table_Name = "C_CashFlow_Rule";
	public static final int Table_ID = getTableID(Table_Name);

	// 添加静态方法
	public static int getTableID(String tableName) {
		return MTable.getTable_ID(tableName);
	}

	public MCashFlowRule(Properties ctx, int C_CashFlow_Rule_ID, String trxName) {
		super(ctx, C_CashFlow_Rule_ID, trxName);
	}

	public MCashFlowRule(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	protected POInfo initPO(Properties ctx) {
		POInfo poi = POInfo.getPOInfo(ctx, Table_ID, get_TrxName());
		return poi;
	}

	// 标准字段
	public int getAD_Org_ID() {
		return get_ValueAsInt("AD_Org_ID");
	}

	public void setCreated(java.sql.Timestamp Created) {
		set_Value("Created", Created);
	}

	public void setCreatedBy(int CreatedBy) {
		set_Value("CreatedBy", CreatedBy);
	}

	public void setUpdated(java.sql.Timestamp Updated) {
		set_Value("Updated", Updated);
	}

	// 业务字段
	public void setName(String Name) {
		set_Value("Name", Name);
	}

	public String getName() {
		return get_ValueAsString("Name");
	}

	public void setDescription(String Description) {
		set_Value("Description", Description);
	}

	public String getDescription() {
		return get_ValueAsString("Description");
	}

	public void setC_CashFlow_Item_ID(int C_CashFlow_Item_ID) {
		set_Value("C_CashFlow_Item_ID", C_CashFlow_Item_ID);
	}

	public int getC_CashFlow_Item_ID() {
		return get_ValueAsInt("C_CashFlow_Item_ID");
	}

	public void setSequence(int Sequence) {
		set_Value("Sequence", Sequence);
	}

	public int getSequence() {
		return get_ValueAsInt("Sequence");
	}

	public void setRule_Type(String Rule_Type) {
		set_Value("Rule_Type", Rule_Type);
	}

	public String getRule_Type() {
		return get_ValueAsString("Rule_Type");
	}

	public void setCondition_Type(String Condition_Type) {
		set_Value("Condition_Type", Condition_Type);
	}

	public String getCondition_Type() {
		return get_ValueAsString("Condition_Type");
	}

	public void setConditions(String conditionsJson) {
		try {
			if (conditionsJson == null || conditionsJson.trim().isEmpty()) {
				set_Value("Conditions", null);
				return;
			}

			// 使用iDempiere标准的JSONB处理方式
			PGobject pgo = new PGobject();
			pgo.setType("jsonb");
			pgo.setValue(conditionsJson);
			set_Value("Conditions", pgo);

		} catch (SQLException e) {
			throw new RuntimeException("Failed to set JSONB conditions", e);
		}
	}

	public String getConditions() {
		Object value = get_Value("Conditions");
		if (value == null) {
			return null;
		}

		if (value instanceof PGobject) {
			PGobject pgo = (PGobject) value;
			return pgo.getValue();
		} else if (value instanceof String) {
			// 处理直接存储的字符串情况
			return (String) value;
		} else {
			return value.toString();
		}
	}

	public void setAccount_ID(int Account_ID) {
		set_Value("Account_ID", Account_ID);
	}

	public int getAccount_ID() {
		return get_ValueAsInt("Account_ID");
	}

	public void setDocBaseType(String DocBaseType) {
		set_Value("DocBaseType", DocBaseType);
	}

	public String getDocBaseType() {
		return get_ValueAsString("DocBaseType");
	}

	public void setBP_Group_ID(int BP_Group_ID) {
		set_Value("BP_Group_ID", BP_Group_ID);
	}

	public int getBP_Group_ID() {
		return get_ValueAsInt("BP_Group_ID");
	}

	public String getUUID() {
		return get_ValueAsString("C_CashFlow_Rule_UU");
	}

	@Override
	protected int get_AccessLevel() {
		return 3;
	}
}