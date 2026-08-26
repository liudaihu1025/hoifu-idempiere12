package com.hoifu.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MQuery;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.PrintInfo;
import org.compiere.model.Query;
import org.compiere.print.MPrintFormat;
import org.compiere.print.ReportCtl;
import org.compiere.print.ReportEngine;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;

@org.adempiere.base.annotation.Process
public class LabelPrintByInOutLine extends SvrProcess {

	// AD_SysConfig 中配置的 Name，用于可配置化，避免硬编码

	private static final String CONFIG_PRINT_FORMAT_NAME_TRANSFER = "PRINT_FORMAT_NAME_TRANSFER";
	private static final String CONFIG_TRANSFER_DOCTYPE_IDS = "INTRAGROUP_TRANSFER_DOCTYPE_IDS";

	private static final String DEFAULT_PRINT_FORMAT_NAME_TRANSFER = "集团内调拨单明细打印";
	private static final String DEFAULT_TRANSFER_DOCTYPE_IDS = "1000783,1000784";

	private static final String CONFIG_VIEW_NAME = "VIEW_NAME";
	private static final String CONFIG_PRINT_FORMAT_NAME = "PRINT_FORMAT_NAME";

	// 兜底默认值，防止 AD_SysConfig 未配置时功能不可用
	private static final String DEFAULT_VIEW_NAME = "m_receipt_label_info_v";
	private static final String DEFAULT_PRINT_FORMAT_NAME = "收货单明细打印";

	

	// ===== 物料中类常量（来自 ReceiptLabelPrintProcess）=====
	private static final String SUB_CATEGORY_YANZHI             = "YL01";   // 纸张  
	private static final String SUB_CATEGORY_YOUMO              = "YL02";   // 油墨  
	private static final String SUB_CATEGORY_GUANGYOU           = "YL03";   // 光油  
	private static final String SUB_CATEGORY_DIANHUALV          = "YL04";   // 电化铝  
	private static final String SUB_CATEGORY_BAOZHUANGFULIAO    = "YL05";   // 包装辅料  
	private static final String SUB_CATEGORY_JITAI_FULIAO       = "YL06";   // 机台辅料  
	private static final String SUB_CATEGORY_BANCAI             = "YL07";   // 版材  
	private static final String SUB_CATEGORY_JIAOSUI            = "YL08";   // 胶水  
	@Override
	protected void prepare() {
	}

	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();
		if (recordId <= 0)
			throw new IllegalArgumentException("未获取到当前行 M_InOutLine_ID");
		
		MInOutLine line = new MInOutLine(getCtx(), recordId, get_TrxName());
		if (line.get_ID() == 0)
			throw new IllegalArgumentException("找不到收货单明细行，M_InOutLine_ID=" + recordId);
		  
		MInOut inout = new MInOut(getCtx(), line.getM_InOut_ID(), get_TrxName());
		MProduct product = MProduct.get(getCtx(), line.getM_Product_ID(), get_TrxName());
		if (product.get_ID() == 0)
			throw new IllegalArgumentException("找不到物料，M_Product_ID=" + line.getM_Product_ID());
		  
		boolean isPrinted = line.get_ValueAsBoolean("islabelprint");
		if (isPrinted) {
			int existingCount = DB.getSQLValueEx(get_TrxName(),
					"SELECT COUNT(1) FROM m_receipt_label WHERE M_InOutLine_ID=? AND AD_Client_ID=? AND IsActive='Y'",
					recordId, getAD_Client_ID());
			if (existingCount <= 0) {
				isPrinted = false; // 标志位与数据不一致，重新按“首次打印”处理
			}
		}
		if (!isPrinted) {
			// 首次打印：生成并保存标签
			BigDecimal qtyReceived = line.getMovementQty();
			int packQty = getPackQty(product);
			String subCategory = getSubCategoryValue(product);
			String barcodePrefix = buildBarcodePrefix(product);

			List<LabelData> labels = generateLabels(product, qtyReceived, packQty, subCategory, barcodePrefix,
					line.getC_UOM_ID());

			for (LabelData ld : labels) {
				PO labelRecord = createReceiptLabelRecord(inout, line, product, ld);
				labelRecord.saveEx();
			}

			line.set_ValueOfColumn("islabelprint", true);
			line.saveEx();
		}
		// 若已打印过（补打场景），跳过标签生成，直接走下面的报表打印

