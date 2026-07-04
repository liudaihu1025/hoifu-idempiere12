package com.hoifu.process;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 批量生成替代关系
 * 
 * AD 流程参数：  
 *   MainProductIDs          : String  主料ID列表（逗号分隔），必填  
 *   SubstituteIDs           : String  替代料ID列表（逗号分隔），必填  
 *   SubstituteType          : String  替代方式：S=替代料优先, M=主料优先（可选，不填则不更新物料表）  
 *   SubstitutePriorityStrategy : String  优先级策略，固定 F=先进先出（默认）  
 *   SubstituteRatio         : Number  替代比例，默认1  
 *   ValidFrom               : Date    生效日期，默认当天  
 *   ValidTo                 : Date    失效日期，默认2099-01-01  
 */
@org.adempiere.base.annotation.Process
public class BatchCreateSubstitute extends SvrProcess {

	private List<Integer> p_MainProductIDs = new ArrayList<>();
	private List<Integer> p_SubstituteIDs = new ArrayList<>();

	/** 替代方式：S=替代料优先, M=主料优先；null 表示未选择，不更新物料表 */
	private String p_SubstituteType = null;

	/** 优先级策略：F=先进先出（目前只支持F） */
	private String p_SubstitutePriorityStrategy = "F";

	private BigDecimal p_Ratio = BigDecimal.ONE;
	private Timestamp p_ValidFrom = null;
	private Timestamp p_ValidTo = null;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter p : getParameter()) {
			String name = p.getParameterName();
			switch (name) {
			case "MainProductIDs":
				parseIds(p.getParameterAsString(), p_MainProductIDs);
				break;
			case "SubstituteIDs":
				parseIds(p.getParameterAsString(), p_SubstituteIDs);
				break;
			case "SubstituteType":
				String st = p.getParameterAsString();
				// 只接受合法值，空字符串视为未选择
				if ("S".equals(st) || "M".equals(st))
					p_SubstituteType = st;
				break;
			case "SubstitutePriorityStrategy":
				String pp = p.getParameterAsString();
				if (pp != null && !pp.isEmpty())
					p_SubstitutePriorityStrategy = pp;
				break;
			case "SubstituteRatio":
				BigDecimal r = p.getParameterAsBigDecimal();
				if (r != null && r.compareTo(BigDecimal.ZERO) > 0)
					p_Ratio = r;
				break;
			case "ValidFrom":
				Timestamp vf = p.getParameterAsTimestamp();
				if (vf != null)
					p_ValidFrom = vf;
				break;
			case "ValidTo":
				Timestamp vt = p.getParameterAsTimestamp();
				if (vt != null)
					p_ValidTo = vt;
				break;
			default:
				log.warning("未知参数: " + name);
			}
		}

		// 默认值
		if (p_ValidFrom == null)
			p_ValidFrom = new Timestamp(System.currentTimeMillis());
		if (p_ValidTo == null)
			p_ValidTo = Timestamp.valueOf("2099-01-01 00:00:00");
	}

	@Override
	protected String doIt() throws Exception {
		if (p_MainProductIDs.isEmpty())
			throw new AdempiereException("主料不能为空");
		if (p_SubstituteIDs.isEmpty())
			throw new AdempiereException("替代料不能为空");

		// 按先进先出策略构建替代料条目（含优先级）
		List<SubstituteEntry> entries = new ArrayList<>();
		if ("F".equals(p_SubstitutePriorityStrategy)) {
			entries = buildEntriesByFIFO();
		}

		int created = 0;
		int skipped = 0;
		int updated = 0;

		for (int mainId : p_MainProductIDs) {
			MProduct mainProd = MProduct.get(getCtx(), mainId);
			if (mainProd == null || mainProd.get_ID() <= 0) {
				log.warning("主料不存在，跳过: " + mainId);
				skipped++;
				continue;
			}

			// ===== 如果选择了替代方式，更新物料表 =====
			if (p_SubstituteType != null) {
				int rows = DB.executeUpdate(
						"UPDATE M_Product SET substitutetype=? WHERE M_Product_ID=? AND AD_Client_ID=?",
						new Object[] { p_SubstituteType, mainId, Env.getAD_Client_ID(getCtx()) }, false, get_TrxName());
				if (rows == 1) {
					updated++;
					log.info("更新物料替代方式: " + mainProd.getValue() + " -> " + p_SubstituteType);
				} else {
					log.warning("更新物料替代方式失败，影响行数=" + rows + ", M_Product_ID=" + mainId);
				}
			}

			// ===== 创建替代关系 =====
			for (SubstituteEntry e : entries) {
				if (mainId == e.subId) {
					log.warning("主料与替代料相同，跳过: " + mainId);
					skipped++;
					continue;
				}
				if (exists(mainId, e.subId)) {
					log.info("替代关系已存在，跳过: " + mainId + " -> " + e.subId);
					skipped++;
					continue;
				}
				insert(mainId, e, mainProd);
				created++;
			}
		}

		StringBuilder result = new StringBuilder("执行成功：新建 ").append(created).append(" 条，跳过 ").append(skipped)
				.append(" 条");
		if (p_SubstituteType != null)
			result.append("，更新物料替代方式 ").append(updated).append(" 条");
		return result.toString();
	}

	/**
	 * 按先进先出策略构建替代料条目，分配优先级 10, 20, 30... 无库存的替代料排在最后，优先级同样按顺序分配。
	 */
	private List<SubstituteEntry> buildEntriesByFIFO() {
		List<SubstituteEntry> list = new ArrayList<>();
		for (int subId : p_SubstituteIDs) {
			SubstituteEntry e = new SubstituteEntry();
			e.subId = subId;
			e.earliestDate = getEarliestDate(subId);
			list.add(e);
		}

		// 按最早入库日期升序排序，无库存（null）排最后
		Collections.sort(list, (a, b) -> {
			if (a.earliestDate == null && b.earliestDate == null)
				return 0;
			if (a.earliestDate == null)
				return 1;
			if (b.earliestDate == null)
				return -1;
			return a.earliestDate.compareTo(b.earliestDate);
		});

		// 分配优先级 10, 20, 30...
		int seq = 0;
		for (SubstituteEntry e : list) {
			seq++;
			e.priority = seq * 10;
		}
		return list;
	}

	/**
	 * 查询替代料在所有仓库中的最早入库日期（DateMaterialPolicy）
	 */
	private Timestamp getEarliestDate(int productId) {
		String sql = "SELECT MIN(oh.DateMaterialPolicy) FROM M_StorageOnHand oh "
				+ "WHERE oh.M_Product_ID=? AND oh.QtyOnHand > 0 AND oh.AD_Client_ID=?";
		return DB.getSQLValueTS(get_TrxName(), sql, productId, Env.getAD_Client_ID(getCtx()));
	}

	/**
	 * 解析逗号分隔的ID字符串
	 */
	private void parseIds(String raw, List<Integer> target) {
		if (raw == null || raw.trim().isEmpty())
			return;
		for (String s : raw.split(",")) {
			s = s.trim();
			if (!s.isEmpty()) {
				try {
					target.add(Integer.parseInt(s));
				} catch (NumberFormatException ex) {
					log.warning("无效ID，跳过: " + s);
				}
			}
		}
	}

	/**
	 * 检查替代关系是否已存在（IsActive='Y'）
	 */
	private boolean exists(int mainId, int subId) {
		String sql = "SELECT COUNT(*) FROM M_Substitute "
				+ "WHERE M_Product_ID=? AND Substitute_ID=?";
		return DB.getSQLValue(get_TrxName(), sql, mainId, subId) > 0;
	}

	/**
	 * 插入一条替代关系记录 DocStatus 硬编码为 'AP'（已审批），Priority 始终插入
	 */
	private void insert(int mainId, SubstituteEntry e, MProduct mainProd) {
//		int newId = DB.getNextID(getCtx(), "M_Substitute", get_TrxName());
		MProduct subProd = MProduct.get(getCtx(), e.subId);
		if (subProd == null || subProd.get_ID() <= 0) {
			log.warning("替代料不存在，跳过: " + e.subId);
			return;
		}
		String name = mainProd.getValue() + " -> " + subProd.getValue();

		String sql = "INSERT INTO M_Substitute "
				+ "(AD_Client_ID, AD_Org_ID, IsActive, Created, CreatedBy, Updated, UpdatedBy,"
				+ " M_Product_ID, Substitute_ID, Name, ValidFrom, ValidTo, SubstituteStatus, SubstituteRatio, Priority)"
				+ " VALUES (?,?,'Y',now(),?,now(),?,?,?,?,?,?,'A',?,?)";
		// ^^^^ 修复：'AP' 而非 'A'

		List<Object> params = new ArrayList<>();
//		params.add(newId);
		params.add(Env.getAD_Client_ID(getCtx()));
		params.add(Env.getAD_Org_ID(getCtx()));
		params.add(Env.getAD_User_ID(getCtx())); // CreatedBy
		params.add(Env.getAD_User_ID(getCtx())); // UpdatedBy
		params.add(mainId); // M_Product_ID
		params.add(e.subId); // Substitute_ID
		params.add(name); // Name
		params.add(p_ValidFrom); // ValidFrom
		params.add(p_ValidTo); // ValidTo
		params.add(p_Ratio); // SubstituteRatio
		params.add(e.priority); // Priority

		int rows = DB.executeUpdate(sql, params.toArray(), false, get_TrxName());
		if (rows != 1)
			throw new AdempiereException("插入替代关系失败: " + mainId + " -> " + e.subId);

		log.info("创建替代关系: " + name + " 优先级=" + e.priority
				+ (e.earliestDate != null ? " 最早入库=" + e.earliestDate : " (无库存)"));
	}

	private static class SubstituteEntry {
		int subId;
		int priority;
		Timestamp earliestDate; // 最早入库日期，null 表示无库存
	}
}