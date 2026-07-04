package org.libero.callouts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.adempiere.base.Core;
import org.adempiere.base.IColumnCallout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.libero.model.MHFLengbieConfig;

public class Callout_M_InOutLine_Product implements IColumnCallout {

    @Override  
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		String col = mField.getColumnName();
		if ("M_Product_ID".equals(col)) {
			String err = onProductChange(ctx, mTab, value);
			if (err != null && !err.isEmpty())
				return err;
			// 产品变化后重量已更新，同步更新表头总重量
			return updateHeaderWeight(ctx, WindowNo, mTab);
		}
		if ("QtyEntered".equals(col)) {
			return updateHeaderWeight(ctx, WindowNo, mTab);
		}
		return null;
	}

	private String onProductChange(Properties ctx, GridTab mTab, Object value) {
		mTab.setValue("Specification", null);
		mTab.setValue("Weight", null);
		mTab.setValue("Area", null);
		mTab.setValue("CreaseLine", null);

		if (value == null)
			return "";
		int M_Product_ID = (Integer) value;
		if (M_Product_ID <= 0)
			return "";

		MProduct product = MProduct.get(ctx, M_Product_ID);
		if (product == null || product.get_ID() <= 0)
			return "";

		// ── 面积：M_Product.BoxArea ──────────────────────────────────────
		mTab.setValue("Area", product.get_Value("BoxArea"));

		// ── 获取 HX_BoxType_ID 外键 ─────────────────────────────────────
		Object boxTypeIDObj = product.get_Value("HX_BoxType_ID");
		if (boxTypeIDObj == null)
			return "";
		int HX_BoxType_ID = (Integer) boxTypeIDObj;
		if (HX_BoxType_ID <= 0)
			return "";

		// ── 加载 HX_BoxType 记录 ─────────────────────────────────────────
		MTable boxTypeTable = MTable.get(ctx, "HX_BoxType");
		if (boxTypeTable == null)
			return "";
		PO boxType = boxTypeTable.getPO(HX_BoxType_ID, null);
		if (boxType == null || boxType.get_ID() <= 0)
			return "";

		// ── 构建公式变量 Map（规格、重量、压线共用）──────────────────────
		BigDecimal length = toBD(product.get_Value("Length"));
		BigDecimal width = toBD(product.get_Value("Width"));
		BigDecimal height = toBD(product.get_Value("Height"));
		BigDecimal weightNet = toBD(product.get_Value("WeightNet"));
		BigDecimal thickness = toBD(product.get_Value("Thickness"));
		String lengbie = (String) product.get_Value("Lengbie");

		BigDecimal calcRatio = BigDecimal.ZERO;
		BigDecimal nailMouthValue = BigDecimal.ZERO;
		BigDecimal plugInterfaceValue = BigDecimal.ZERO;

		if (lengbie != null && !lengbie.isEmpty()) {
			MHFLengbieConfig config = new Query(ctx, MHFLengbieConfig.Table_Name, "HX_BoxType_ID=? AND Lengbie=?", null)
					.setParameters(HX_BoxType_ID, lengbie).first();
			if (config != null) {
				calcRatio = config.getCalcRatio();
				nailMouthValue = config.getNailmMouthValue();
				plugInterfaceValue = config.getPlugInterfaceValue();
			}
		}

		Map<String, Object> variables = new HashMap<>();
		variables.put("长度", length);
		variables.put("宽度", width);
		variables.put("高度", height);
		variables.put("净重", weightNet);
		variables.put("厚度", thickness);
		variables.put("系数", calcRatio);
		variables.put("钉口", nailMouthValue);
		variables.put("插接口", plugInterfaceValue);

		// ── 规格：执行公式，结果用 * 连接 ───────────────────────────────
		String lengthFormula = boxType.get_ValueAsString("verticalexpandlength");
		String widthFormula = boxType.get_ValueAsString("verticalexpandwidth");
		if (!lengthFormula.isEmpty() && !widthFormula.isEmpty()) {
			try {
				BigDecimal cardLength = calculateFormula(lengthFormula, variables);
				BigDecimal cardWidth = calculateFormula(widthFormula, variables);
				mTab.setValue("Specification", cardLength.stripTrailingZeros().toPlainString() + "*"
						+ cardWidth.stripTrailingZeros().toPlainString());
			} catch (ScriptException e) {
				return "规格公式计算错误: " + e.getMessage();
			}
		}

		// ── 重量：执行 WeightFormula 公式，结果为 BigDecimal ────────────
		String weightFormula = boxType.get_ValueAsString("WeightFormula");
		if (!weightFormula.isEmpty()) {
			try {
				BigDecimal weight = calculateFormula(weightFormula, variables);
				mTab.setValue("Weight", weight);
			} catch (ScriptException e) {
				return "重量公式计算错误: " + e.getMessage();
			}
		}

		// ── 压线：执行公式，结果用 + 连接 ───────────────────────────────
		String creaseFormula1 = boxType.get_ValueAsString("paperwidthcrease1");
		String creaseFormula2 = boxType.get_ValueAsString("paperwidthcrease2");
		String creaseFormula3 = boxType.get_ValueAsString("paperwidthcrease3");
		try {
			List<String> parts = new ArrayList<>();
			if (!creaseFormula1.isEmpty())
				parts.add(calculateFormula(creaseFormula1, variables).stripTrailingZeros().toPlainString());
			if (!creaseFormula2.isEmpty())
				parts.add(calculateFormula(creaseFormula2, variables).stripTrailingZeros().toPlainString());
			if (!creaseFormula3.isEmpty())
				parts.add(calculateFormula(creaseFormula3, variables).stripTrailingZeros().toPlainString());
			mTab.setValue("CreaseLine", String.join("+", parts));
		} catch (ScriptException e) {
			return "压线公式计算错误: " + e.getMessage();
		}

		return "";
    }  
  
	private String updateHeaderWeight(Properties ctx, int WindowNo, GridTab mTab) {
		BigDecimal lineWeight = toBD(mTab.getValue("Weight"));
		BigDecimal qtyEntered = toBD(mTab.getValue("QtyEntered"));
		BigDecimal lineTotal = lineWeight.multiply(qtyEntered);

		int M_InOut_ID = Env.getContextAsInt(ctx, WindowNo, "M_InOut_ID");
		int M_InOutLine_ID = Env.getContextAsInt(ctx, WindowNo, mTab.getTabNo(), "M_InOutLine_ID");

		if (M_InOut_ID <= 0)
			return "";

		BigDecimal otherTotal = DB
				.getSQLValueBDEx(null,
						"SELECT COALESCE(SUM(Weight * QtyEntered), 0) FROM M_InOutLine "
								+ "WHERE M_InOut_ID=? AND M_InOutLine_ID!=? AND IsActive='Y'",
						M_InOut_ID, M_InOutLine_ID);
		if (otherTotal == null)
			otherTotal = BigDecimal.ZERO;

		GridTab parentTab = mTab.getParentTab();
		if (parentTab != null) {
			parentTab.setValue("Weight", otherTotal.add(lineTotal));
		}

        return "";  
    }  

	private BigDecimal toBD(Object val) {
		if (val instanceof BigDecimal)
			return (BigDecimal) val;
		if (val == null)
			return BigDecimal.ZERO;
		try {
			return new BigDecimal(val.toString());
		} catch (Exception e) {
			return BigDecimal.ZERO;
		}
	}

	private BigDecimal calculateFormula(String formula, Map<String, Object> variables) throws ScriptException {
		ScriptEngine engine = Core.getScriptEngine("groovy");
		if (engine == null)
			throw new ScriptException("Groovy 引擎不可用");
		for (Map.Entry<String, Object> entry : variables.entrySet()) {
			engine.put(entry.getKey(), entry.getValue());
		}
		Object result = engine.eval(formula);
		if (result instanceof Number)
			return new BigDecimal(result.toString());
		throw new ScriptException("公式计算结果不是数字: " + result);
	}
}