		// 从 AD_SysConfig 按 Name 读取配置值（会自动按 AD_Client_ID 覆盖优先级），
		// 找不到时回退到默认值
		String viewName = MSysConfig.getValue(CONFIG_VIEW_NAME, DEFAULT_VIEW_NAME, getAD_Client_ID());

		// 判断是否为集团内调拨单
		String transferDocTypeIds = MSysConfig.getValue(CONFIG_TRANSFER_DOCTYPE_IDS, DEFAULT_TRANSFER_DOCTYPE_IDS,
				getAD_Client_ID());
		boolean isTransfer = false;
		if (transferDocTypeIds != null && transferDocTypeIds.length() > 0) {
			for (String idStr : transferDocTypeIds.split(",")) {
				if (String.valueOf(inout.getC_DocType_ID()).equals(idStr.trim())) {
					isTransfer = true;
					break;
				}
			}
		}
		// 根据不同单据选择不同的打印格式
		String printFormatName = isTransfer
				? MSysConfig.getValue(CONFIG_PRINT_FORMAT_NAME_TRANSFER, DEFAULT_PRINT_FORMAT_NAME_TRANSFER,
						getAD_Client_ID())
				: MSysConfig.getValue(CONFIG_PRINT_FORMAT_NAME, DEFAULT_PRINT_FORMAT_NAME, getAD_Client_ID());
		int AD_PrintFormat_ID = DB.getSQLValueEx(get_TrxName(),
				"SELECT AD_PrintFormat_ID FROM AD_PrintFormat WHERE Name=? AND AD_Client_ID IN (0,?) ORDER BY AD_Client_ID DESC",
				printFormatName, getAD_Client_ID());
		if (AD_PrintFormat_ID <= 0)
			throw new IllegalArgumentException("找不到打印格式: " + printFormatName);

		MPrintFormat format = MPrintFormat.get(getCtx(), AD_PrintFormat_ID, false);

		// 直接按 M_InOutLine_ID 过滤，不要求是主键
		MQuery query = new MQuery(viewName);

		query.addRestriction("M_InOutLine_ID", MQuery.EQUAL, Integer.valueOf(recordId));

		PrintInfo info = new PrintInfo(getProcessInfo());

		ReportEngine re = new ReportEngine(getCtx(), format, query, info, get_TrxName());

		// 调用系统统一的报表预览入口（ZK 网页里会打开标准报表查看窗口，自带 PDF 下载）
		ReportCtl.preview(re);

