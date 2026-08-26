package com.hoifu.event.processor;  
  
import java.math.BigDecimal;
import java.sql.Timestamp;

import org.adempiere.base.event.IEventTopics;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MDocType;
import org.compiere.model.MOrder;
import org.compiere.model.MOrderLine;
import org.compiere.model.MProduct;
import org.compiere.model.MProductPO;
import org.compiere.model.MProductPrice;
import org.compiere.model.MSequence;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
  
public class COrderEventProcessor implements IEventProcessor {  

	private static final CLogger log = CLogger.getCLogger(COrderEventProcessor.class);
  
    private static final String COLUMNNAME_C_Tax_ID = "C_Tax_ID";  
    // 委外订单 UUID
    private static final String SUBCONTRACT_DOCTYPE_UU = "5ae935d6-5237-4d93-ac5b-4967cf9be287";  
    
    @Override  
    public boolean supports(PO po, String topic) {  
        return po instanceof MOrder;  
    }  
  
    @Override  
    public void process(PO po, String topic) {  
        MOrder order = (MOrder) po;  
        syncTaxToLines(order, topic);
        inheritDocumentNoFromSubcontractSource(order, topic);
		createSamplingDemandOnComplete(order, topic);
		syncProductPriceOnComplete(order, topic);
		createProductPOOnComplete(order, topic);
    }  
  
    /**  
     * 若 C_Order.C_Tax_ID 发生变化，则将新值同步到所有 C_OrderLine  
     */  
    void syncTaxToLines(MOrder order, String topic) {  
        // 仅在 AFTER_CHANGE 且 C_Tax_ID 确实变化时触发  
        if (!IEventTopics.PO_AFTER_CHANGE.equals(topic)) {  
            return;  
        }  
        if (!order.is_ValueChanged(COLUMNNAME_C_Tax_ID)) {  
            return;  
        }  
  
        int newTaxId = order.get_ValueAsInt(COLUMNNAME_C_Tax_ID);  
        if (newTaxId <= 0) {  
            return;  
        }  
  
        MOrderLine[] lines = order.getLines();  
        for (MOrderLine line : lines) {  
            line.setC_Tax_ID(newTaxId);  
            line.saveEx();  
        }  
    }
    
    /**  
     * 若订单是通过反向单据（counter document）生成的，  
     * 且源订单的单据类型是委外订单，则直接使用源订单的单号。  
     *  
     * 判断依据：Ref_Order_ID > 0 表示该订单是由源订单生成的对应单据。  
     */  
    void inheritDocumentNoFromSubcontractSource(MOrder order, String topic) {  
        // 仅在新建前触发（此时 Ref_Order_ID 已在内存中设置，DocumentNo 可被覆盖）  
        if (!IEventTopics.PO_BEFORE_NEW.equals(topic)) {  
            return;  
        }  
        // Ref_Order_ID > 0 才是反向单据  
        int refOrderId = order.getRef_Order_ID();  
        if (refOrderId <= 0) {  
            return;  
        }  
        // 加载源订单  
        MOrder sourceOrder = new MOrder(order.getCtx(), refOrderId, order.get_TrxName());  
        if (sourceOrder.getC_Order_ID() <= 0) {  
            return;  
        }  
        // 判断源订单的单据类型是否是委外订单  
        MDocType sourceDocType = MDocType.get(order.getCtx(), sourceOrder.getC_DocType_ID());  
        if (sourceDocType == null) {  
            return;  
        }  
        if (!SUBCONTRACT_DOCTYPE_UU.equals(sourceDocType.getC_DocType_UU())) {  
            return;  
        }  
        // 直接使用源订单的单号  
        order.setDocumentNo(sourceOrder.getDocumentNo());  
    }  

