package com.hoifu.config;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

/**
 * 资产负债表配置管理类 从BS_Config表中读取项目定义和科目映射
 */
public class BalanceSheetConfig {

	private static final CLogger log = CLogger.getCLogger(BalanceSheetConfig.class);

	/**
	 * 获取资产负债表项目定义 优先从BS_Config表读取，回退到默认配置
	 */
	public static String[][] getBalanceSheetItems() {
		List<String[]> items = loadFromConfigTable();
		if (!items.isEmpty()) {
			return items.stream().sorted(Comparator.comparingInt(a -> Integer.parseInt(a[0]))).toArray(String[][]::new);
		}

		// 回退到默认配置
		return getDefaultBalanceSheetItems();
	}

	/**
	 * 从BS_Config表读取项目配置
	 */
	private static List<String[]> loadFromConfigTable() {
		List<String[]> items = new ArrayList<>();
		String sql = "SELECT lineno, leftname, rightname, leftaccounttype, rightaccounttype "
				+ "FROM adempiere.bs_config WHERE ad_client_id = ? AND isactive = 'Y' ORDER BY lineno";

		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String[] item = new String[5];
					item[0] = String.valueOf(rs.getInt("lineno"));
					// 关键修改：将NULL值转换为空字符串，保持与原配置一致
					item[1] = rs.getString("leftname") != null ? rs.getString("leftname") : "";
					item[2] = rs.getString("rightname") != null ? rs.getString("rightname") : "";
					item[3] = rs.getString("leftaccounttype") != null ? rs.getString("leftaccounttype") : "";
					item[4] = rs.getString("rightaccounttype") != null ? rs.getString("rightaccounttype") : "";
					items.add(item);
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "从配置表读取失败", e);
		}

		return items;
	}

	/**
	 * 获取左侧资产类公式映射 优先从BS_Config表读取
	 */
	public static Map<String, String> getLeftFormulaMap() {
		Map<String, String> map = new LinkedHashMap<>();
		String sql = "SELECT leftname, leftformula FROM adempiere.bs_config "
				+ "WHERE ad_client_id = ? AND isactive = 'Y' AND leftformula IS NOT NULL ORDER BY lineno";

		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String name = rs.getString("leftname");
					String formula = rs.getString("leftformula");
					if (name != null && !name.trim().isEmpty() && formula != null && !formula.trim().isEmpty()) {
						map.put(name.replaceAll("^[\\s\\u3000]+|[\\s\\u3000]+$", ""), formula.trim());
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "从配置表读取左侧公式失败", e);
		}

		if (!map.isEmpty()) {
			return map;
		}

		// 回退到默认配置
		return getDefaultLeftFormulaMap();
	}

	/**
	 * 获取右侧负债权益类公式映射 优先从BS_Config表读取
	 */
	public static Map<String, String> getRightFormulaMap() {
		Map<String, String> map = new LinkedHashMap<>();
		String sql = "SELECT rightname, rightformula FROM adempiere.bs_config "
				+ "WHERE ad_client_id = ? AND isactive = 'Y' AND rightformula IS NOT NULL ORDER BY lineno";

		try (PreparedStatement ps = DB.prepareStatement(sql, null)) {
			ps.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					String name = rs.getString("rightname");
					String formula = rs.getString("rightformula");
					if (name != null && !name.trim().isEmpty() && formula != null && !formula.trim().isEmpty()) {
						map.put(name.replaceAll("^[\\s\\u3000]+|[\\s\\u3000]+$", ""), formula.trim());
					}
				}
			}
		} catch (Exception e) {
			log.log(Level.SEVERE, "从配置表读取右侧公式失败", e);
		}

		if (!map.isEmpty()) {
			return map;
		}

		// 回退到默认配置
		return getDefaultRightFormulaMap();
	}

	/**
	 * 默认资产负债表项目定义
	 * 
	 * @Title: getDefaultBalanceSheetItems
	 * @return
	 * @return String[][]
	 */
	private static String[][] getDefaultBalanceSheetItems() {
		return new String[][] {
				// 资产类
				{ "1", "流动资产:", "流动负债:", "A", "L" }, { "2", "货币资金", "短期借款", "A", "L" },
				{ "3", "　　现金", "应付票据", "A", "L" }, { "4", "　　银行存款", "应付账款", "A", "L" },
				{ "5", "　　短期投资", "　　应付账款-集团内", "A", "L" }, { "6", "应收票据", "　　应付账款-集团外", "A", "L" },
				{ "7", "应收账款", "预收账款", "A", "L" }, { "8", "　　应收账款-集团内", "应付职工薪酬", "A", "L" },
				{ "9", "　　应收账款-集团外", "应交税费", "A", "L" }, { "10", "预付账款", "应付利息", "A", "L" },
				{ "11", "应收股利", "应付股利", "A", "L" }, { "12", "应收利息", "其他应付款", "A", "L" },
				{ "13", "其他应收款", "　　其他应付款-集团内", "A", "L" }, { "14", "　　其他应收款-集团内", "　　其他应付款-集团外", "A", "L" },
				{ "15", "　　其他应收款-集团外", "其他流动负债", "A", "L" }, { "16", "存货", "流动负债合计", "A", "L" },
				{ "17", "　　原材料", "非流动负债:", "A", "L" }, { "18", "　　发出商品", "长期借款", "A", "L" },
				{ "19", "　　库存商品", "长期应付款", "A", "L" }, { "20", "　　周转材料", "递延收益", "A", "L" },
				{ "21", "其他流动资产", "其他非流动负债", "A", "L" }, { "22", "流动资产合计", "非流动负债合计", "A", "L" },
				{ "23", "非流动资产:", "负债合计", "A", "L" }, { "24", "长期股权投资", "", "A", "" }, { "25", "长期应收款", "", "A", "" },
				{ "26", "固定资产原价", "", "A", "" }, { "27", "累计折旧", "", "A", "" }, { "28", "固定资产账面净值", "", "A", "" },
				{ "29", "在建工程", "", "A", "" }, { "30", "工程物资", "", "A", "" }, { "31", "固定资产清理", "", "A", "" },
				{ "32", "生产性生物资产", "股东权益:", "A", "E" }, { "33", "无形资产", "实收资本(股本)", "A", "E" },
				{ "34", "开发支出", "资本公积", "A", "E" }, { "35", "长期待摊费用", "盈余公积", "A", "E" },
				{ "36", "其他非流动资产", "未分配利润", "A", "E" }, { "37", "非流动资产合计", "股东权益合计", "A", "E" },
				{ "38", "资产总计", "负债和股东权益合计", "A", "E" } };
	}

	/**
	 * 默认左侧资产类公式映射（使用方向后缀 #D 表示借方）
	 */
	private static Map<String, String> getDefaultLeftFormulaMap() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("货币资金", "现金+银行存款+短期投资");
		map.put("现金", "1001%");
		map.put("银行存款", "1002%");
		map.put("短期投资", "1101%");
		map.put("应收票据", "1121%");
		map.put("应收账款", "112201#D+112202#D+2203#D"); // 修改：取借方
		map.put("应收账款-集团内", "112201#D");
		map.put("应收账款-集团外", "112202#D");
		map.put("预付账款", "1123#D+2202#D"); // 修改：取借方
		map.put("应收股利", "1131%");
		map.put("应收利息", "1132%");
		map.put("其他应收款", "1221#D+2241#D"); // 修改：取借方
		map.put("其他应收款-集团内", "122101#D+122103#D+224101#D+224103#D");
		map.put("其他应收款-集团外", "122102#D+122104#D+224102#D+224104#D");
		map.put("存货", "原材料+发出商品+库存商品+周转材料");
		map.put("原材料", "1403%");
		map.put("发出商品", "1407%");
		map.put("库存商品", "1405%");
		map.put("周转材料", "1411%");
		map.put("其他流动资产", "0");
		map.put("流动资产合计", "货币资金+应收票据+应收账款+预付账款+应收股利+应收利息+其他应收款+存货+其他流动资产");
		map.put("长期股权投资", "1511%");
		map.put("长期应收款", "1531%");
		map.put("固定资产原价", "1601%");
		map.put("累计折旧", "1602%");
		map.put("固定资产账面净值", "固定资产原价-累计折旧");
		map.put("在建工程", "1604%+1605%");
		map.put("工程物资", "1605%");
		map.put("固定资产清理", "1606%");
		map.put("生产性生物资产", "1621%");
		map.put("无形资产", "1701%");
		map.put("开发支出", "5301%");
		map.put("长期待摊费用", "1801%");
		map.put("其他非流动资产", "0");
		map.put("非流动资产合计", "长期股权投资+长期应收款+固定资产账面净值+在建工程+工程物资+固定资产清理+生产性生物资产+无形资产+开发支出+长期待摊费用+其他非流动资产");
		map.put("资产总计", "流动资产合计+非流动资产合计");
		return map;
	}

	/**
	 * 默认右侧负债权益类公式映射（使用方向后缀 #C 表示贷方）
	 */
	private static Map<String, String> getDefaultRightFormulaMap() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("短期借款", "2001%");
		map.put("应付票据", "2201%");
		map.put("应付账款", "220201#C+220202#C+1123#C"); // 修改：取贷方
		map.put("应付账款-集团内", "220201#C");
		map.put("应付账款-集团外", "220202#C");
		map.put("预收账款", "1122#C+2203#C"); // 修改：取贷方
		map.put("应付职工薪酬", "2211%");
		map.put("应交税费", "2221%");
		map.put("应付利息", "2231%");
		map.put("应付股利", "2232%");
		map.put("其他应付款", "2241#C+1221#C"); // 修改：取贷方
		map.put("其他应付款-集团内", "224101#C+224103#C+122101#C+122103#C");
		map.put("其他应付款-集团外", "224102#C+224104#C+122102#C+122104#C");
		map.put("其他流动负债", "0");
		map.put("流动负债合计", "短期借款+应付票据+应付账款+预收账款+应付职工薪酬+应交税费+应付利息+应付股利+其他应付款+其他流动负债");
		map.put("长期借款", "2501%");
		map.put("长期应付款", "2701%");
		map.put("递延收益", "2401%");
		map.put("其他非流动负债", "0");
		map.put("非流动负债合计", "长期借款+长期应付款+递延收益+其他非流动负债");
		map.put("负债合计", "流动负债合计+非流动负债合计");
		map.put("实收资本(股本)", "4001%");
		map.put("资本公积", "4002%");
		map.put("盈余公积", "4101%");
		map.put("未分配利润", "4104%");
		map.put("股东权益合计", "实收资本(股本)+资本公积+盈余公积+未分配利润");
		map.put("负债和股东权益合计", "负债合计+股东权益合计");
		return map;
	}
}