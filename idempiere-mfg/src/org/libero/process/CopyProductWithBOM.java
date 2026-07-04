package org.libero.process;

import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.compiere.model.MProduct;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;

/**
 * 复制产品资料及其BOM（含BOM子件） 在产品窗口选中某条记录后触发，复制产品主数据及其下所有BOM和BOM子件。 Value 使用原值 +
 * 时间戳保证唯一性。
 */
public class CopyProductWithBOM extends SvrProcess {

	private int p_Record_ID = 0;
	private int bomCount = 0;
	private int lineCount = 0;

	@Override
	protected void prepare() {
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (para[i].getParameter() != null) {
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
			}
		}
		p_Record_ID = getRecord_ID();
	}

	@Override
	protected String doIt() throws Exception {
		if (p_Record_ID == 0)
			throw new IllegalArgumentException("Source M_Product_ID == 0");

		Properties ctx = Env.getCtx();

		// 1. 加载源产品
		MProduct sourceProduct = new MProduct(ctx, p_Record_ID, get_TrxName());
		log.info("Copying product: " + sourceProduct.getValue() + " - " + sourceProduct.getName());

		// 2. 复制产品主数据
		MProduct newProduct = new MProduct(ctx, 0, get_TrxName());
		PO.copyValues(sourceProduct, newProduct);
		newProduct.setAD_Org_ID(sourceProduct.getAD_Org_ID());
		// 手动设置IsProduct为Y
		newProduct.set_CustomColumn("IsProduct", Boolean.TRUE);
		// Value 使用原值 + 时间戳，保证唯一性
		String newProductValue = sourceProduct.getValue() + "_" + System.currentTimeMillis();
		newProduct.setValue(newProductValue);
		newProduct.saveEx();
		log.info("New product created: " + newProduct.getValue() + " (ID=" + newProduct.get_ID() + ")");

		// 3. 查询源产品下所有 PP_Product_BOM
		List<MPPProductBOM> boms = new Query(ctx, MPPProductBOM.Table_Name,
				MPPProductBOM.COLUMNNAME_M_Product_ID + "=?", get_TrxName()).setParameters(p_Record_ID)
				.setOnlyActiveRecords(true).list();

		if (boms.isEmpty()) {
			return "已复制产品: " + sourceProduct.getValue() + " → " + newProduct.getValue() + "，源产品无BOM记录";
		}

		for (MPPProductBOM sourceBOM : boms) {
			// 4. 复制 BOM 头
			MPPProductBOM newBOM = new MPPProductBOM(ctx, 0, get_TrxName());
			PO.copyValues(sourceBOM, newBOM);
			newBOM.setAD_Org_ID(sourceBOM.getAD_Org_ID()); // ← 显式设置
			newBOM.setM_Product_ID(newProduct.get_ID()); // 关联新产品
			// BOM Value 使用新产品 Value（单个BOM）或加序号（多个BOM）
			String bomValue = (boms.size() == 1) ? newProduct.getValue() : newProduct.getValue() + "_" + (bomCount + 1);
			newBOM.setValue(bomValue);
			newBOM.setDocumentNo(bomValue);
			newBOM.saveEx();
			bomCount++;

			// 5. 复制 BOM 子件行
			MPPProductBOMLine[] lines = sourceBOM.getLines();
			for (MPPProductBOMLine line : lines) {
				MPPProductBOMLine newLine = new MPPProductBOMLine(ctx, 0, get_TrxName());
				MPPProductBOMLine.copyValues(line, newLine);
				newLine.setAD_Org_ID(line.getAD_Org_ID());
				newLine.setPP_Product_BOM_ID(newBOM.getPP_Product_BOM_ID()); // 关联新BOM
				newLine.saveEx();
				lineCount++;
			}
		}

		return "已复制产品: " + sourceProduct.getValue() + " → " + newProduct.getValue();
	}

	@Override
	protected void postProcess(boolean success) {
		this.addLog("BOM " + bomCount + " 条，BOM子件 " + lineCount + " 条");
	}
}