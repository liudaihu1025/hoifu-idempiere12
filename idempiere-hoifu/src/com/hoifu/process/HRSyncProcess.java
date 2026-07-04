package com.hoifu.process;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MActivity;
import org.compiere.model.MBPGroup;
import org.compiere.model.MBPartner;
import org.compiere.model.MClientInfo;
import org.compiere.model.MOrg;
import org.compiere.model.MUser;
import org.compiere.model.Query;
import org.compiere.model.X_C_Job;
import org.compiere.model.X_C_JobCategory;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hoifu.utils.HRTokenUtils;

/**
 * HR系统数据同步流程 同步顺序: 职位类别(C_JobCategory) → 职位(C_Job) → 部门(C_Activity) →
 * 员工(C_BPartner) 每日凌晨1点由 AD_Scheduler 触发 配置项读取自 AD_SysConfig: HR_API_BASE_URL /
 * HR_API_USER_ID / HR_API_PASSWORD / HR_API_COMPANY_ID_JS
 * 
 * @author liudaihu
 */
@org.adempiere.base.annotation.Process
public class HRSyncProcess extends SvrProcess {

	private static final SimpleDateFormat SDF_FULL = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy/MM/dd");

	// 统计计数
	private int deptInsert, deptUpdate;
	private int catInsert;
	private int jobInsert, jobUpdate;
	private int empInsert, empUpdate;

	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		int clientId = getAD_Client_ID();

		// 每次重新获取 Token（不缓存）
		String token = HRTokenUtils.fetchToken(clientId);

		syncJobs(token, clientId);
		syncDepartments(token, clientId);
		syncEmployees(token, clientId);

