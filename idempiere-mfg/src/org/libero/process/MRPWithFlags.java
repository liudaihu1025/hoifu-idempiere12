package org.libero.process;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.DB;

/**
 * MRP 扩展子类 — 新增 GenerateWO（是否生成工单）参数控制 + 预览明细收集
 *
 * 设计原则： - 不修改父类 MRP.java 任何一行代码 - 覆写 prepare()：先调用 super.prepare()
 * 复用父类参数解析，再读取子类独有的 GenerateWO 参数 - 覆写 createSupply()：当产品为 BOM
 * 类型且用户选择不生成工单时跳过，其余分支照常调用父类 - 覆写 createPPOrder() /
 * createRequisition()：在父类执行完（saveEx 写入当前事务）后， 用同一事务查询刚生成的记录，组装 PreviewRow
 * 存入内存列表 - doIt() / runMRP() / deleteMRP() 等完全继承，不覆写 - 模拟模式下父类 doIt() 最终会
 * rollback()，但 PreviewRow 已在 Java 内存中，不受回滚影响
 */
public class MRPWithFlags extends MRP
{
	// ==================== 子类独有参数 ====================

	/** 是否生成生产工单（默认 true，即默认生成） */
	private boolean p_GenerateWO = true;

	/** 组织 ID（父类 p_AD_Org_ID 为 private，子类自行读取） */
	private int p_AD_Org_ID2 = 0;
	/** 仓库 ID（父类 p_M_Warehouse_ID 为 private，子类自行读取） */
	private int p_M_Warehouse_ID2 = 0;

	// ==================== 预览明细数据（Java 内存，不受事务回滚影响） ====================

	/** 预览数据行 — 通用结构，用于表单渲染明细表格 */
	public static class PreviewRow
	{
		public final String documentNo;    // 单据号
		public final String productValue;  // 物料编码
		public final String productName;   // 物料名称
		public final String date1;         // 日期1（工单日期/申购日期/配送日期）
		public final String date2;         // 日期2（承诺交期/需求日期/承诺日期）
		public final String quantity;      // 数量
		public final String inTransitQty;  // 在途数量（仅申购单有意义）
		public final String uom;           // 单位
		public final String extra;         // 附加信息（资源/供应商/仓库）

		public PreviewRow(String documentNo, String productValue, String productName,
				String date1, String date2, String quantity, String inTransitQty, String uom, String extra)
		{
			this.documentNo = documentNo;
			this.productValue = productValue;
			this.productName = productName;
			this.date1 = date1;
			this.date2 = date2;
			this.quantity = quantity;
			this.inTransitQty = inTransitQty;
			this.uom = uom;
			this.extra = extra;
		}

		public String getDocumentNo()   { return documentNo; }
		public String getProductValue() { return productValue; }
		public String getProductName()  { return productName; }
		public String getDate1()        { return date1; }
		public String getDate2()        { return date2; }
		public String getQuantity()     { return quantity; }
		public String getInTransitQty() { return inTransitQty; }
		public String getUom()          { return uom; }
		public String getExtra()        { return extra; }
	}

	/** 生产工单预览列表 */
	private List<PreviewRow> previewMOList = new ArrayList<>();
	/** 申购单预览列表 */
	private List<PreviewRow> previewMRList = new ArrayList<>();
	/** 配送订单预览列表 */
	private List<PreviewRow> previewDOList = new ArrayList<>();

	// ==================== 只读 Getter ====================

	/** 获取生产工单预览列表 */
	public List<PreviewRow> getPreviewMOList() { return previewMOList; }
	/** 获取申购单预览列表 */
	public List<PreviewRow> getPreviewMRList() { return previewMRList; }
	/** 获取配送订单预览列表 */
	public List<PreviewRow> getPreviewDOList() { return previewDOList; }

	// ==================== 覆写方法 ====================