	/**
	 * 销售订单完成后，自动生成打样需求单（DY_SamplingDemand）。 若已存在关联记录则跳过（防重复）。
	 */
    void createSamplingDemandOnComplete(MOrder order, String topic) {  
		// 1. 仅在 DOC_AFTER_COMPLETE 触发  
		if (!IEventTopics.DOC_AFTER_COMPLETE.equals(topic))  
			return;  
  
		// 2. 仅处理销售订单  
		if (!order.isSOTrx())  
			return;  
  
		// 3. 必须有打样类型  
		String samplingType = order.get_ValueAsString("samplingtype");  
		if (samplingType == null || samplingType.trim().isEmpty())  
			return;  
  
		String trxName = order.get_TrxName();  
  
		// 4. 防重复：已存在对应需求单则跳过  
		int existCount = DB.getSQLValue(trxName, "SELECT COUNT(*) FROM DY_SamplingDemand WHERE C_Order_ID=?",  
				order.getC_Order_ID());  
		if (existCount > 0)  
			return;  
  
		// 5. 取行号最小的订单明细（getLines() 默认按 Line ASC 排序）  
		MOrderLine[] lines = order.getLines(true, null);  
		if (lines == null || lines.length == 0)  
			return;  
		MOrderLine firstLine = lines[0];  

		// 6. 先创建需求单 PO 对象（不保存），设置 Created 供序列解析年月  
		MTable table = MTable.get(order.getCtx(), "dy_samplingdemand");  
		PO demand = table.getPO(0, trxName);  
		Timestamp now = new Timestamp(System.currentTimeMillis());  
		demand.set_ValueNoCheck("Created", now);  
  
		// 7. 生成单号（使用 AD_Sequence: DocumentNo_DY_SamplingDemand）  
		// 传入 demand，让 Env.parseVariable 能解析前缀中的 @Created<yyyyMM>@  
		String documentNo = MSequence.getDocumentNo(order.getAD_Client_ID(), "dy_samplingdemand", trxName, demand);  
  
		// 8. 根据打样类型确定状态和平面设计标志  
		// DS=设计打样 → 平面设计待受理(GW)，平面设计任务=Y  
		// 其他 → 工艺设计待受理(PW)，平面设计任务=N  
		boolean isDesignSampling = "DS".equals(samplingType);  
		String requestStatus = isDesignSampling ? "GW" : "PW";  
		String needGraphicDesign = isDesignSampling ? "Y" : "N";  
  
		String description = order.getDescription() != null ? order.getDescription() : "";  
  
		demand.set_ValueNoCheck("AD_Client_ID", order.getAD_Client_ID());  
		demand.set_ValueNoCheck("AD_Org_ID", order.getAD_Org_ID());  
		demand.set_ValueNoCheck("DocumentNo", documentNo); // 自动生成  
		demand.set_ValueNoCheck("Description", description); // 需求描述=销售订单描述  
		demand.set_ValueNoCheck("RequestDate", order.getDatePromised()); // 需求时间=销售订单.承诺交期  
		demand.set_ValueNoCheck("CustomerRequirement", description); // 客户要求=销售订单描述  
		demand.set_ValueNoCheck("RequestSource", "SO"); // 需求来源：打样销售订单  
		demand.set_ValueNoCheck("C_Order_ID", order.getC_Order_ID()); // 关联单号=销售订单  
		demand.set_ValueNoCheck("RequestType", samplingType); // 需求类型=销售订单.打样类型  
		demand.set_ValueNoCheck("QtySampling", firstLine.getQtyOrdered()); // 打样数量=销售订单.订单明细.数量（多行，取第一行）
		demand.set_ValueNoCheck("Amt", firstLine.getLineNetAmt());// 打样金额=销售订单.订单明细.LineNetAmt（取第一行）
		demand.set_ValueNoCheck("AD_User_ID", order.getCreatedBy()); // 发起人=销售订单的创建人
		demand.set_ValueNoCheck("RequestStatus", requestStatus); // 需求状态  
		demand.set_ValueNoCheck("IsNeedGraphicDesign", needGraphicDesign); // 是否需要平面设计  
		demand.set_ValueNoCheck("IsNeedProcessDesign", "Y"); // 是否需要工艺设计  
		demand.set_ValueNoCheck("RegistrationDate", now); // 登记时间（默认当前时间，复用 now）  
		demand.saveEx();  
  
		// 9. 复制销售订单附件到需求单  
		MAttachment srcAttachment = order.getAttachment(false);  
		if (srcAttachment != null && srcAttachment.getEntryCount() > 0) {  
			try {  
				MAttachment newAttachment = new MAttachment(order.getCtx(), table.getAD_Table_ID(), // DY_SamplingDemand 的 AD_Table_ID  
						demand.get_ID(), demand.get_UUID(), trxName); // 与 demand 同一事务，确保能查到未提交记录  
				for (MAttachmentEntry entry : srcAttachment.getEntries()) {  
					byte[] data = entry.getData(); // 自动触发懒加载  
					if (data != null) {  
						newAttachment.addEntry(entry.getName(), data);  
					}  
				}  
				newAttachment.saveEx();  
			} catch (Exception e) {  
				log.warning("复制销售订单附件到打样需求单失败，C_Order_ID=" + order.getC_Order_ID() + "，原因：" + e.getMessage());  
			}  
		}  
	}

