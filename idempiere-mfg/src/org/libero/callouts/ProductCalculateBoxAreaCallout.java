package org.libero.callouts;  
  
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.adempiere.base.Core;
import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.Query;
import org.libero.model.MHFLengbieConfig;
import org.libero.model.MHX_BoxType;  
  
@Callout(tableName = "M_Product", columnName = {"Length", "Width", "Height", "Lengbie", "HX_BoxType_ID"})  
public class ProductCalculateBoxAreaCallout implements IColumnCallout {  
  
	// mm² 转 m² 的转换因子：1 mm² = 0.000001 m²  
	private static final BigDecimal MM2_TO_M2 = new BigDecimal("0.000001");  
  
	@Override  
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {  
		if (value == null || value.equals(oldValue))  
			return "";  
  
		// 获取产品字段值  
		BigDecimal length = (BigDecimal) mTab.getValue("Length");  
		BigDecimal width = (BigDecimal) mTab.getValue("Width");  
		BigDecimal height = (BigDecimal) mTab.getValue("Height");  
		BigDecimal weightNet = (BigDecimal) mTab.getValue("WeightNet");  
		BigDecimal thickness = (BigDecimal) mTab.getValue("Thickness");  
		String lengbie = (String) mTab.getValue("Lengbie");  // 改为 String 类型  
		Integer boxTypeId = (Integer) mTab.getValue("HX_BoxType_ID");  
  
		if (boxTypeId == null || boxTypeId == 0 || lengbie == null || lengbie.isEmpty())  
			return "";  
  
		// 获取盒型公式  
		MHX_BoxType boxType = new MHX_BoxType(ctx, boxTypeId, null);  
		String lengthFormula = boxType.getverticalexpandlength(); // 如：(长度+宽度)*2+钉口  
		String widthFormula = boxType.getverticalexpandwidth();   // 如：宽度+高度+系数  
  
		// 获取楞别配置参数（使用 String 类型的 lengbie）  
		String whereClause = "HX_BoxType_ID=? AND Lengbie=?";  
		MHFLengbieConfig config = new Query(ctx, MHFLengbieConfig.Table_Name, whereClause, null)  
			.setParameters(boxTypeId, lengbie)  
			.first();  
  
		BigDecimal calcRatio = BigDecimal.ZERO; // 系数
		BigDecimal nailMouthValue = BigDecimal.ZERO; // 钉口
		BigDecimal plugInterfaceValue = BigDecimal.ZERO; // 插接口
		if (config != null) {
			calcRatio = config.getCalcRatio(); // 系数
			nailMouthValue = config.getNailmMouthValue(); // 钉口
			plugInterfaceValue = config.getPlugInterfaceValue(); // 插接口
		}
  
		// 构建变量 Map  
		Map<String, Object> variables = new HashMap<>();  
		variables.put("长度", length != null ? length : BigDecimal.ZERO);  
		variables.put("宽度", width != null ? width : BigDecimal.ZERO);  
		variables.put("高度", height != null ? height : BigDecimal.ZERO);  
		variables.put("净重", weightNet != null ? weightNet : BigDecimal.ZERO);  
		variables.put("厚度", thickness != null ? thickness : BigDecimal.ZERO);  
		variables.put("系数", calcRatio != null ? calcRatio : BigDecimal.ZERO);  
		variables.put("钉口", nailMouthValue != null ? nailMouthValue : BigDecimal.ZERO);  
		variables.put("插接口", plugInterfaceValue != null ? plugInterfaceValue : BigDecimal.ZERO);  
  
		try {  
			// 计算竖坑展长（单位：mm）  
			BigDecimal cardLength = calculateFormula(lengthFormula, variables);  
			  
			// 计算竖坑展宽（单位：mm）  
			BigDecimal cardWidth = calculateFormula(widthFormula, variables);  
			  
			// 计算纸板面积（mm² → m²，两位小数四舍五入）  
			BigDecimal cardAreaMM2 = cardLength.multiply(cardWidth);  
			BigDecimal cardAreaM2 = cardAreaMM2.multiply(MM2_TO_M2).setScale(2, RoundingMode.HALF_UP);  
			  
			// 计算纸箱面积（mm² → m²，两位小数四舍五入）  
			BigDecimal boxAreaMM2 = cardWidth.multiply(cardLength);  
			BigDecimal boxAreaM2 = boxAreaMM2.multiply(MM2_TO_M2).setScale(2, RoundingMode.HALF_UP);  
  
			// 设置结果字段  
			mTab.setValue("CardLength", cardLength);  // 保持 mm 单位  
			mTab.setValue("CardWidth", cardWidth);    // 保持 mm 单位  
			mTab.setValue("CardArea", cardAreaM2);     // 转换为 m²  
			mTab.setValue("BoxArea", boxAreaM2);       // 转换为 m²  
			// ★★★ 新增：计算压线信息，存入 creaseLine ★★★
			String creaseFormula1 = boxType.get_ValueAsString("paperwidthcrease1");
			String creaseFormula2 = boxType.get_ValueAsString("paperwidthcrease2");
			String creaseFormula3 = boxType.get_ValueAsString("paperwidthcrease3");

			List<String> creaseParts = new ArrayList<>();
			if (!creaseFormula1.isEmpty()) {
				BigDecimal c1 = calculateFormula(creaseFormula1, variables);
				creaseParts.add(c1.stripTrailingZeros().toPlainString());
			}
			if (!creaseFormula2.isEmpty()) {
				BigDecimal c2 = calculateFormula(creaseFormula2, variables);
				creaseParts.add(c2.stripTrailingZeros().toPlainString());
			}
			if (!creaseFormula3.isEmpty()) {
				BigDecimal c3 = calculateFormula(creaseFormula3, variables);
				creaseParts.add(c3.stripTrailingZeros().toPlainString());
			}
			mTab.setValue("creaseLine", String.join("+", creaseParts)); // ← 字段名改为 creaseLine
  
		} catch (ScriptException e) {  
			return "公式计算错误: " + e.getMessage();  
		}  
  
		return "";  
	}  
  
	/**  
	 * 使用 Groovy 引擎计算公式  
	 */  
	private BigDecimal calculateFormula(String formula, Map<String, Object> variables) throws ScriptException {  
		ScriptEngine engine = Core.getScriptEngine("groovy");  
		if (engine == null)  
			throw new ScriptException("Groovy 引擎不可用");  
  
		// 注入变量  
		for (Map.Entry<String, Object> entry : variables.entrySet()) {  
			engine.put(entry.getKey(), entry.getValue());  
		}  
  
		Object result = engine.eval(formula);  
		if (result instanceof Number) {  
			return new BigDecimal(result.toString());  
		}  
		throw new ScriptException("公式计算结果不是数字: " + result);  
	}  
}