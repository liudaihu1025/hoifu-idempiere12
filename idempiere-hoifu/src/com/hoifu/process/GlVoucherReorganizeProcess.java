package com.hoifu.process;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.compiere.model.MAcctSchema;
import org.compiere.model.MClientInfo;
import org.compiere.model.MDocType;
import org.compiere.model.MOrg;
import org.compiere.model.MPeriod;
import org.compiere.model.MProcessPara;
import org.compiere.model.MSequence;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Util;  
  
@org.adempiere.base.annotation.Process  
public class GlVoucherReorganizeProcess extends SvrProcess {  
  
	private int p_C_AcctSchema_ID = 0;
	private int p_GL_Category_ID = 0;
	private int p_C_Period_ID = 0;
	private boolean p_IsCheckOnly = true; // 检查编号是否连续
	private boolean p_IsManualNumber = false; // 是否手动编号

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if ("C_AcctSchema_ID".equals(name)) {
				p_C_AcctSchema_ID = para.getParameterAsInt();
			} else if ("GL_Category_ID".equals(name)) {
				p_GL_Category_ID = para.getParameterAsInt();
			} else if ("C_Period_ID".equals(name)) {
				p_C_Period_ID = para.getParameterAsInt();
			} else if ("IsCheckOnly".equals(name)) {
				p_IsCheckOnly = para.getParameterAsBoolean();
			} else if ("IsManualNumber".equals(name)) {
				p_IsManualNumber = para.getParameterAsBoolean();
			} else {
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para);
			}
		}
		// 默认值
		if (p_C_AcctSchema_ID == 0) {
			p_C_AcctSchema_ID = MClientInfo.get(getCtx(), getAD_Client_ID()).getC_AcctSchema1_ID();
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (p_C_Period_ID == 0)
			throw new IllegalArgumentException("期间(C_Period_ID)为必选参数");

		// 获取期间信息，确定日期范围
		MPeriod period = MPeriod.get(getCtx(), p_C_Period_ID);
		if (period == null)
			throw new IllegalArgumentException("无效的期间");
		Timestamp startDate = period.getStartDate();
		Timestamp endDate = period.getEndDate();

		// 构建查询条件
		StringBuilder where = new StringBuilder();
		where.append("AD_Client_ID=? AND C_AcctSchema_ID=?");
		where.append(" AND DateAcct BETWEEN ? AND ?");
		List<Object> params = new ArrayList<>();
		params.add(getAD_Client_ID());
		params.add(p_C_AcctSchema_ID);
		params.add(startDate);
		params.add(endDate);

		if (p_GL_Category_ID > 0) {
			where.append(" AND GL_Category_ID=?");
			params.add(p_GL_Category_ID);
		}

		// 查询所有符合条件的凭证
		List<PO> vouchers = new Query(getCtx(), "Gl_Voucher", where.toString(), get_TrxName())
				.setParameters(params.toArray())
				.setOrderBy("AD_Org_ID, C_AcctSchema_ID, GL_Category_ID, DateAcct, SeqNo, Gl_Voucher_ID").list();

		if (vouchers.isEmpty())
			return "未找到符合条件的凭证";

		// 按 组织+账套+凭证类别+年月 分组
		// 分组key: AD_Org_ID-C_AcctSchema_ID-GL_Category_ID-yyyyMM
		SimpleDateFormat sdfGroup = new SimpleDateFormat("yyyy-MM");
		Map<String, List<PO>> groups = new LinkedHashMap<>();
		for (PO v : vouchers) {
			int orgId = v.getAD_Org_ID();
			int acctSchemaId = v.get_ValueAsInt("C_AcctSchema_ID");
			int glCategoryId = v.get_ValueAsInt("GL_Category_ID");
			Timestamp dateAcct = (Timestamp) v.get_Value("DateAcct");
			String ym = sdfGroup.format(dateAcct);
			String key = orgId + "-" + acctSchemaId + "-" + glCategoryId + "-" + ym;
			groups.computeIfAbsent(key, k -> new ArrayList<>()).add(v);
		}

		StringBuilder result = new StringBuilder();
		int totalGroups = 0;
		int totalVouchers = 0;

		if (p_IsCheckOnly) {
			// ===== 检查模式 =====
			int lineNo = 0;
			for (Map.Entry<String, List<PO>> entry : groups.entrySet()) {
				lineNo++;
				List<PO> groupVouchers = entry.getValue();
				totalVouchers += groupVouchers.size();

				// 获取分组信息用于显示
				PO first = groupVouchers.get(0);
				String groupLabel = buildGroupLabel(first, sdfGroup);

				// 获取序列配置（用于格式化显示）
				int docTypeId = first.get_ValueAsInt("C_DocType_ID");
				MDocType dt = MDocType.get(getCtx(), docTypeId);
				String decimalPattern = null;
				if (dt != null && dt.getDocNoSequence_ID() > 0) {
					MSequence seq = new MSequence(getCtx(), dt.getDocNoSequence_ID(), get_TrxName());
					decimalPattern = seq.getDecimalPattern();
				}

				// 收集所有 SeqNo
				List<Integer> seqNos = new ArrayList<>();
				for (PO v : groupVouchers) {
					Object seqObj = v.get_Value("SeqNo");
					if (seqObj != null) {
						seqNos.add(((Number) seqObj).intValue());
					}
				}
				Collections.sort(seqNos);

				int minSeq = seqNos.isEmpty() ? 0 : seqNos.get(0);
				int maxSeq = seqNos.isEmpty() ? 0 : seqNos.get(seqNos.size() - 1);

				// 检查重号
				List<String> duplicates = new ArrayList<>();
				Set<Integer> seen = new HashSet<>();
				for (int seq : seqNos) {
					if (!seen.add(seq)) {
						String formatted = formatSeqNo(seq, decimalPattern);
						if (!duplicates.contains(formatted))
							duplicates.add(formatted);
					}
				}

				// 检查断号（从1开始应该连续）
				List<String> gaps = new ArrayList<>();
				Set<Integer> seqSet = new HashSet<>(seqNos);
				// 不是从1开始也算断号
				for (int i = 1; i <= maxSeq; i++) {
					if (!seqSet.contains(i)) {
						gaps.add(formatSeqNo(i, decimalPattern));
					}
				}

				String seqRange = formatSeqNo(minSeq, decimalPattern) + "-" + formatSeqNo(maxSeq, decimalPattern);
				StringBuilder line = new StringBuilder();
				line.append(lineNo).append("、").append(groupLabel).append("，共").append(groupVouchers.size())
						.append("张凭证，").append(seqRange);

				if (duplicates.isEmpty()) {
					line.append("，无重号");
				} else {
					line.append("，").append(duplicates.size()).append("个重号（").append(String.join(",", duplicates))
							.append("）");
				}
				if (gaps.isEmpty()) {
					line.append("，无断号");
				} else {
					line.append("，").append(gaps.size()).append("个断号(").append(String.join(",", gaps)).append(")");
				}

				addLog(line.toString());
				totalGroups++;
			}
			result.append("共检查").append(totalGroups).append("行，").append(totalVouchers).append("张凭证");

		} else {
			// ===== 整理模式 =====
			int lineNo = 0;
			for (Map.Entry<String, List<PO>> entry : groups.entrySet()) {
				lineNo++;
				List<PO> groupVouchers = entry.getValue();

				// 先检查是否有断号，没有断号就不整理
				List<Integer> seqNos = new ArrayList<>();
				for (PO v : groupVouchers) {
					Object seqObj = v.get_Value("SeqNo");
					if (seqObj != null) {
						seqNos.add(((Number) seqObj).intValue());
					}
				}
				Collections.sort(seqNos);

				boolean needReorg = false;
				Set<Integer> seqSet = new HashSet<>(seqNos);
				int maxSeq = seqNos.isEmpty() ? 0 : seqNos.get(seqNos.size() - 1);
				for (int i = 1; i <= maxSeq; i++) {
					if (!seqSet.contains(i)) {
						needReorg = true;
						break;
					}
				}
				// 如果最小值不是1，也算断号
				if (!needReorg && !seqNos.isEmpty() && seqNos.get(0) != 1) {
					needReorg = true;
				}
				// 检查重号也需要整理
				if (!needReorg) {
					Set<Integer> seen = new HashSet<>();
					for (int seq : seqNos) {
						if (!seen.add(seq)) {
							needReorg = true;
							break;
						}
					}
				}

				// 获取序列配置（检查和整理都需要用于格式化显示）
				PO first = groupVouchers.get(0);
				int docTypeId = first.get_ValueAsInt("C_DocType_ID");
				MDocType dt = MDocType.get(getCtx(), docTypeId);
				MSequence seq = null;
				String decimalPattern = null;
				String prefixTemplate = null;
				String suffixTemplate = null;
				if (dt != null && dt.getDocNoSequence_ID() > 0) {
					seq = new MSequence(getCtx(), dt.getDocNoSequence_ID(), get_TrxName());
					decimalPattern = seq.getDecimalPattern();
					prefixTemplate = seq.getPrefix();
					suffixTemplate = seq.getSuffix();
				}

				String groupLabel = buildGroupLabel(first, sdfGroup);

				if (!needReorg) {
					// 无断号无重号，不整理
					String range = formatSeqNo(1, decimalPattern) + "-"
							+ formatSeqNo(groupVouchers.size(), decimalPattern);
					addLog(lineNo + "、" + groupLabel + "，共" + groupVouchers.size() + "张凭证，" + range + "，无断号，跳过整理");
					totalGroups++;
					totalVouchers += groupVouchers.size();
					continue;
				}

				groupVouchers.sort((a, b) -> {
					// 先按会计日期
					Timestamp da = (Timestamp) a.get_Value("DateAcct");
					Timestamp db = (Timestamp) b.get_Value("DateAcct");
					int cmp = da.compareTo(db);
					if (cmp != 0)
						return cmp;
					// 会计日期相同，按创建时间
					Timestamp ca = (Timestamp) a.get_Value("Created");
					Timestamp cb = (Timestamp) b.get_Value("Created");
					cmp = ca.compareTo(cb);
					if (cmp != 0)
						return cmp;
					// 创建时间也相同，按记录ID保证确定性
					return Integer.compare(a.get_ID(), b.get_ID());
				});

				// 重新编号
				int newSeqNo = 1;
				for (PO v : groupVouchers) {
					v.set_ValueNoCheck("SeqNo", newSeqNo);

					// 更新 DocumentNo：用序列配置的前缀+新编号+后缀
					if (seq != null) {
						String newDocNo = buildDocumentNo(v, prefixTemplate, suffixTemplate, decimalPattern, newSeqNo,
								get_TrxName());
						v.set_ValueNoCheck("DocumentNo", newDocNo);
					}

					v.saveEx();
					newSeqNo++;
				}

				totalVouchers += groupVouchers.size();
				totalGroups++;

				String seqRange = formatSeqNo(1, decimalPattern) + "-"
						+ formatSeqNo(groupVouchers.size(), decimalPattern);
				addLog(lineNo + "、" + groupLabel + "，共" + groupVouchers.size() + "张凭证，" + seqRange);
			}
			result.append("共整理").append(totalGroups).append("行，").append(totalVouchers).append("张凭证");
		}
		return result.toString();
	}

	private String formatSeqNo(int seqNo, String decimalPattern) {
		if (decimalPattern != null && decimalPattern.length() > 0) {
			return new DecimalFormat(decimalPattern).format(seqNo);
		}
		return String.valueOf(seqNo);
	}

	/**
	 * 构建分组标签，例如：0211-江苏海富精品 2025年中会准 总账 2026-01
	 */
	private String buildGroupLabel(PO voucher, SimpleDateFormat sdfGroup) {
		int orgId = voucher.getAD_Org_ID();
		int acctSchemaId = voucher.get_ValueAsInt("C_AcctSchema_ID");
		int glCategoryId = voucher.get_ValueAsInt("GL_Category_ID");
		Timestamp dateAcct = (Timestamp) voucher.get_Value("DateAcct");

		// 组织: Value-Name
		MOrg org = MOrg.get(getCtx(), orgId);
		String orgLabel = org.getValue() + "-" + org.getName();

		// 账套名称
		MAcctSchema as = MAcctSchema.get(getCtx(), acctSchemaId);
		String asName = as != null ? as.getName() : String.valueOf(acctSchemaId);

		// 凭证类别名称
		String glCatName = DB.getSQLValueString(get_TrxName(), "SELECT Name FROM GL_Category WHERE GL_Category_ID=?",
				glCategoryId);
		if (glCatName == null)
			glCatName = String.valueOf(glCategoryId);

		// 年月
		String ym = sdfGroup.format(dateAcct);

		return orgLabel + "  " + asName + "  " + glCatName + "  " + ym;
	}

	/**
	 * 根据序列配置构建新的 DocumentNo 参考 MSequence.getDocumentNoFromSeq() 的逻辑：prefix +
	 * formattedNumber + suffix
	 * 
	 * @param voucher        凭证PO（用于解析上下文变量）
	 * @param prefixTemplate 前缀模板，如 @AD_Org_ID<Value>@-@GL_Category_ID<Value>@-@DateDoc<yyMM>@-
	 * @param suffixTemplate 后缀模板
	 * @param decimalPattern 数字格式，如 "0000"
	 * @param seqNo          新的序号
	 * @param trxName        事务名
	 * @return 新的 DocumentNo
	 */
	private String buildDocumentNo(PO voucher, String prefixTemplate, String suffixTemplate, String decimalPattern,
			int seqNo, String trxName) {
		StringBuilder doc = new StringBuilder();

		// 解析前缀（将 /K 标记去掉后解析上下文变量）
		if (!Util.isEmpty(prefixTemplate)) {
			String prefixValue = Env.parseVariable(prefixTemplate.replaceAll("/K@", "@"), voucher, trxName, false);
			doc.append(prefixValue);
		}

		// 格式化序号
		if (!Util.isEmpty(decimalPattern)) {
			doc.append(new DecimalFormat(decimalPattern).format(seqNo));
		} else {
			doc.append(seqNo);
		}

		// 解析后缀
		if (!Util.isEmpty(suffixTemplate)) {
			String suffixValue = Env.parseVariable(suffixTemplate.replaceAll("/K@", "@"), voucher, trxName, false);
			doc.append(suffixValue);
		}

		return doc.toString();
	}
}