	/**
	 * 采购订单完成后，自动维护物料在"采购价格表"最新版本下的标准价（PriceStd）。
	 * 规则：无价格记录 → 新建（PriceStd=订单行单价）；已有记录且 PriceStd=0 → 更新为订单行单价；
	 *      已有记录且 PriceStd≠0 → 不修改。
	 */
	void syncProductPriceOnComplete(MOrder order, String topic) {
		// 1. 仅在 DOC_AFTER_COMPLETE 触发
		if (!IEventTopics.DOC_AFTER_COMPLETE.equals(topic))
			return;

		// 2. 仅处理采购订单
		if (order.isSOTrx())
			return;

		String trxName = order.get_TrxName();

		// 3. 遍历订单明细，跳过没有物料的行（如费用行）
		MOrderLine[] lines = order.getLines();
		if (lines == null || lines.length == 0)
			return;

		// 4. 查询"采购价格表"下 IsActive='Y' 且 Created 最新的价格表版本
		String sql = "SELECT plv.M_PriceList_Version_ID " + "FROM M_PriceList_Version plv "
				+ "INNER JOIN M_PriceList pl ON (plv.M_PriceList_ID=pl.M_PriceList_ID) "
				+ "WHERE pl.IsSOPriceList='N' AND pl.Name='采购价格表' AND pl.IsActive='Y' "
				+ "AND plv.IsActive='Y' AND pl.AD_Client_ID=? ORDER BY plv.Created DESC";

		int priceListVersionId = DB.getSQLValue(trxName, sql, order.getAD_Client_ID());

		for (MOrderLine line : lines) {
			if (line.getM_Product_ID() <= 0)
				continue;

			if (priceListVersionId <= 0) {
				log.warning("未找到有效的采购价格表版本，跳过，M_Product_ID=" + line.getM_Product_ID()
						+ "，C_OrderLine_ID=" + line.getC_OrderLine_ID());
				continue;
			}

			// 5. 查询该物料在该价格表版本下是否已有价格记录
			MProductPrice pp = MProductPrice.get(order.getCtx(), priceListVersionId,
					line.getM_Product_ID(), trxName);

			// 6. 按规则维护 PriceStd
			if (pp == null) {
				// 无价格记录 → 新建，PriceStd=订单行单价（PriceList/PriceLimit 先按 0 处理）
				pp = new MProductPrice(order.getCtx(), priceListVersionId, line.getM_Product_ID(), trxName);
				pp.setAD_Org_ID(order.getAD_Org_ID());

				pp.setPrices(BigDecimal.ZERO, line.getPriceEntered(), BigDecimal.ZERO);

//				pp.setPriceList(BigDecimal.ZERO);
//				pp.setPriceStd(line.getPriceEntered());
//				pp.setPriceLimit(BigDecimal.ZERO);
				pp.saveEx();

				log.info("创建 M_ProductPrice，M_Product_ID=" + line.getM_Product_ID()
						+ "，M_PriceList_Version_ID=" + priceListVersionId
						+ "，PriceStd=" + line.getPriceEntered());
			} else if (BigDecimal.ZERO.compareTo(pp.getPriceStd()) == 0) {
				// 已有记录且 PriceStd=0 → 更新为订单行单价

				pp.setPrices(pp.getPriceList(), line.getPriceEntered(), pp.getPriceLimit());

//				pp.setPriceStd(line.getPriceEntered());

				pp.saveEx();
				log.info("更新 M_ProductPrice，M_Product_ID=" + line.getM_Product_ID()
						+ "，M_PriceList_Version_ID=" + priceListVersionId
						+ "，PriceStd=" + line.getPriceEntered());
			}
			// 已有记录且 PriceStd≠0 → 不修改
		}
	}

	/**
	 * 采购订单完成后，若物料在 M_Product_PO（《物料管理》→《采购》页签）中没有任何采购数据，
	 * 则自动创建一条：供应商=采购订单供应商，采购价格(PricePO)=订单行单价。
	 */
	void createProductPOOnComplete(MOrder order, String topic) {
		// 1. 仅在 DOC_AFTER_COMPLETE 触发
		if (!IEventTopics.DOC_AFTER_COMPLETE.equals(topic))
			return;

		// 2. 仅处理采购订单
		if (order.isSOTrx())
			return;

		String trxName = order.get_TrxName();

		// 3. 遍历订单明细，跳过没有物料的行（如费用行）
		MOrderLine[] lines = order.getLines();
		if (lines == null || lines.length == 0)
			return;
		for (MOrderLine line : lines) {
			int productId = line.getM_Product_ID();
			if (productId <= 0)
				continue;

			// 4. 该物料已有任何 M_Product_PO 记录则跳过（不限组织、不限供应商）
			MProductPO existing = new Query(order.getCtx(), MProductPO.Table_Name,
					MProductPO.COLUMNNAME_M_Product_ID + " = ?", trxName)
					.setParameters(productId)
					.first();
			if (existing != null) {
				log.info("物料已存在 M_Product_PO 采购数据，跳过，M_Product_ID=" + productId);
				continue;
			}

			// 5. 创建 M_Product_PO：供应商=采购订单供应商，采购价格=订单行单价
			MProductPO po = new MProductPO(order.getCtx(), 0, trxName);
			po.setAD_Org_ID(order.getAD_Org_ID());
			po.setM_Product_ID(productId);
			po.setC_BPartner_ID(order.getC_BPartner_ID());
			po.setIsCurrentVendor(true); // 该物料第一条采购记录，作为当前供应商

			// 补充：VendorProductNo 不能为空，默认取物料自身编号（M_Product.Value）
			MProduct product = MProduct.get(order.getCtx(), productId);
			po.setVendorProductNo(product != null ? product.getValue() : String.valueOf(productId));

			po.setPricePO(line.getPriceEntered()); // 采购价格（《采购》页签【采购价格】字段）
			po.saveEx();

			log.info("创建 M_Product_PO，M_Product_ID=" + productId
					+ "，C_BPartner_ID=" + order.getC_BPartner_ID()
					+ "，PricePO=" + line.getPriceEntered());
		}
	}

}