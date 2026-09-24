package com.hoifu.process;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAsset;
import org.compiere.model.MAssetChange;
import org.compiere.model.MProcessPara;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;

/**
 * 变更资产使用信息（支持单资产/多资产）
 */
@org.adempiere.base.annotation.Process
public class AssetUsageChangeProcess extends SvrProcess {
	/** 变更日期 */
	private Timestamp p_ChangeDate = null;

	/** 新部门 */
	private int p_C_Activity_ID = 0;

	/** 新使用单位 */
	private int p_C_BPartner_ID = 0;

	/** 新使用人 */
	private int p_AD_User_ID = 0;

	/** 新使用地址 */
	private int p_C_BPartner_Location_ID = 0;

	/** 新使用位置 */
	private int p_M_Locator_ID = 0;

	/** 变更原因 */
	private String p_ChangeReason = null;

	/**
	 * 解析流程参数。
	 */
	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				continue;

			if (name.equals("ChangeDate"))
				p_ChangeDate = (Timestamp) para[i].getParameter();
			else if (name.equals("C_Activity_ID"))
				p_C_Activity_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_ID"))
				p_C_BPartner_ID = para[i].getParameterAsInt();
			else if (name.equals("AD_User_ID"))
				p_AD_User_ID = para[i].getParameterAsInt();
			else if (name.equals("C_BPartner_Location_ID"))
				p_C_BPartner_Location_ID = para[i].getParameterAsInt();
			else if (name.equals("M_Locator_ID"))
				p_M_Locator_ID = para[i].getParameterAsInt();
			else if (name.equals("ChangeReason"))
				p_ChangeReason = (String) para[i].getParameter();
			else
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);
		}

		// 默认值统一处理
		if (p_ChangeDate == null)
			p_ChangeDate = Env.getContextAsDate(getCtx(), Env.DATE);

		if (Util.isEmpty(p_ChangeReason, true))
			p_ChangeReason = "因使用单位变化发生变更";
	} // prepare


	@Override
	protected String doIt() throws Exception {
		try {
			return process();
		} catch (AdempiereException e) {
			// 业务校验类异常（未检测到变更 / 状态不允许等），保留具体提示，直接向上抛
			throw e;
		} catch (Exception e) {
			// 非预期异常：记录原始堆栈到系统日志，但不把细节暴露给用户
			log.log(Level.SEVERE, "AssetUsageChangeProcess.doIt error", e);
			throw new AdempiereException("当前暂时无法处理，请联系系统管理员");
		}
	}

	/**
	 * 核心处理逻辑： 1) 取出所有目标资产 2) 校验资产状态是否允许变更（整体校验，任一不满足则整批失败） 3) 逐个资产比较字段旧值/新值（不做 >0
	 * 守卫，0 也视为合法的"清空"值），先算好变更内容，不做保存 4) 若全部资产都没有任何字段变化，整批失败 5) 逐个资产更新主数据（直接赋值，包含赋值为
	 * 0 触发清空）+ 写 A_Asset_Change 变更日志（同一事务，异常则整体回滚） 6) 把每个变更字段以单独一行写入
	 * addBufferLog，供流程结果详情展示
	 */
	private String process() {
		List<Integer> assetIds = getAssetIds();
		if (assetIds.isEmpty())
			throw new AdempiereException("未选中任何资产");

		List<MAsset> assets = new ArrayList<MAsset>();
		for (int id : assetIds) {
			MAsset asset = new MAsset(getCtx(), id, get_TrxName());
			if (asset.get_ID() <= 0)
				throw new AdempiereException("资产不存在：A_Asset_ID=" + id);
			assets.add(asset);
		}

		// 数据检查1：资产状态是否允许变更（整体校验：任意一个不满足，整批全部失败）
		for (MAsset asset : assets) {
			String status = asset.getA_Asset_Status();
			if (!MAsset.A_ASSET_STATUS_New.equals(status) && !MAsset.A_ASSET_STATUS_Activated.equals(status)
					&& !MAsset.A_ASSET_STATUS_Preservation.equals(status)) {
				throw new AdempiereException("当前资产状态不允许发生变更");
			}
		}

		// 数据检查2：逐个资产比较原值和参数值，先算好变更内容，不做任何保存。
		Map<MAsset, List<String>> changeDetailMap = new LinkedHashMap<MAsset, List<String>>();
		boolean anyChanged = false;

		for (MAsset asset : assets) {
			List<String> lines = new ArrayList<String>();

			if (p_C_Activity_ID != asset.getC_Activity_ID())
				lines.add(
						buildLine("部门", getActivityValue(asset.getC_Activity_ID()), getActivityValue(p_C_Activity_ID)));

			if (p_C_BPartner_ID != asset.getC_BPartner_ID())
				lines.add(
						buildLine("使用单位", getBPartnerName(asset.getC_BPartner_ID()), getBPartnerName(p_C_BPartner_ID)));

			if (p_AD_User_ID != asset.getAD_User_ID())
				lines.add(buildLine("使用人", getUserName(asset.getAD_User_ID()), getUserName(p_AD_User_ID)));

			if (p_C_BPartner_Location_ID != asset.getC_BPartner_Location_ID())
				lines.add(buildLine("使用地址", getBPLocationName(asset.getC_BPartner_Location_ID()),
						getBPLocationName(p_C_BPartner_Location_ID)));

			if (p_M_Locator_ID != asset.getM_Locator_ID())
				lines.add(buildLine("使用位置", getLocatorValue(asset.getM_Locator_ID()), getLocatorValue(p_M_Locator_ID)));


			// 只有真正发生变化的资产才放入 map，无变化的资产直接跳过
			if (!lines.isEmpty()) {
				anyChanged = true;
				changeDetailMap.put(asset, lines);
			}
		}

		if (!anyChanged)
			throw new AdempiereException("未检测到任何变更，请至少修改一个字段");

		// 数据处理：逐个资产更新主数据 + 写变更日志（同一事务，一个异常整体回滚）。
		for (Map.Entry<MAsset, List<String>> entry : changeDetailMap.entrySet()) {
			MAsset asset = entry.getKey();
			List<String> lines = entry.getValue();

			asset.setC_Activity_ID(p_C_Activity_ID);
			asset.setC_BPartner_ID(p_C_BPartner_ID);
			asset.setAD_User_ID(p_AD_User_ID);
			asset.setC_BPartner_Location_ID(p_C_BPartner_Location_ID);
			asset.setM_Locator_ID(p_M_Locator_ID);
			asset.saveEx(get_TrxName());



			// 新增 A_Asset_Change 记录（ChangeType='UPD'）
			MAssetChange.createChange(getCtx(), asset.getAD_Org_ID(), asset.getA_Asset_ID(), "UPD",
					p_ChangeReason, p_C_Activity_ID, p_C_BPartner_ID, p_AD_User_ID, p_C_BPartner_Location_ID,
					p_M_Locator_ID, asset.getA_Asset_Status(), get_TrxName());

			// 流程返回详情：每变更一个字段输出一行日志（addBufferLog 会在流程结果的
			// 日志明细表里逐行展示，满足"有几个字段更新就输出几行日志"的要求）
			boolean isFirstLine = true;
			for (String line : lines) {
				String msg = isFirstLine ? asset.getValue() + " - " + line : "　　　　- " + line; // 用空格缩进替代资产编号，视觉上对齐
				addBufferLog(0, null, null, msg, 0, 0);
				isFirstLine = false;
			}
		}

		return "已成功更新资产信息";
	}

	/**
	 * 三段式获取资产ID：多选(getRecord_IDs) → 单选(getRecord_ID) → T_Selection 信息窗口
	 */
	private List<Integer> getAssetIds() {
		List<Integer> ids = getRecord_IDs();
		if (ids == null || ids.isEmpty()) {
			int recordId = getRecord_ID();
			if (recordId > 0) {
				ids = new ArrayList<Integer>();
				ids.add(recordId);
			} else {
				int[] selIds = DB.getIDsEx(get_TrxName(),
						"SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?", getAD_PInstance_ID());
				ids = new ArrayList<Integer>();
				if (selIds != null) {
					for (int id : selIds)
						ids.add(id);
				}
			}
		}
		return ids == null ? new ArrayList<Integer>() : ids;
	}

	/** 拼接"字段：旧值 更新为 新值"的单行描述 */
	private String buildLine(String fieldLabel, String oldValue, String newValue) {
		return fieldLabel + "：" + (oldValue == null ? "" : oldValue) + " 更新为 " + (newValue == null ? "" : newValue);
	}

	/** 按 C_Activity_ID 查询部门编码 */
	private String getActivityValue(int id) {
		if (id <= 0)
			return "";
		return DB.getSQLValueStringEx(get_TrxName(), "SELECT Value FROM C_Activity WHERE C_Activity_ID=?", id);
	}

	/** 按 C_BPartner_ID 查询业务伙伴名称 */
	private String getBPartnerName(int id) {
		if (id <= 0)
			return "";
		return DB.getSQLValueStringEx(get_TrxName(), "SELECT Name FROM C_BPartner WHERE C_BPartner_ID=?", id);
	}

	/** 按 AD_User_ID 查询用户名称 */
	private String getUserName(int id) {
		if (id <= 0)
			return "";
		return DB.getSQLValueStringEx(get_TrxName(), "SELECT Name FROM AD_User WHERE AD_User_ID=?", id);
	}

	/** 按 C_BPartner_Location_ID 查询地址名称 */
	private String getBPLocationName(int id) {
		if (id <= 0)
			return "";
		return DB.getSQLValueStringEx(get_TrxName(),
				"SELECT Name FROM C_BPartner_Location WHERE C_BPartner_Location_ID=?", id);
	}

	/** 按 M_Locator_ID 查询库位编码 */
	private String getLocatorValue(int id) {
		if (id <= 0)
			return "";
		return DB.getSQLValueStringEx(get_TrxName(), "SELECT Value FROM M_Locator WHERE M_Locator_ID=?", id);
	}
}