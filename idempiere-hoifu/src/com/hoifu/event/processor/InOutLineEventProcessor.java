package com.hoifu.event.processor;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.adempiere.base.Core;
import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProduct;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.hoifu.service.qc.IIQCService;
import com.hoifu.service.qc.IOQCService;
import com.hoifu.service.qc.IRQCService;

public class InOutLineEventProcessor implements IEventProcessor {

	private static final CLogger log = CLogger.getCLogger(InOutLineEventProcessor.class);

	private final IIQCService iqcService;
	private final IOQCService oqcService;
	private final IRQCService rqcService;

	public InOutLineEventProcessor(IIQCService iqcService, IOQCService oqcService, IRQCService rqcService) {
		this.iqcService = iqcService;
		this.oqcService = oqcService;
		this.rqcService = rqcService;
	}

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MInOutLine;

	}

	@Override
	public void process(PO po, String topic) {
		MInOutLine line = (MInOutLine) po;
		if (IEventTopics.PO_BEFORE_NEW.equals(topic)) {
			fillInOutLineFields(line); // 保存前逻辑
		} else if (IEventTopics.PO_AFTER_NEW.equals(topic)) {
			handleQC(line); // 保存后逻辑
		}
	}

	// ── QC 逻辑 ──────────────────────────────────────────────────────────────
	private void handleQC(MInOutLine line) {
		if (!MSysConfig.getBooleanValue("QC_ENABLE_CHECK", false))
			return;
		MInOut parent = (MInOut) line.getParent();
		if (rqcService.isReturnDocument(parent))
			rqcService.createFromLine(parent, line);
		else if (!parent.isSOTrx())
			iqcService.createFromReceiptLine(line);
		else
			oqcService.createFromShipmentLine(line);
	}

	// ── 自定义字段填充逻辑 ────────────────────────────────────────────────────
	private void fillInOutLineFields(MInOutLine line) {
		Object productIdObj = line.get_Value("M_Product_ID");
		if (productIdObj == null)
			return;
		int productId = ((Number) productIdObj).intValue();
		if (productId <= 0)
			return;

		MProduct product = MProduct.get(line.getCtx(), productId);
		if (product == null)
			return;

		BigDecimal length = toBD(product.get_Value("Length"));
		BigDecimal width = toBD(product.get_Value("Width"));
		BigDecimal height = toBD(product.get_Value("Height"));
		BigDecimal weightNet = toBD(product.get_Value("WeightNet"));
		BigDecimal thickness = toBD(product.get_Value("Thickness"));
		String lengbie = product.get_ValueAsString("Lengbie");

		Object boxTypeIdObj = product.get_Value("HX_BoxType_ID");
		if (boxTypeIdObj == null)
			return;
		int boxTypeId = ((Number) boxTypeIdObj).intValue();
		if (boxTypeId <= 0)
			return;

		MTable boxTypeTable = MTable.get(line.getCtx(), "HX_BoxType");
		PO boxType = new Query(line.getCtx(), boxTypeTable, "HX_BoxType_ID=?", null).setParameters(boxTypeId).first();
		if (boxType == null)
			return;

		BigDecimal calcRatio = BigDecimal.ZERO;
		BigDecimal nailMouth = BigDecimal.ZERO;
		BigDecimal plugInterface = BigDecimal.ZERO;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			String sql = "SELECT CalcRatio, NailmMouthValue, PlugInterfaceValue " + "FROM HF_LengbieConfig "
					+ "WHERE HX_BoxType_ID=? AND Lengbie=? AND IsActive='Y'";
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, boxTypeId);
			pstmt.setString(2, lengbie);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				BigDecimal v;
				v = rs.getBigDecimal("CalcRatio");
				if (v != null)
					calcRatio = v;
				v = rs.getBigDecimal("NailmMouthValue");
				if (v != null)
					nailMouth = v;
				v = rs.getBigDecimal("PlugInterfaceValue");
				if (v != null)
					plugInterface = v;
			}
		} catch (Exception e) {
			log.warning("查询楞别配置失败: " + e.getMessage());
			return;
		} finally {
			DB.close(rs, pstmt);
		}

		Map<String, Object> variables = new HashMap<>();
		variables.put("长度", length);
		variables.put("宽度", width);
		variables.put("高度", height);
		variables.put("净重", weightNet);
		variables.put("厚度", thickness);
		variables.put("系数", calcRatio);
		variables.put("钉口", nailMouth);
		variables.put("插接口", plugInterface);

		try {
			fillSpecification(line, boxType, variables);
			fillWeight(line, boxType, variables);
			fillArea(line, product);
			fillCreaseLine(line, boxType, variables);
		} catch (ScriptException e) {
			log.warning("公式计算错误: " + e.getMessage());
		}
	}

	private void fillSpecification(MInOutLine line, PO boxType, Map<String, Object> variables) throws ScriptException {
		String lengthFormula = boxType.get_ValueAsString("verticalexpandlength");
		String widthFormula = boxType.get_ValueAsString("verticalexpandwidth");
		if (lengthFormula.isEmpty() || widthFormula.isEmpty())
			return;
		BigDecimal cardLength = calculateFormula(lengthFormula, variables);
		BigDecimal cardWidth = calculateFormula(widthFormula, variables);
		line.set_ValueOfColumn("Specification",
				cardLength.stripTrailingZeros().toPlainString() + "*" + cardWidth.stripTrailingZeros().toPlainString());
	}

	private void fillWeight(MInOutLine line, PO boxType, Map<String, Object> variables) throws ScriptException {
		String weightFormula = boxType.get_ValueAsString("WeightFormula");
		if (weightFormula.isEmpty())
			return;
		line.set_ValueOfColumn("Weight", calculateFormula(weightFormula, variables));
	}

	private void fillArea(MInOutLine line, MProduct product) {
		Object boxArea = product.get_Value("BoxArea");
		if (boxArea != null)
			line.set_ValueOfColumn("Area", boxArea);
	}

	private void fillCreaseLine(MInOutLine line, PO boxType, Map<String, Object> variables) throws ScriptException {
		String f1 = boxType.get_ValueAsString("paperwidthcrease1");
		String f2 = boxType.get_ValueAsString("paperwidthcrease2");
		String f3 = boxType.get_ValueAsString("paperwidthcrease3");
		List<String> parts = new ArrayList<>();
		if (!f1.isEmpty())
			parts.add(calculateFormula(f1, variables).stripTrailingZeros().toPlainString());
		if (!f2.isEmpty())
			parts.add(calculateFormula(f2, variables).stripTrailingZeros().toPlainString());
		if (!f3.isEmpty())
			parts.add(calculateFormula(f3, variables).stripTrailingZeros().toPlainString());
		if (!parts.isEmpty())
			line.set_ValueOfColumn("CreaseLine", String.join("+", parts));
	}

	// ── 工具方法 ──────────────────────────────────────────────────────────────
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
		for (Map.Entry<String, Object> entry : variables.entrySet())
			engine.put(entry.getKey(), entry.getValue());
		Object result = engine.eval(formula);
		if (result instanceof Number)
			return new BigDecimal(result.toString());
		throw new ScriptException("公式计算结果不是数字: " + result);
	}
}