	/**
	 * 覆写 prepare()：复用父类参数解析 + 读取子类独有的 GenerateWO 参数
	 *
	 * 父类 prepare() 已处理：AD_Org_ID, S_Resource_ID, M_Warehouse_ID,
	 *   IsRequiredDRP, Version, IsSimulate, DeleteMRP
	 * 父类对未知参数名仅打印 SEVERE 日志，不会抛异常，属于可接受的日志噪音
	 */
	@Override
	protected void prepare()
	{
		// 先调用父类，复用全部已有参数解析逻辑
		super.prepare();

		// 再遍历参数列表，读取子类独有的参数
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++)
		{
			String name = para[i].getParameterName();
			if (para[i].getParameter() == null)
				;
			else if (name.equals("GenerateWO"))
			{
				p_GenerateWO = para[i].getParameterAsBoolean();
			}
			else if (name.equals("AD_Org_ID"))
			{
				p_AD_Org_ID2 = para[i].getParameterAsInt();
			}
			else if (name.equals("M_Warehouse_ID"))
			{
				p_M_Warehouse_ID2 = para[i].getParameterAsInt();
			}
		}
	}

	/**
	 * 覆写 createSupply()：在 BOM 分支加入 p_GenerateWO 判断
	 *
	 * 当落入 BOM 分支（product.isBOM() 且非采购）且 p_GenerateWO=false 时，跳过工单创建；
	 * 其余分支（DRP / 采购 / BOM+生成工单）原样调用 super.createSupply()
	 */
	@Override
	protected void createSupply(int AD_Org_ID, int PP_MRP_ID, MProduct product,
			BigDecimal QtyPlanned, Timestamp DemandDateStartSchedule)
			throws AdempiereException, SQLException
	{
		// BOM 产品且用户选择不生成工单 → 跳过
		if (product.isBOM() && !product.isPurchased() && !p_GenerateWO)
		{
			log.info("用户选择不生成工单，跳过 BOM 产品: " + product.getValue()
					+ " (M_Product_ID=" + product.getM_Product_ID() + ")");
			return;
		}

		// 其余情况：原样调用父类逻辑
		super.createSupply(AD_Org_ID, PP_MRP_ID, product, QtyPlanned, DemandDateStartSchedule);
	}

	/**
	 * 覆写 createPPOrder()：父类执行完 saveEx() 后，在同一事务查询刚生成的工单，收集预览数据
	 *
	 * 父类执行后 PP_Order 已写入当前事务（含 DocumentNo），可在同一事务内查询。
	 * 模拟模式下 doIt() 最终会 rollback()，但 previewMOList 已在 Java 内存中，不受影响。
	 */
	@Override
	protected void createPPOrder(int AD_Org_ID, int PP_MRP_ID, MProduct product,
			BigDecimal QtyPlanned, Timestamp DemandDateStartSchedule)
			throws AdempiereException, SQLException
	{
		// 先让父类执行（saveEx 写入当前事务，含 DocumentNo 生成）
		super.createPPOrder(AD_Org_ID, PP_MRP_ID, product, QtyPlanned, DemandDateStartSchedule);

		// 在同一事务内查询刚生成的最新工单
		try
		{
			String sql = "SELECT DocumentNo, DateOrdered, DatePromised, QtyOrdered, C_UOM_ID, S_Resource_ID "
					+ "FROM PP_Order "
					+ "WHERE AD_Client_ID=? AND AD_Org_ID=? AND M_Product_ID=? "
					+ "ORDER BY PP_Order_ID DESC";
			List<Object> row = DB.getSQLValueObjectsEx(get_TrxName(), sql,
					getAD_Client_ID(), AD_Org_ID, product.getM_Product_ID());
			if (row != null && row.size() >= 6)
			{
				String documentNo = row.get(0) != null ? row.get(0).toString() : "";
				String date1 = row.get(1) != null ? row.get(1).toString().substring(0, Math.min(10, row.get(1).toString().length())) : "";
				String date2 = row.get(2) != null ? row.get(2).toString().substring(0, Math.min(10, row.get(2).toString().length())) : "";
				String qty = row.get(3) != null ? row.get(3).toString() : "";
				String uom = "";
				if (row.get(4) != null)
				{
					try { uom = MUOM.get(getCtx(), ((Number)row.get(4)).intValue()).getUOMSymbol(); }
					catch (Exception e) { uom = row.get(4).toString(); }
				}
				String resource = "";
				if (row.get(5) != null)
				{
					try { resource = org.compiere.model.MResource.get(getCtx(), ((Number)row.get(5)).intValue()).getName(); }
					catch (Exception e) { resource = row.get(5).toString(); }
				}
				previewMOList.add(new PreviewRow(documentNo, product.getValue(), product.getName(),
						date1, date2, qty, "", uom, resource));
			}
		}
		catch (Exception e)
		{
			log.log(Level.WARNING, "收集生产工单预览数据失败", e);
		}
	}

	/**
	 * 覆写 createRequisition()：父类执行完 saveEx() 后，在同一事务查询刚生成的申购单行，收集预览数据
	 *
	 * 父类使用分组缓存机制，可能新建申购单头也可能向已有头追加行。 通过查询当前事务中最新的
	 * M_RequisitionLine（按产品+仓库匹配）获取刚写入的数据。
	 */
	@Override
	protected void createRequisition(int AD_Org_ID, int PP_MRP_ID, MProduct product,
			BigDecimal QtyPlanned, Timestamp DemandDateStartSchedule)
			throws AdempiereException, SQLException
	{
		// 先让父类执行（saveEx 写入当前事务）
		super.createRequisition(AD_Org_ID, PP_MRP_ID, product, QtyPlanned, DemandDateStartSchedule);

		// 在同一事务内查询刚生成/更新的申购单行
		try
		{
			String sql = "SELECT r.DocumentNo, r.DateDoc, r.DateRequired, rl.Qty, rl.C_UOM_ID, bp.Name "
					+ "FROM M_RequisitionLine rl "
					+ "INNER JOIN M_Requisition r ON (r.M_Requisition_ID = rl.M_Requisition_ID) "
					+ "LEFT JOIN C_BPartner bp ON (bp.C_BPartner_ID=rl.C_BPartner_ID) "
					+ "WHERE rl.AD_Client_ID=? AND rl.AD_Org_ID=? AND rl.M_Product_ID=? "
					+ "ORDER BY rl.M_RequisitionLine_ID DESC";
			List<Object> row = DB.getSQLValueObjectsEx(get_TrxName(), sql, getAD_Client_ID(), AD_Org_ID,
					product.getM_Product_ID());
			if (row != null && row.size() >= 6)
			{
				String documentNo = row.get(0) != null ? row.get(0).toString() : "";
				String date1 = row.get(1) != null ? row.get(1).toString().substring(0, Math.min(10, row.get(1).toString().length())) : "";
				String date2 = row.get(2) != null ? row.get(2).toString().substring(0, Math.min(10, row.get(2).toString().length())) : "";
				String qty = row.get(3) != null ? row.get(3).toString() : "";
				String uom = "";
				if (row.get(4) != null)
				{
					try { uom = MUOM.get(getCtx(), ((Number)row.get(4)).intValue()).getUOMSymbol(); }
					catch (Exception e) { uom = row.get(4).toString(); }
				}
				String supplier = row.get(5) != null ? row.get(5).toString() : "";

				// 查询在途数量：M_StorageReservation 中 IsSOTrx='N' 的预留总量
				String inTransitQty = "";
				try
				{
					String inTransitSql = "SELECT COALESCE(SUM(sr.Qty), 0) "
							+ "FROM M_StorageReservation sr "
							+ "WHERE sr.AD_Client_ID=? AND sr.AD_Org_ID=? "
							+ "AND sr.M_Product_ID=? AND sr.M_Warehouse_ID=? "
							+ "AND sr.IsSOTrx='N'";
					BigDecimal inTransit = DB.getSQLValueBD(get_TrxName(), inTransitSql,
							getAD_Client_ID(), p_AD_Org_ID2,
							product.getM_Product_ID(), p_M_Warehouse_ID2);
					if (inTransit != null && inTransit.signum() != 0)
					{
						inTransitQty = inTransit.toPlainString();
					}
				}
				catch (Exception ex)
				{
					log.log(Level.WARNING, "查询在途数量失败: " + product.getValue(), ex);
				}

				previewMRList.add(new PreviewRow(documentNo, product.getValue(), product.getName(),
						date1, date2, qty, inTransitQty, uom, supplier));
			}
		}
		catch (Exception e)
		{
			log.log(Level.WARNING, "收集申购单预览数据失败", e);
		}
	}

	/**
	 * 覆写 createDDOrder()：父类执行完 saveEx() 后，在同一事务查询刚生成的配送订单行，收集预览数据
	 *
	 * 配送订单涉及网络分配（可能生成多条），通过查询当前事务中最新的 DD_OrderLine 获取数据。
	 */
	@Override
	protected void createDDOrder(int AD_Org_ID, int PP_MRP_ID, MProduct product,
			BigDecimal QtyPlanned, Timestamp DemandDateStartSchedule)
			throws AdempiereException, SQLException
	{
		// 先让父类执行（saveEx 写入当前事务）
		super.createDDOrder(AD_Org_ID, PP_MRP_ID, product, QtyPlanned, DemandDateStartSchedule);

		// 在同一事务内查询刚生成的最新配送订单行
		try
		{
			String sql = "SELECT d.DocumentNo, d.DateOrdered, d.DatePromised, dl.QtyOrdered, dl.C_UOM_ID, w.Name "
					+ "FROM DD_OrderLine dl "
					+ "INNER JOIN DD_Order d ON (d.DD_Order_ID=dl.DD_Order_ID) "
					+ "LEFT JOIN M_Warehouse w ON (w.M_Warehouse_ID=d.M_Warehouse_ID) "
					+ "WHERE dl.AD_Client_ID=? AND dl.M_Product_ID=? "
					+ "ORDER BY dl.DD_OrderLine_ID DESC";
			List<Object> row = DB.getSQLValueObjectsEx(get_TrxName(), sql,
					getAD_Client_ID(), product.getM_Product_ID());
			if (row != null && row.size() >= 6)
			{
				String documentNo = row.get(0) != null ? row.get(0).toString() : "";
				String date1 = row.get(1) != null ? row.get(1).toString().substring(0, Math.min(10, row.get(1).toString().length())) : "";
				String date2 = row.get(2) != null ? row.get(2).toString().substring(0, Math.min(10, row.get(2).toString().length())) : "";
				String qty = row.get(3) != null ? row.get(3).toString() : "";
				String uom = "";
				if (row.get(4) != null)
				{
					try { uom = MUOM.get(getCtx(), ((Number)row.get(4)).intValue()).getUOMSymbol(); }
					catch (Exception e) { uom = row.get(4).toString(); }
				}
				String warehouse = row.get(5) != null ? row.get(5).toString() : "";
				previewDOList.add(new PreviewRow(documentNo, product.getValue(), product.getName(),
						date1, date2, qty, "", uom, warehouse));
			}
		}
		catch (Exception e)
		{
			log.log(Level.WARNING, "收集配送订单预览数据失败", e);
		}
	}
}
