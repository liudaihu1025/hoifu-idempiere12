package com.hoifu.service.qc.impl;

import org.compiere.model.MInOutLine;
import org.compiere.model.MProduct;
import org.compiere.model.Query;
import org.compiere.util.CLogger;

import com.hoifu.model.MQC_IQCInspect;
import com.hoifu.service.qc.IIQCInspectService;

/**
 * 来料检验单生成服务实现（新检验体系，与旧 IQCServiceImpl 完全隔离）
 */
public class IQCInspectServiceImpl implements IIQCInspectService {

	private static final CLogger log = CLogger.getCLogger(IQCInspectServiceImpl.class);

	private static final String INSPECTIONTYPE_IQC = "IQC";

	@Override
	public void createFromCompletedReceiptLine(MInOutLine line) {
		try {
			// 1. 加载物料（不用缓存的 MProduct.get，避免不可变对象问题）
			MProduct product = new MProduct(line.getCtx(), line.getM_Product_ID(), line.get_TrxName());
			if (product == null || product.get_ID() <= 0)
				return;

			// 2. 读取检验类型，非 IQC 则跳过
			String inspectionType = (String) product.get_Value("InspectionType");
			if (!INSPECTIONTYPE_IQC.equals(inspectionType))
				return;

			// 3. 幂等性校验：该行已生成过检验单则跳过
			boolean exists = new Query(line.getCtx(), MQC_IQCInspect.Table_Name,
					"M_InOutLine_ID=? AND IsActive='Y'", line.get_TrxName())
					.setParameters(line.getM_InOutLine_ID())
					.match();
			if (exists) {
				log.fine("收货明细行[ID=" + line.getM_InOutLine_ID() + "]已存在检验单，跳过");
				return;
			}

			// 4. 生成来料检验单
			MQC_IQCInspect inspect = new MQC_IQCInspect(line.getCtx(), 0, line.get_TrxName());

			inspect.setAD_Org_ID(line.getAD_Org_ID());
			inspect.setM_InOut_ID(line.getM_InOut_ID());
			inspect.setM_InOutLine_ID(line.getM_InOutLine_ID());
			inspect.setM_Product_ID(line.getM_Product_ID());
			// C_DocType_ID/DocumentNo 由 MQC_IQCInspect.beforeSave() 自动生成
			inspect.saveEx();

			log.info("已为收货明细行[ID=" + line.getM_InOutLine_ID()
					+ "]生成来料检验单: " + inspect.getDocumentNo());

		} catch (Exception e) {
			// 记录错误日志但不阻断收货单完成
			log.severe("收货明细行[ID=" + line.getM_InOutLine_ID() + "]生成来料检验单失败: " + e.getMessage());
		}
	}
}
