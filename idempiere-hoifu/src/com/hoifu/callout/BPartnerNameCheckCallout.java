package com.hoifu.callout;

import java.util.Properties;
import java.util.regex.Pattern;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.Query;
import org.compiere.util.Env;
import org.compiere.util.Msg;
import org.compiere.util.Util;

/**
 * C_BPartner.Name / IsVendor / IsCustomer 变化后， 仅当该记录为供应商或客户时，检查是否与其它供应商/客户重名，
 * 若存在则仅提示用户，不做任何字段赋值、不阻止保存。
 */
@Callout(tableName = "C_BPartner", columnName = { "Name", "IsVendor", "IsCustomer" })
public class BPartnerNameCheckCallout implements IColumnCallout {

	// 非法字符规则：空格、英文半角括号
	private static final Pattern INVALID_CHARS = Pattern.compile("[\\s()]");

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {

		Object nameObj = mTab.getValue("Name");
		String name = (nameObj == null) ? null : nameObj.toString();
		if (Util.isEmpty(name, true)) {
			return "";
		}

		// 规则1：非法字符校验（不允许空格、英文半角括号），对所有往来单位生效
		if (INVALID_CHARS.matcher(name).find()) {
			return Msg.getMsg(Env.getCtx(), "BPartnerInvalidNameChars");
		}

		//规则2： 仅当供应商或客户标识为 Y 时才校验
		boolean isVendor = mTab.getValueAsBoolean("IsVendor");
		boolean isCustomer = mTab.getValueAsBoolean("IsCustomer");
		if (!isVendor && !isCustomer) {
			return "";
		}

		// 当前记录自身的ID，避免跟自己比较
		Integer currentID = (Integer) mTab.getValue("C_BPartner_ID");

		String whereClause = "Name=? AND AD_Client_ID=? AND (IsVendor='Y' OR IsCustomer='Y') ";
		Query query = new Query(ctx, MBPartner.Table_Name, whereClause, null).setParameters(name,
				Env.getAD_Client_ID(ctx));

		MBPartner existing = query.first();

		// 排除自己本身
		if (existing != null && currentID != null && existing.get_ID() == currentID.intValue()) {
			return "";
		}

		if (existing != null && existing.get_ID() != 0) {
			return Msg.getMsg(Env.getCtx(), "BPartnerDuplicateName", new Object[] { name });
		}

		return "";
	}
}