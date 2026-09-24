package org.libero.model;

import java.math.BigDecimal;
import java.util.Properties;

import org.compiere.model.MBPartner;
import org.compiere.model.MOrgInfo;
import org.compiere.model.MProduct;
import org.compiere.model.MResource;
import org.compiere.model.MUser;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.libero.tables.X_C_WorkTeamMember;

import com.hoifu.enums.HFSysConfigEnum;

@org.adempiere.base.Model(table = "C_WorkTeamMember")
public class MC_WorkTeamMember extends X_C_WorkTeamMember {

	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(MC_WorkTeamMember.class);

	public MC_WorkTeamMember(Properties ctx, int C_WorkTeamMember_ID, String trxName) {
		super(ctx, C_WorkTeamMember_ID, trxName);
	}

	public MC_WorkTeamMember(Properties ctx, String C_WorkTeamMember_UU, String trxName) {
		super(ctx, C_WorkTeamMember_UU, trxName);
	}

	public MC_WorkTeamMember(Properties ctx, java.sql.ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	@Override
	protected boolean beforeSave(boolean newRecord) {
		// 获取所属班组的核算方式（AccountingType 在 C_WorkTeam 表上）
		// P=计件，T=计时
		String accountingType = DB.getSQLValueString(get_TrxName(),
				"SELECT AccountingType FROM C_WorkTeam WHERE C_WorkTeam_ID=?", getC_WorkTeam_ID());
		boolean isPieceWork = "P".equals(accountingType);

		BigDecimal ratio = getpieceratio();

		if (isPieceWork) {
			// 1. 计件比例范围校验：0.00 ~ 1.00（仅计件模式）
			if (ratio != null) {
				if (ratio.compareTo(BigDecimal.ZERO) <= 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
					log.saveError("Error", "计件比例必须大于0并且小于等于1");
					return false;
				}
			}

			// 2. 班组计件比例合计不能超过 1.00（仅计件模式）
			BigDecimal total = DB.getSQLValueBD(get_TrxName(),
					"SELECT COALESCE(SUM(PieceRatio), 0) FROM C_WorkTeamMember "
							+ "WHERE C_WorkTeam_ID=? AND IsActive='Y' AND C_WorkTeamMember_ID<>?",
					getC_WorkTeam_ID(), getC_WorkTeamMember_ID());
			BigDecimal newTotal = (total != null ? total : BigDecimal.ZERO)
					.add(ratio != null ? ratio : BigDecimal.ZERO);
			if (newTotal.compareTo(BigDecimal.ONE) > 0) {
				log.saveError("Error", "所有成员的计件比例合计总数不能超过1，当前已有合计：" + total.toPlainString());
				return false;
			}
		}
		
		// 同一班组内用户不能重复
		if (newRecord || is_ValueChanged("AD_User_ID")) {
			int cnt = DB.getSQLValue(get_TrxName(),
					"SELECT COUNT(*) FROM C_WorkTeamMember "
							+ "WHERE C_WorkTeam_ID=? AND AD_User_ID=? AND IsActive='Y' " + "AND C_WorkTeamMember_ID!=?",
					getC_WorkTeam_ID(), getAD_User_ID(), getC_WorkTeamMember_ID());
			if (cnt > 0) {
				log.saveError("Error", "该用户已是班组成员，不能重复添加");
				return false;
			}
		}
		
		// 一个班组只允许一个机长
		if ("CL".equals(getPosition()) && (newRecord || is_ValueChanged("Position"))) {
			int cnt = DB.getSQLValue(get_TrxName(), "SELECT COUNT(*) FROM C_WorkTeamMember "
					+ "WHERE C_WorkTeam_ID=? AND Position='CL' AND IsActive='Y' " + "AND C_WorkTeamMember_ID!=?",
					getC_WorkTeam_ID(), getC_WorkTeamMember_ID());
			if (cnt > 0) {
				log.saveError("Error", "班组内已存在机长，每个班组只允许一个机长");
				return false;
			}
		}

		// 3. 同一用户只能有一个缺省班组
		if (isDefault()) {
			int count = DB.getSQLValue(
					get_TrxName(), "SELECT COUNT(*) FROM C_WorkTeamMember "
							+ "WHERE AD_User_ID=? AND IsDefault='Y' AND IsActive='Y' " + "AND C_WorkTeamMember_ID<>?",
					getAD_User_ID(), getC_WorkTeamMember_ID());
			if (count > 0) {
				log.saveError("Error", "该用户已有缺省班组，一个用户只能有一个缺省班组");
				return false;
			}
		}

		return true;
	}

	/**
	 * 保存成功后的联动处理： 班组成员是新员工时，若该成员"直接人工=Y"且该用户从未绑定过人力资源类型的资源，
	 * 说明其没有对应的人力资源产品/成本记录，参与报工时无法生成直接人工成本， 需要自动为其新增一条人力资源类型的 S_Resource 记录。
	 */
	@Override
	protected boolean afterSave(boolean newRecord, boolean success) {
		if (!success)
			return success;

		// 触发条件：
		// 1. 新增班组成员；或
		// 2. 修改已有成员记录时，AD_User_ID 发生变更（相当于把这个岗位换绑了新员工）；或
		// 3. 修改已有成员记录时，IsDirectLabor 由 N 改为 Y（原本不算直接人工，现在需要建资源了）
		boolean userChanged = is_ValueChanged("AD_User_ID");
		boolean directLaborChanged = is_ValueChanged("IsDirectLabor");

		if (!newRecord && !userChanged && !directLaborChanged) {
			// 既不是新增，用户也没变，直接人工标志也没变 —— 不需要重新判断，跳过
			return success;
		}

		autoCreateResourceForNewEmployee();

		return success;
	}

	/**
	 * 用户数据检查 + 自动新增资源。 触发条件： 1.当前班组成员的"直接人工"=Y，只有参与直接人工核算的成员才需要资源/成本记录）
	 * 2.当前用户未绑定过"人力资源"类型的资源。命中条件后，新建一条人力资源类型的 S_Resource 记录。 反查得到（租户级配置，不硬编码 ID）。
	 */
	private void autoCreateResourceForNewEmployee() {
		// 1. 直接人工判断：不是直接人工的成员，不需要资源/成本记录，直接跳过
		boolean isDirectLabor = "true".equals(get_ValueAsString("IsDirectLabor"));
		if (!isDirectLabor) {
			return;
		}

		int userId = getAD_User_ID();
		if (userId <= 0) {
			// 班组成员未关联具体用户，无法判断"是否新员工"，跳过
			return;
		}

		String trxName = get_TrxName();

		// 2. 读取"人力资源"类型对应的 S_ResourceType.Value（租户级配置，AD_SysConfig，避免硬编码）
		String hrTypeValue = HFSysConfigEnum.HF_HR_RESOURCE_TYPE_VALUE.getValue(getAD_Client_ID());
		if (hrTypeValue == null || hrTypeValue.isEmpty()) {
			log.warning("未配置 HF_HR_RESOURCE_TYPE_VALUE，无法确定人力资源类型，跳过自动建资源，AD_User_ID=" + userId);
			return;
		}

		// 反查该 Value 对应的 S_ResourceType_ID
		int hrResourceTypeId = DB.getSQLValue(trxName,
				"SELECT S_ResourceType_ID FROM S_ResourceType WHERE Value=? AND AD_Client_ID=? AND IsActive='Y'",
				hrTypeValue, getAD_Client_ID());
		if (hrResourceTypeId <= 0) {
			log.warning("未找到 Value=" + hrTypeValue + " 对应的人力资源类型（S_ResourceType），跳过自动建资源，AD_User_ID=" + userId);
			return;
		}

		// 3. 用户数据检查：当前用户是否已绑定过"人力资源"类型的资源
		// 注意：只按人力资源类型限定，不判断其他类型（如生产线/设备）的资源绑定情况
		int boundCnt = DB.getSQLValue(trxName,
				"SELECT COUNT(*) FROM S_Resource " + "WHERE AD_User_ID=? AND S_ResourceType_ID=? AND IsActive='Y'",
				userId, hrResourceTypeId);
		if (boundCnt > 0) {
			// 已绑定过人力资源类型的资源，不需要自动创建
			return;
		}

		// 取用户姓名作为资源名称
		String userName = DB.getSQLValueString(trxName, "SELECT Name FROM AD_User WHERE AD_User_ID=?", userId);
		if (userName == null || userName.isEmpty()) {
			userName = "User_" + userId;
		}

		// 人力资源属于虚拟资源，没有实体仓库属性，但 S_Resource.M_Warehouse_ID 是必填列，
		// 取该班组成员所属组织的默认仓库作为占位值
		int warehouseId = MOrgInfo.get(getCtx(), getAD_Org_ID(), trxName).getM_Warehouse_ID();
		if (warehouseId <= 0) {
			log.warning("组织 AD_Org_ID=" + getAD_Org_ID() + " 未配置默认仓库，无法自动创建人力资源，AD_User_ID=" + userId);
			return;
		}

		// 新建资源：MResource.saveEx() 会自动触发：
		// 1) beforeSave —— 自动创建关联的 M_Product（产品链接的资源类型=人力资源）
		// 2) afterSave —— 产品与资源双向回写关联
		// 3) 触发 ResourceEventProcessor 的 PO_AFTER_NEW 事件 —— 发送系统通知+企微机器人通知财务
		MResource resource = new MResource(getCtx(), 0, trxName);
		resource.setAD_Org_ID(getAD_Org_ID());
		resource.setValue("HR" + userId);
		resource.setName(userName); // 用户名称
		resource.setS_ResourceType_ID(hrResourceTypeId); // 资源类型
		resource.setAD_User_ID(userId); // 用户
		resource.setM_Warehouse_ID(warehouseId); // 仓库
		resource.saveEx(trxName);

		// 补充人力资源产品的 SKU/UPC：
		// SKU = 员工业务伙伴编码（AD_User.C_BPartner_ID.Value）
		// UPC = 员工的 LDAP 账号（AD_User.LDAPUser）
		MProduct product = resource.getProduct();
		if (product != null) {
			MUser adUser = MUser.get(getCtx(), userId);
			if (adUser != null) {
				int bpartnerId = adUser.getC_BPartner_ID();
				if (bpartnerId > 0) {
					MBPartner bp = MBPartner.get(getCtx(), bpartnerId);
					if (bp != null && bp.getValue() != null) {
						product.setSKU(bp.getValue());
					}
				}
				String ldapUser = adUser.getLDAPUser();
				if (ldapUser != null && !ldapUser.isEmpty()) {
					product.setUPC(ldapUser);
				}
				product.saveEx(trxName);
			}
		}

		log.info("班组成员新增（直接人工），自动创建人力资源：AD_User_ID=" + userId + "，S_Resource_ID=" + resource.getS_Resource_ID()
				+ "，C_WorkTeamMember_ID=" + getC_WorkTeamMember_ID());
	}
}