		return String.format("同步完成 | 部门[新增:%d 更新:%d] | 职位类别[新增:%d] | 职位[新增:%d 更新:%d] | 员工[新增:%d 更新:%d]", deptInsert,
				deptUpdate, catInsert, jobInsert, jobUpdate, empInsert, empUpdate);
	}

	// =========================================================
	// 1. 职位类别 + 职位同步（C_JobCategory + C_Job）
	// =========================================================
	private void syncJobs(String token, int clientId) throws Exception {
		log.info("开始同步职位类别和职位...");
		JSONArray jobs = HRTokenUtils.fetchJobs(token, clientId);
		Timestamp syncNow = now();

		// 预加载现有职位类别（Name → ID）
		Map<String, Integer> catMap = new HashMap<>();
		List<X_C_JobCategory> existingCats = new Query(getCtx(), X_C_JobCategory.Table_Name,
				"AD_Client_ID=? AND IsActive='Y'", get_TrxName()).setParameters(clientId).list();
		for (X_C_JobCategory c : existingCats)
			catMap.put(c.getName(), c.getC_JobCategory_ID());

		// 预加载现有职位（Value → PO）
		Map<String, X_C_Job> jobMap = new HashMap<>();
		List<X_C_Job> existingJobs = new Query(getCtx(), X_C_Job.Table_Name, "AD_Client_ID=? AND Value IS NOT NULL",
				get_TrxName()).setParameters(clientId).list();
		for (X_C_Job j : existingJobs)
			jobMap.put((String) j.get_Value("Value"), j);

		for (int i = 0; i < jobs.length(); i++) {
			JSONObject job = jobs.getJSONObject(i);
			String jobCode = job.optString("职别代码", "").trim();
			String jobName = job.optString("职别名称", "").trim();
			String catName = job.optString("职别类型", "默认").trim();
			boolean enabled = job.optBoolean("是否启用", true);
			Timestamp hrCreate = parseTs(job.optString("建立日期", ""));
			Timestamp hrUpdate = parseTs(job.optString("修改日期", ""));

			if (jobCode.isEmpty() || jobName.isEmpty())
				continue;

			// 1.1 确保职位类别存在
			int catId = getOrCreateJobCategory(catName, catMap, clientId, syncNow);

			// 1.2 新增或更新职位
			X_C_Job existing = jobMap.get(jobCode);
			if (existing == null) {
				X_C_Job newJob = new X_C_Job(getCtx(), 0, get_TrxName());
				newJob.setAD_Org_ID(0);
				newJob.setName(jobName);
				newJob.set_ValueOfColumn("Value", jobCode);
				newJob.setC_JobCategory_ID(catId);
				newJob.setIsEmployee(false);
				newJob.setIsActive(enabled);
				newJob.set_ValueOfColumn("HR_SyncDate", syncNow);
				newJob.set_ValueOfColumn("HR_CreateDate", hrCreate);
				newJob.set_ValueOfColumn("HR_UpdateDate", hrUpdate);
				newJob.saveEx();
				jobMap.put(jobCode, newJob);
				jobInsert++;
				log.info("新增职位: " + jobCode + " - " + jobName);
			} else {
				existing.setName(jobName);
				existing.setC_JobCategory_ID(catId);
				existing.setIsActive(enabled);
				existing.set_ValueOfColumn("HR_SyncDate", syncNow);
				existing.set_ValueOfColumn("HR_CreateDate", hrCreate);
				existing.set_ValueOfColumn("HR_UpdateDate", hrUpdate);
				existing.saveEx();
				jobUpdate++;
			}
		}
		log.info("职位同步完成");
	}

	private int getOrCreateJobCategory(String catName, Map<String, Integer> catMap, int clientId, Timestamp syncNow) {
		if (catName.isEmpty())
			catName = "默认";
		Integer id = catMap.get(catName);
		if (id != null)
			return id;

		X_C_JobCategory cat = new X_C_JobCategory(getCtx(), 0, get_TrxName());
		cat.setAD_Org_ID(0);
		cat.setName(catName);
		cat.set_ValueOfColumn("HR_SyncDate", syncNow);
		cat.saveEx();
		catMap.put(catName, cat.getC_JobCategory_ID());
		catInsert++;
		log.info("新增职位类别: " + catName);
		return cat.getC_JobCategory_ID();
	}

	// =========================================================
	// 2. 部门同步（C_Activity）
	// =========================================================
	private void syncDepartments(String token, int clientId) throws Exception {
		log.info("开始同步部门...");
		JSONArray depts = HRTokenUtils.fetchDepartments(token, clientId);
		Timestamp syncNow = now();

		// 预加载现有 C_Activity（Value → MActivity）
		Map<String, MActivity> actMap = new HashMap<>();
		List<MActivity> existingActs = new Query(getCtx(), MActivity.Table_Name, "AD_Client_ID=? AND Value IS NOT NULL",
				get_TrxName()).setParameters(clientId).list();
		for (MActivity a : existingActs)
			actMap.put(a.getValue(), a);

		// 第一遍：新增/更新所有 C_Activity
		Map<String, Integer> codeToActId = new HashMap<>();
		for (int i = 0; i < depts.length(); i++) {
			JSONObject dept = depts.getJSONObject(i);
			String deptCode = dept.optString("部门代码", "").trim();
			String deptName = dept.optString("部门名称", "").trim();
			String deptDesc = dept.optString("部门结构", "").trim();
			Timestamp hrCreate = parseTs(dept.optString("建立日期", ""));
			Timestamp hrUpdate = parseTs(dept.optString("修改日期", ""));
			int level = dept.optInt("层级", 1);

			if (deptCode.isEmpty() || deptName.isEmpty())
				continue;

			boolean isSummary = (level == 0); // 层级=0 → 汇总项
			MActivity act = actMap.get(deptCode);
			if (act == null) {
				// 新增：MActivity.saveEx() 自动触发 insert_Tree(TREETYPE_Activity)
				act = new MActivity(getCtx(), 0, get_TrxName());
				act.setAD_Org_ID(0);
				act.setValue(deptCode);
				act.setName(deptName);
				act.setDescription(deptDesc);
				act.setIsSummary(isSummary); // 根据层级动态设置，不再硬编码 false
				act.set_ValueOfColumn("HR_SyncDate", syncNow);
				act.set_ValueOfColumn("HR_CreateDate", hrCreate);
				act.set_ValueOfColumn("HR_UpdateDate", hrUpdate);
				act.saveEx();
				actMap.put(deptCode, act);
				deptInsert++;
				log.info("新增部门: " + deptCode + " - " + deptName);
			} else {
				act.setName(deptName);
				act.setDescription(deptDesc);
				act.setIsSummary(isSummary); // 根据层级动态设置，不再硬编码 false
				act.set_ValueOfColumn("HR_SyncDate", syncNow);
				act.set_ValueOfColumn("HR_CreateDate", hrCreate);
				act.set_ValueOfColumn("HR_UpdateDate", hrUpdate);
				act.saveEx();
				deptUpdate++;
			}
			codeToActId.put(deptCode, act.getC_Activity_ID());
		}

		// 第二遍：更新 AD_TreeNode 父子关系   暂时不用HR系统的层级关系
//		int rootActId = findRootActivity(clientId);
//		updateDepartmentTree(depts, codeToActId, rootActId, clientId);
		log.info("部门同步完成");
	}

	private int findRootActivity(int clientId) {
		String sql = "SELECT C_Activity_ID FROM C_Activity "
				+ "WHERE AD_Client_ID=? AND Name='海富集团' AND IsSummary='Y' AND IsActive='Y' "
				+ "ORDER BY C_Activity_ID LIMIT 1";
		int id = DB.getSQLValue(get_TrxName(), sql, clientId);
		if (id <= 0)
			log.warning("未找到'江苏海富'汇总节点，顶层部门将挂在树根(0)");
		return id;
	}

	private void updateDepartmentTree(JSONArray depts, Map<String, Integer> codeToActId, int rootActId, int clientId) {
		MClientInfo ci = MClientInfo.get(getCtx(), clientId);
		int treeId = ci.getAD_Tree_Activity_ID();
		if (treeId <= 0) {
			log.warning("未找到Activity树，跳过树形关系更新");
			return;
		}
		for (int i = 0; i < depts.length(); i++) {
			JSONObject dept = depts.getJSONObject(i);
			String deptCode = dept.optString("部门代码", "").trim();
			String parentCode = dept.optString("上级部门", "").trim();
			Integer nodeId = codeToActId.get(deptCode);
			if (nodeId == null)
				continue;

			int parentNodeId = (!parentCode.isEmpty() && codeToActId.containsKey(parentCode))
					? codeToActId.get(parentCode)
					: (rootActId > 0 ? rootActId : 0);

			DB.executeUpdateEx("UPDATE AD_TreeNode SET Parent_ID=?, Updated=NOW() WHERE AD_Tree_ID=? AND Node_ID=?",
					new Object[] { parentNodeId, treeId, nodeId }, get_TrxName());
		}
	}

	// =========================================================
	// 3. 员工同步（C_BPartner，IsEmployee='Y'）
	// =========================================================
	private void syncEmployees(String token, int clientId) throws Exception {
		log.info("开始同步员工...");
		JSONArray emps = HRTokenUtils.fetchEmployees(token, clientId);
		Timestamp syncNow = now();

		// 预加载 AD_Org（公司名称/简称 → AD_Org_ID）
		// 优先用 Name 匹配，也支持 Value 匹配
		Map<String, Integer> orgNameToId = new HashMap<>();
		for (MOrg org : MOrg.getOfClient(clientId)) {
			orgNameToId.put(org.getName(), org.getAD_Org_ID());
		}

		// 预加载 C_Activity（Value → C_Activity_ID）
		Map<String, Integer> actCodeToId = new HashMap<>();
		List<MActivity> acts = new Query(getCtx(), MActivity.Table_Name, "AD_Client_ID=? AND Value IS NOT NULL",
				get_TrxName()).setParameters(clientId).list();
		for (MActivity a : acts)
			actCodeToId.put(a.getValue(), a.getC_Activity_ID());

		// 预加载 C_Job（Value → C_Job_ID）
		Map<String, Integer> jobCodeToId = new HashMap<>();
		List<X_C_Job> cJobs = new Query(getCtx(), X_C_Job.Table_Name, "AD_Client_ID=? AND Value IS NOT NULL",
				get_TrxName()).setParameters(clientId).list();
		for (X_C_Job j : cJobs)
			jobCodeToId.put((String) j.get_Value("Value"), j.getC_Job_ID());

		// 预加载现有员工 C_BPartner（Value=员工码 → MBPartner）
		Map<String, MBPartner> bpMap = new HashMap<>();
		List<MBPartner> existingBPs = new Query(getCtx(), MBPartner.Table_Name, "AD_Client_ID=? AND IsEmployee='Y'",
				get_TrxName()).setParameters(clientId).list();
		for (MBPartner bp : existingBPs)
			bpMap.put(bp.getValue(), bp);

		// 查找名称包含"集团"的 BP 组（取第一条），找不到则用默认组
		int bpGroupId = DB.getSQLValue(
				get_TrxName(), "SELECT C_BP_Group_ID FROM C_BP_Group "
						+ "WHERE AD_Client_ID=? AND Name = '集团内' AND IsActive='Y' " + "ORDER BY C_BP_Group_ID LIMIT 1",
				clientId);
		if (bpGroupId <= 0) {
			MBPGroup defaultGroup = MBPGroup.getDefault(getCtx());
			if (defaultGroup == null)
				throw new AdempiereException("未找到BP组（集团），请检查 C_BP_Group 表");
			bpGroupId = defaultGroup.getC_BP_Group_ID();
			log.warning("未找到名称含'集团'的BP组，使用默认BP组: " + defaultGroup.getName());
		}

		for (int i = 0; i < emps.length(); i++) {
			JSONObject emp = emps.getJSONObject(i);

			// 审核状态=Y 才同步
			String auditStatus = emp.optString("审核", "N").trim();
			if (!"Y".equalsIgnoreCase(auditStatus))
				continue;

			String companyName = emp.optString("公司名称", "").trim(); // 江苏海富包装科技有限公司
			String empCode = emp.optString("员工码", "").trim();
			String empName = emp.optString("姓名", "").trim();
			String deptCode = emp.optString("部门代码", "").trim();
			String jobCode = emp.optString("职别代码", "").trim();
			String phone = emp.optString("本人联系电话", "").trim();
			String workStatus = emp.optString("在职状态", "").trim();
			String employmentStatus = emp.optString("录用状态", "").trim();
			String quitDate = emp.optString("离职日期", "").trim();
			String auditDate = emp.optString("审核日期", "").trim();
			Timestamp hrCreate = parseTs(emp.optString("建立日期", ""));
			Timestamp hrUpdate = parseTs(emp.optString("修改日期", ""));

			if (empCode.isEmpty() || empName.isEmpty())
				continue;

			// 编码转 ID
			Integer actId = actCodeToId.get(deptCode);
			Integer cJobId = jobCodeToId.get(jobCode);

			// 在职/离职判断
			boolean isActive = (quitDate == null || quitDate.isEmpty() || "null".equalsIgnoreCase(quitDate));
			Timestamp auditTs = parseTs(auditDate);

			// 按员工码（Value）查
			MBPartner bp = bpMap.get(empCode);

			if (bp == null) {
				// 新增
				bp = new MBPartner(getCtx(), 0, get_TrxName());
				bp.setAD_Org_ID(0); // 默认全部组织

				// 根据公司名称查找组织
				Integer orgId = orgNameToId.get(companyName);
				if (orgId != null && orgId > 0) {
					bp.setAD_Org_ID(orgId);
					log.info("员工[" + empCode + "]匹配组织: " + companyName + " → AD_Org_ID=" + orgId);
				}
				bp.setValue(empCode);
				bp.setName(empName);
				bp.setIsEmployee(true);
				bp.setIsCustomer(false);
				bp.setIsVendor(false);
				bp.setC_BP_Group_ID(bpGroupId); // beforeSave 会自动调用 setBPGroup(grp)
				bp.setIsActive(isActive);
				setEmpCustomFields(bp, actId, cJobId, phone, workStatus, employmentStatus, auditTs, auditStatus,
						syncNow, hrCreate, hrUpdate);
				bp.saveEx(); // afterSave 自动 insert_Tree + insert_Accounting
				bpMap.put(empCode, bp);
				empInsert++;
				log.info("新增员工: " + empCode + " - " + empName);
			} else {
				// 更新
				bp.setName(empName);
				bp.setIsActive(isActive);
				setEmpCustomFields(bp, actId, cJobId, phone, workStatus, employmentStatus, auditTs, auditStatus,
						syncNow, hrCreate, hrUpdate);
				bp.saveEx();
				// 离职时，同步禁用关联的 AD_User（联系人）  
			    if (!isActive) {  
			        deactivateBPUsers(bp.getC_BPartner_ID(), clientId);  
			    }  
				empUpdate++;
			}
		}
		log.info("员工同步完成");
	}

	/**
	 * 员工离职时，将关联的 AD_User（联系人）设置为无效
	 */
	private void deactivateBPUsers(int bpId, int clientId) {
		try {
			List<MUser> users = new Query(getCtx(), MUser.Table_Name,
					"C_BPartner_ID=? AND AD_Client_ID=? AND IsActive='Y'", get_TrxName()).setParameters(bpId, clientId)
					.list();
			for (MUser user : users) {
				user.setIsActive(false);
				user.saveEx();
				log.info("离职员工联系人已禁用: AD_User_ID=" + user.getAD_User_ID() + " Name=" + user.getName());
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "禁用 AD_User 失败: C_BPartner_ID=" + bpId, e);
		}
	}

	/**
	 * 设置 C_BPartner 上的 HR 自定义字段
	 */
	private void setEmpCustomFields(MBPartner bp, Integer actId, Integer cJobId, String phone, String workStatus,
			String employmentStatus, Timestamp auditTs, String auditStatus, Timestamp syncNow, Timestamp hrCreate,
			Timestamp hrUpdate) {
		if (actId != null)
			bp.set_ValueOfColumn("C_Activity_ID", actId);
		if (cJobId != null)
			bp.set_ValueOfColumn("C_Job_ID", cJobId);
		if (!phone.isEmpty())
			bp.set_ValueOfColumn("HR_Phone", phone);
		if (!workStatus.isEmpty())
			bp.set_ValueOfColumn("HR_WorkStatus", workStatus);
		bp.set_ValueOfColumn("HR_employmentstatus", employmentStatus);
		if (auditTs != null)
			bp.set_ValueOfColumn("HR_AuditDate", auditTs);
		bp.set_ValueOfColumn("HR_AuditStatus", auditStatus.isEmpty() ? "Y" : auditStatus);
		bp.set_ValueOfColumn("HR_SyncDate", syncNow);
		if (hrCreate != null)
			bp.set_ValueOfColumn("HR_CreateDate", hrCreate);
		if (hrUpdate != null)
			bp.set_ValueOfColumn("HR_UpdateDate", hrUpdate);
	}

	// =========================================================
	// 工具方法
	// =========================================================
	private Timestamp parseTs(String s) {
		if (s == null || s.isEmpty() || "null".equalsIgnoreCase(s))
			return null;
		try {
			String clean = s.contains(".") ? s.substring(0, s.lastIndexOf('.')) : s;
			return new Timestamp(SDF_FULL.parse(clean).getTime());
		} catch (Exception e) {
			try {
				return new Timestamp(SDF_DATE.parse(s).getTime());
			} catch (Exception ex) {
				return null;
			}
		}
	}

	private Timestamp now() {
		return new Timestamp(System.currentTimeMillis());
	}
}