		return null;
	}
	
		  
	/**
	 * 按物料中类规则生成标签清单。 规则优先级：YL07 版材按张 → 包装数量=0 整量一张 → YL05/YL06 整量一张 → 其余按包装数量拆分。
	 */
	private List<LabelData> generateLabels(MProduct product, BigDecimal qtyReceived, int packQty, String subCategory,
			String barcodePrefix, int uomId) {
		List<LabelData> labels = new ArrayList<LabelData>();
		  
		if (SUB_CATEGORY_BANCAI.equals(subCategory)) {
			int n = qtyReceived.intValue();
			if (n <= 0)
				n = 1;
			for (int i = 1; i <= n; i++)
				labels.add(new LabelData(product.getValue(), BigDecimal.ONE, uomId, i));
			return labels;
		}
		  
		if (packQty <= 0) {
			labels.add(new LabelData(product.getValue(), qtyReceived, uomId, 1));
			return labels;
		}
		  
		if (SUB_CATEGORY_BAOZHUANGFULIAO.equals(subCategory) || SUB_CATEGORY_JITAI_FULIAO.equals(subCategory)) {
			labels.add(new LabelData(barcodePrefix, qtyReceived, uomId, 1));
			return labels;
		}
		  
		BigDecimal[] divRem = qtyReceived.divideAndRemainder(BigDecimal.valueOf(packQty));
		int fullPacks = divRem[0].intValue();
		BigDecimal remainder = divRem[1];
		  
		int nextSeq = getNextSeqNo(barcodePrefix);
		for (int i = 0; i < fullPacks; i++) {
			String seq = String.format("%04d", nextSeq + i);
			labels.add(new LabelData(barcodePrefix + seq, BigDecimal.valueOf(packQty), uomId, nextSeq + i));
		}
		if (remainder.compareTo(BigDecimal.ZERO) > 0) {
			int seqNo = nextSeq + fullPacks;
			String seq = String.format("%04d", seqNo);
			labels.add(new LabelData(barcodePrefix + seq, remainder, uomId, seqNo));
		}
		  
		if (labels.isEmpty())
			labels.add(new LabelData(barcodePrefix + String.format("%04d", nextSeq), qtyReceived, uomId, nextSeq));

		return labels;
	}
		  
	/**
	 * 保存标签记录到 m_receipt_label（通用 PO 写法，未生成 Model 类）。
	 */
	private PO createReceiptLabelRecord(MInOut inout, MInOutLine line, MProduct product, LabelData ld) {
		PO labelRecord = MTable.get(getCtx(), "m_receipt_label").getPO(0, get_TrxName());
		labelRecord.set_ValueOfColumn("AD_Client_ID", getAD_Client_ID());
		labelRecord.set_ValueOfColumn("AD_Org_ID", line.getAD_Org_ID());
		labelRecord.set_ValueOfColumn("M_InOut_ID", inout.getM_InOut_ID());
		labelRecord.set_ValueOfColumn("M_InOutLine_ID", line.getM_InOutLine_ID());
		labelRecord.set_ValueOfColumn("M_Product_ID", product.getM_Product_ID());
		labelRecord.set_ValueOfColumn("LabelBarcode", ld.barcode);
		labelRecord.set_ValueOfColumn("Qty", ld.qty);
		labelRecord.set_ValueOfColumn("C_UOM_ID", ld.uomId);
		labelRecord.set_ValueOfColumn("SeqNo", ld.seqNo);
		return labelRecord;
	}
		  
	/**
	 * 读取物料包装数量（UnitsPerPack）。 该列在数据库中为 NUMERIC，直接调用生成的 getUnitsPerPack() 会因
	 * BigDecimal 强转 Integer 抛 ClassCastException，此处按 BigDecimal
	 * 读取后取整（包装数量语义上为整数，截断即可）。
	 */
	private int getPackQty(MProduct product) {
		Object value = product.get_Value("UnitsPerPack");
		if (value instanceof BigDecimal)
			return ((BigDecimal) value).intValue();
		return product.get_ValueAsInt("UnitsPerPack");
	}

	private String getSubCategoryValue(MProduct product) {
		int catL2Id = product.get_ValueAsInt("M_Product_Category_ID_L2");
		if (catL2Id <= 0)
			return "";
		MProductCategory cat = new MProductCategory(getCtx(), catL2Id, get_TrxName());
		if (cat.get_ID() == 0)
			return "";
		String value = cat.getValue();
		return value == null ? "" : value.trim();
	}

	/**
	 * 条码前缀 = M_Product_Category_ID（L1 大类）对应分类的 Value + YYMMDD。
	 * 同一批次内所有标签共享此前缀，仅末尾4位流水号递增。
	 */
	private String buildBarcodePrefix(MProduct product) {
		MProductCategory cat = new MProductCategory(getCtx(), product.getM_Product_Category_ID(), get_TrxName());
		String catValue = "";
		if (cat.get_ID() > 0) {
			catValue = cat.getValue();
			if (catValue == null)
				catValue = "";
		}
		return catValue + buildDateCode();
	}
		  
	/**
	 * 跨批次查询 m_receipt_label 中已有条码的最大末尾4位序号 +1，防止重复。
	 */
	private int getNextSeqNo(String barcodePrefix) {
		PO last = new Query(getCtx(), "m_receipt_label", "LabelBarcode LIKE ? AND IsActive='Y'", get_TrxName())
				.setParameters(barcodePrefix + "____").setOrderBy("LabelBarcode DESC").first();
		int maxSeq = 0;
		if (last != null) {
			String lastBarcode = last.get_ValueAsString("LabelBarcode");
			if (lastBarcode != null && lastBarcode.length() >= barcodePrefix.length() + 4) {
				try {
					maxSeq = Integer.parseInt(lastBarcode.substring(barcodePrefix.length()));
				} catch (NumberFormatException e) {
					maxSeq = 0;
				}  
			}  
		}
		return maxSeq + 1;
	}
		  
	/**
	 * 生成日期段 YYMMDD：2 位年份 + 2 位月份 + 2 位日，共 6 位。如 2026-08-07 → 260807。
	 */
	private String buildDateCode() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR) % 100;
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		return String.format("%02d", year) + String.format("%02d", month) + String.format("%02d", day);
	}
		  
	/** 标签数据载体 */
	private static class LabelData {
		final String barcode;
		final BigDecimal qty;
		final int uomId;
		final int seqNo;
		  
		LabelData(String barcode, BigDecimal qty, int uomId, int seqNo) {
			this.barcode = barcode;
			this.qty = qty;
			this.uomId = uomId;
			this.seqNo = seqNo;
		}
	}
}