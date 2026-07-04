/**
 * Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
 * If you shall try to cheat and find a loophole in this license, then KARMA will exact your share.
 * and your worldly gain shall come to naught and those who share shall gain eventually above you.
 * In compliance with previous GPLv2.0 works of ComPiere USA, eEvolution MEXICO, iDempiere contributors and Mutlimage SLOVAKIA
 */
package org.libero.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.model.MDocType;
import org.compiere.model.MLocator;
import org.compiere.model.MProduct;
import org.compiere.model.MStorageOnHand;
import org.compiere.model.MUOM;
import org.compiere.model.MWarehouse;
import org.compiere.model.Query;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.libero.tables.I_PP_Cost_Collector;
import org.libero.tables.X_PP_Cost_Collector;
import org.libero.tables.X_PP_Order_BOMLine;
import org.zkoss.zk.ui.Executions;
/**
 * PP Order BOM Line Model.
 *  
 * @author Victor Perez www.e-evolution.com     
 * @author Teo Sarca, www.arhipac.ro
 */
public class MPPOrderBOMLine extends X_PP_Order_BOMLine
{
	private static final long serialVersionUID = 1L;
	
	public static MPPOrderBOMLine forM_Product_ID(Properties ctx, int PP_Order_ID, int M_Product_ID, String trxName)
	{
		//TODO: vpj-cd What happen when a product it more the time in Order
		final String whereClause = COLUMNNAME_PP_Order_ID+"=? AND "+COLUMNNAME_M_Product_ID+"=?";
		return new Query(ctx, Table_Name, whereClause, trxName)
			.setParameters(PP_Order_ID, M_Product_ID)
			.firstOnly();
	}
	
	public MPPOrderBOMLine(Properties ctx, int PP_Order_BOMLine_ID, String trxName)
	{
		super (ctx, PP_Order_BOMLine_ID, trxName);  
		if (PP_Order_BOMLine_ID == 0)
		{
			setDefault();
		}	
	}	//	PP_Order_BOMLine_ID


	public MPPOrderBOMLine(Properties ctx, ResultSet rs,String trxName)
	{
		super (ctx, rs,trxName);
	}	//	MOrderLine
	
	/**
	 * Peer constructor
	 * @param bomLine
	 * @param PP_Order_ID
	 * @param PP_Order_BOM_ID
	 * @param M_Warehouse_ID
	 * @param trxName
	 */
	public MPPOrderBOMLine(MPPProductBOMLine bomLine,
			int PP_Order_ID, int PP_Order_BOM_ID, int M_Warehouse_ID,
			String trxName)
	{
		this(bomLine.getCtx(), 0, trxName);
		
		setPP_Order_BOM_ID(PP_Order_BOM_ID);
		setPP_Order_ID(PP_Order_ID);
		setM_Warehouse_ID(M_Warehouse_ID);
		//
		setM_ChangeNotice_ID(bomLine.getM_ChangeNotice_ID());
		setDescription(bomLine.getDescription());
		setHelp(bomLine.getHelp());
		setAssay(bomLine.getAssay());
		setQtyBatch(bomLine.getQtyBatch());
		setQtyBOM(bomLine.getQtyBOM());
		setIsQtyPercentage(bomLine.isQtyPercentage());
		setComponentType(bomLine.getComponentType());
		setC_UOM_ID(bomLine.getC_UOM_ID());
		setForecast(bomLine.getForecast());
		setIsCritical(bomLine.isCritical());
		setIssueMethod(bomLine.getIssueMethod());
		setLeadTimeOffset(bomLine.getLeadTimeOffset());
		setM_AttributeSetInstance_ID(bomLine.getM_AttributeSetInstance_ID());
		setM_Product_ID(bomLine.getM_Product_ID());
		setScrap(bomLine.getScrap());
		setValidFrom(bomLine.getValidFrom());
		setValidTo(bomLine.getValidTo());
		setBackflushGroup(bomLine.getBackflushGroup());
		// 添加主物料字段复制
		setKeymat(bomLine.getKeymat());
	}

	/**
	 * Parent (PP_Order)
	 */
	private MPPOrder m_parent = null;
	
	/**
	 * Do we need to explode this BOM line. Set when ComponentType is Phantom and m_qtyRequieredPhantom != null.
	 * If set, the line is exploded on after save
	 */
	private boolean m_isExplodePhantom = false;
	/**
	 * Qty used for exploding this BOM Line.
	 */
	private BigDecimal m_qtyRequieredPhantom = null;
	
	@Override
	protected boolean beforeSave(boolean newRecord) {
		boolean isFromUI = Executions.getCurrent() != null;

		if (isFromUI) {
			MPPOrder order = getParent();
			if (order != null) {
				if (isSpecialOrder(order)) {
					if (isLockedForSpecialOrder(order)) {
						// throw new AdempiereException("当前工单状态不允许修改BOM子件");
					}
				} else {
					String orderStatus = order.get_ValueAsString("OrderStatus");
					if (!"Ready".equals(orderStatus)) {
						// throw new AdempiereException("普通工单只有待发布状态才允许新增或修改BOM子件");
					}
					if (!isActive()) {
						// throw new AdempiereException("De-Activating an BOM Line is not allowed");
					}
					if (!newRecord && is_ValueChanged(COLUMNNAME_M_Product_ID)) {
						throw new AdempiereException("Changing Product is not allowed");
					}
				}
			} else {
				if (!isActive()) {
					throw new AdempiereException("De-Activating an BOM Line is not allowed");
				}
				if (!newRecord && is_ValueChanged(COLUMNNAME_M_Product_ID)) {
					throw new AdempiereException("Changing Product is not allowed");
				}
			}
		}

		// 自动填充 PP_Order_BOM_ID（保留，不受 isFromUI 限制）
		if (newRecord && getPP_Order_BOM_ID() == 0 && getPP_Order_ID() > 0) {
			final String sql = "SELECT PP_Order_BOM_ID FROM PP_Order_BOM WHERE PP_Order_ID=? AND IsActive='Y' FETCH FIRST 1 ROWS ONLY";
			int bomId = DB.getSQLValueEx(get_TrxName(), sql, getPP_Order_ID());
			if (bomId > 0) {
				setPP_Order_BOM_ID(bomId);
			}
		}

		// 以下原有逻辑保持不变
		if (getLine() == 0) {
			String sql = "SELECT COALESCE(MAX(" + COLUMNNAME_Line + "),0)+10 FROM " + Table_Name + " WHERE "
					+ COLUMNNAME_PP_Order_ID + "=?";
			int ii = DB.getSQLValueEx(get_TrxName(), sql, getPP_Order_ID());
			setLine(ii);
		}

		if (newRecord && COMPONENTTYPE_Phantom.equals(getComponentType())) {
			m_qtyRequieredPhantom = getQtyRequiered();
			m_isExplodePhantom = true;
			setQtyRequiered(Env.ZERO);
		}

		if (newRecord || is_ValueChanged(COLUMNNAME_C_UOM_ID) || is_ValueChanged(COLUMNNAME_QtyEntered)
				|| is_ValueChanged(COLUMNNAME_QtyRequiered)) {
			int precision = MUOM.getPrecision(getCtx(), getC_UOM_ID());
			setQtyEntered(getQtyEntered().setScale(precision, RoundingMode.UP));
			setQtyRequiered(getQtyRequiered().setScale(precision, RoundingMode.UP));
		}

		if (is_ValueChanged(MPPOrderBOMLine.COLUMNNAME_QtyDelivered)
				|| is_ValueChanged(MPPOrderBOMLine.COLUMNNAME_QtyRequiered)) {
			reserveStock();
		}

		return true;
	}
	@Override
	protected boolean afterSave(boolean newRecord, boolean success)
	{
		if (!success)
			return false;
		explodePhantom();
		return true;
	}
	
	@Override
	protected boolean beforeDelete() {
		MPPOrder order = getParent();

		if (order != null) {
			if (isSpecialOrder(order)) {
				// 研发/打样工单：Completed/Shipped/Stored 状态下不允许删除
				if (isLockedForSpecialOrder(order)) {
					// throw new AdempiereException("当前工单状态不允许删除BOM子件");
				}
			} else {
				// 普通工单：只有 Ready 状态才允许删除
				String orderStatus = order.get_ValueAsString("OrderStatus");
				if (!"Ready".equals(orderStatus)) {
					// throw new AdempiereException("普通工单只有已发布状态才允许删除BOM子件");
				}
			}
		}

		// 原有逻辑：释放库存预留
		setQtyRequiered(Env.ZERO);
		reserveStock();
		return true;
	}

	/**
	 * Explode Phantom Items
	 * TODO: check if BOM and BOM Lines are valid
	 */
	private void explodePhantom()
	{
		if(m_isExplodePhantom && m_qtyRequieredPhantom != null)
		{
			MProduct parent = MProduct.get(getCtx(), getM_Product_ID());
			int PP_Product_BOM_ID = MPPProductBOM.getBOMSearchKey(parent);
			if (PP_Product_BOM_ID <= 0)
			{
				return;
			}
			MPPProductBOM bom = MPPProductBOM.get(getCtx(), PP_Product_BOM_ID);
			if (bom != null)
			{
				for(MPPProductBOMLine PP_Product_BOMline : bom.getLines())
				{
					MPPOrderBOMLine PP_Order_BOMLine = new MPPOrderBOMLine(PP_Product_BOMline,
																getPP_Order_ID(), getPP_Order_BOM_ID(),
																getM_Warehouse_ID(),
																get_TrxName());
					PP_Order_BOMLine.setAD_Org_ID(getAD_Org_ID());
					PP_Order_BOMLine.setQtyPlusScrap(m_qtyRequieredPhantom);
					PP_Order_BOMLine.saveEx();
				}
			}
			m_isExplodePhantom = false;
		}
	}

	@Override
	public MProduct getM_Product()
	{
		return MProduct.get(getCtx(), getM_Product_ID());
	}

	@Override
	public MUOM getC_UOM()
	{
		return MUOM.get(getCtx(), getC_UOM_ID());
	}
	
	@Override
	public MWarehouse getM_Warehouse()
	{
		return MWarehouse.get(getCtx(), getM_Warehouse_ID());
	}
	
	/**
	 * Qty Required for a Phantom Component. The Qty that will be exploded after line is saved.
	 * @return
	 */
	public BigDecimal getQtyRequieredPhantom()
	{
		return m_qtyRequieredPhantom != null ? m_qtyRequieredPhantom : Env.ZERO;
	}

	/**
	 * 	Get Parent
	 *	@return PP_Order
	 */
	public MPPOrder getParent()
	{
		int id = getPP_Order_ID();
		if (id <= 0)
		{
			m_parent = null;
			return null;
		}
		if (m_parent == null || m_parent.get_ID() != id)
		{
			m_parent = new MPPOrder(getCtx(), id, get_TrxName());
		}
		return m_parent;
	}	//	getParent
	
	private boolean isSpecialOrder(MPPOrder order) {
		if (order == null || order.getC_DocTypeTarget_ID() <= 0)
			return false;
		MDocType docType = MDocType.get(getCtx(), order.getC_DocTypeTarget_ID());
		if (docType == null)
			return false;
		String name = docType.getName();
		return name != null && (name.contains("研发") || name.contains("打样"));
	}

	private boolean isLockedForSpecialOrder(MPPOrder order) {
		String orderStatus = order.get_ValueAsString("OrderStatus");
		return "Completed".equals(orderStatus) || "Shipped".equals(orderStatus) || "Stored".equals(orderStatus);
	}

	/**
	 * @return UOM precision
	 */
	public int getPrecision()
	{
		return MUOM.getPrecision(getCtx(), getC_UOM_ID());
	}
	
	/**
	 * Return Unified BOM Qty Multiplier 
	 * @return If is percentage then QtyBatch / 100 will be returned, else QtyBOM. 
	 */
	public BigDecimal getQtyMultiplier()
	{
		BigDecimal qty;
		if (isQtyPercentage())
		{
			qty = getQtyBatch().divide(Env.ONEHUNDRED, 8, RoundingMode.HALF_UP);
		}
		else
		{
			qty = getQtyBOM();
		}
		return qty;
	}

	/**
	 * Qty Ordered + Scrap
	 * @param QtyOrdered
	 */
	public void setQtyPlusScrap(BigDecimal QtyOrdered)
	{
		BigDecimal multiplier = getQtyMultiplier();	
		BigDecimal qty = QtyOrdered.multiply(multiplier).setScale(8, RoundingMode.UP);
		
		if (isComponentType(COMPONENTTYPE_Component,COMPONENTTYPE_Phantom
							,COMPONENTTYPE_Packing
							,COMPONENTTYPE_By_Product
							,COMPONENTTYPE_Co_Product))
		{
			setQtyRequiered(qty);
		}
		else if (isComponentType(COMPONENTTYPE_Tools))
		{
			setQtyRequiered(multiplier);
		}
		else
		{
			throw new AdempiereException("@NotSupported@ @ComponentType@ "+getComponentType());
		}
		//
		// Set Scrap of Component
		BigDecimal qtyScrap = getScrap();
		if (qtyScrap.signum() != 0)
		{
			qtyScrap = qtyScrap.divide(Env.ONEHUNDRED, 8, BigDecimal.ROUND_UP);
			setQtyRequiered(getQtyRequiered().divide(Env.ONE.subtract(qtyScrap), 8, BigDecimal.ROUND_HALF_UP));
		}
	}
	
	@Override  
	public void setQtyRequiered (BigDecimal QtyRequired)  
	{  
	    if (QtyRequired != null && getC_UOM_ID() != 0)  
	    {  
	        int precision = getPrecision();  
	        // 改为向上取整  
	        QtyRequired = QtyRequired.setScale(precision, RoundingMode.UP);  
	          
	        // 确保最小值不为0  
	        if (QtyRequired.compareTo(Env.ZERO) > 0)  
	        {  
	            BigDecimal minValue = BigDecimal.ONE.scaleByPowerOfTen(-precision);  
	            if (QtyRequired.compareTo(minValue) < 0)  
	            {  
	                QtyRequired = minValue;  
	            }  
	        }  
	    }  
	    super.setQtyRequiered (QtyRequired);  
	}

	@Override  
	public void setQtyReserved (BigDecimal QtyReserved)  
	{  
	    if (QtyReserved != null && getC_UOM_ID() != 0)  
	    {  
	        int precision = getPrecision();  
	        // 改为向上取整  
	        QtyReserved = QtyReserved.setScale(precision, RoundingMode.UP);  
	          
	        // 确保最小值不为0  
	        if (QtyReserved.compareTo(Env.ZERO) > 0)  
	        {  
	            BigDecimal minValue = BigDecimal.ONE.scaleByPowerOfTen(-precision);  
	            if (QtyReserved.compareTo(minValue) < 0)  
	            {  
	                QtyReserved = minValue;  
	            }  
	        }  
	    }  
	    super.setQtyReserved (QtyReserved);  
	}
	
	/**
	 * @return Qty Open (Required - Delivered)
	 */
	public BigDecimal getQtyOpen()
	{
		return getQtyRequiered().subtract(getQtyDelivered());
	}
	
	/** Storage Qty On Hand */
	private BigDecimal m_qtyOnHand = null;
	/** Storage Qty Available */
	private BigDecimal m_qtyAvailable = null;

	/**
	 * Load Storage Info
	 * @param reload
	 */
	private void loadStorage(boolean reload)
	{
		if (!reload && m_qtyOnHand != null && m_qtyAvailable != null)
		{
			return;
		}
		//
		final String sql = "SELECT "
							+" bomQtyAvailable("+COLUMNNAME_M_Product_ID+", "+COLUMNNAME_M_Warehouse_ID+", 0)"
							+",bomQtyOnHand("+COLUMNNAME_M_Product_ID+", "+COLUMNNAME_M_Warehouse_ID+", 0)"
							+" FROM "+Table_Name
							+" WHERE "+COLUMNNAME_PP_Order_BOMLine_ID+"=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try
		{
			pstmt = DB.prepareStatement(sql, get_TrxName());
			DB.setParameters(pstmt, new Object[]{get_ID()});
			rs = pstmt.executeQuery();
			if (rs.next())
			{
				m_qtyAvailable = rs.getBigDecimal(1);
				m_qtyOnHand = rs.getBigDecimal(2);
			}
		}
		catch (SQLException e)
		{
			throw new DBException(e, sql);
		}
		finally
		{
			DB.close(rs, pstmt);
			rs = null; pstmt = null;
		}
	}

	/**
	 * @return storage Available Qty
	 */
	public BigDecimal getQtyAvailable()
	{
		loadStorage(false);
		return m_qtyAvailable;
	}
	
	/**
	 * @return recorded Qty Usage Variance so far
	 */
	public BigDecimal getQtyVariance()
	{
		final String whereClause = I_PP_Cost_Collector.COLUMNNAME_PP_Order_BOMLine_ID+"=?"
		+" AND "+I_PP_Cost_Collector.COLUMNNAME_PP_Order_ID+"=?"
		+" AND "+I_PP_Cost_Collector.COLUMNNAME_DocStatus+" IN (?,?)"
		+" AND "+I_PP_Cost_Collector.COLUMNNAME_CostCollectorType+"=?"
		;
		BigDecimal qtyUsageVariance = new Query(getCtx(), I_PP_Cost_Collector.Table_Name, whereClause, get_TrxName())
		.setParameters(new Object[]{
				getPP_Order_BOMLine_ID(),
				getPP_Order_ID(),
				X_PP_Cost_Collector.DOCSTATUS_Completed,
				X_PP_Cost_Collector.DOCSTATUS_Closed,
				X_PP_Cost_Collector.COSTCOLLECTORTYPE_UsegeVariance
		})
		.sum(I_PP_Cost_Collector.COLUMNNAME_MovementQty);
		//
		return qtyUsageVariance;
	}

	/**
	 * @return storage Qty On Hand
	 */
	public BigDecimal getQtyOnHand()
	{
		loadStorage(false);
		return m_qtyOnHand;
	}
	
	/**
	 * @param componentTypes one or more component types
	 * @return true of Component Type is any of following types
	 */
	public boolean isComponentType(String ... componentTypes)
	{
		String currentType = getComponentType();
		for (String type : componentTypes)
		{
			if (currentType.equals(type))
			{
				return true;
			}
		}
		return false;
	}
	
	public boolean isCoProduct()
	{
		return isComponentType(COMPONENTTYPE_Co_Product);
	}
	
	public boolean isByProduct()
	{
		return isComponentType(COMPONENTTYPE_By_Product);
	}
	
	public boolean isComponent()
	{
		return isComponentType(COMPONENTTYPE_Component, COMPONENTTYPE_Packing);
	}
	
	/**
	 * 	Add to Description
	 *	@param description text
	 */
	public void addDescription (String description)
	{
		String desc = getDescription();
		if (desc == null)
			setDescription(description);
		else	
			setDescription(desc + " | " + description);
	}	//	addDescription

	/**
	 * Set default values
	 */
	private void setDefault()
	{
		setDescription("");
		setQtyDelivered(Env.ZERO);
		setQtyPost(Env.ZERO);
		setQtyReject(Env.ZERO);
		setQtyRequiered(Env.ZERO);
		setQtyReserved(Env.ZERO);
		setQtyScrap(Env.ZERO);
	}
	
	/**
	 * Reserve Inventory for this BOM Line
	 */
	protected void reserveStock()
	{
		final int header_M_Warehouse_ID = getParent().getM_Warehouse_ID();

		//	Check/set WH/Org
		if (header_M_Warehouse_ID != 0) //	enforce WH
		{
			if (header_M_Warehouse_ID != getM_Warehouse_ID())
				setM_Warehouse_ID(header_M_Warehouse_ID);
			if (getAD_Org_ID() != getAD_Org_ID())
				setAD_Org_ID(getAD_Org_ID());
		}
		//
		final BigDecimal target = getQtyRequiered();
		final BigDecimal difference = target.subtract(getQtyReserved()).subtract(getQtyDelivered());
		log.info("Line=" + getLine() + " - Target=" + target + ",Difference=" + difference + " - Required=" + getQtyRequiered()
				+ ",Reserved=" + getQtyReserved() + ",Delivered=" + getQtyDelivered());
		if (difference.signum() == 0)
		{
			return;
		}

		//	Check Product - Stocked and Item
		MProduct product = getM_Product();
		if (!product.isStocked())
		{
			return;
		}
		BigDecimal reserved = difference;//TODO do you need to update storage?
		int M_Locator_ID = getM_Locator_ID(reserved);
		//	Update Storage
//		if (!MStorageOnHand.add(getCtx(), getM_Warehouse_ID(), M_Locator_ID,
//				getM_Product_ID(), getM_AttributeSetInstance_ID(), Env.ZERO, get_TrxName()))
		if (!MStorageOnHand.add(getCtx(), M_Locator_ID, getM_Product_ID(), getM_AttributeSetInstance_ID(), Env.ZERO,
				null, null, get_TrxName()))
		{
			throw new AdempiereException("Storage Update  Error!");
		}
		//	update line
		setQtyReserved(getQtyReserved().add(difference));
	} //	reserveStock
	
	/**
	 * @param qty
	 * @return Storage locator for current product/asi/warehouse and qty
	 * @see MStorage#getM_Locator_ID(int, int, int, BigDecimal, String)
	 */
	private int getM_Locator_ID(BigDecimal qty)
	{
		int M_Locator_ID = 0;
		int M_ASI_ID = getM_AttributeSetInstance_ID();
		// Get existing Locator
		if (M_ASI_ID != 0)
		{
			M_Locator_ID = MStorageOnHand.getM_Locator_ID(getM_Warehouse_ID(), getM_Product_ID(), M_ASI_ID, qty, get_TrxName());
		}
		// Get Default
		if (M_Locator_ID == 0)
		{
			M_Locator_ID = getM_Locator_ID();
		}
		// Get Default Locator for Warehouse - teo_sarca [ 2724743 ]
		if (M_Locator_ID == 0)
		{
			MLocator locator = MWarehouse.get(getCtx(), getM_Warehouse_ID()).getDefaultLocator();
			if (locator != null)
			{
				M_Locator_ID = locator.get_ID();
			}
		}
		return M_Locator_ID;
	}
	
	@Override
	public String toString()
	{
		return getClass().getSimpleName() +"["+get_ID()
				+", Product="+getM_Product_ID()
				+", ComponentType="+getComponentType()
				+",QtyBatch="+getQtyBatch()
				+",QtyRequiered="+getQtyRequiered()
				+",QtyScrap="+getQtyScrap()
				+"]";
	}

	/** Set Keymat (String version - for data copying). */
	public void setKeymat(String Keymat) {
		set_Value(COLUMNNAME_Keymat, Keymat);
	}

	/** Get Keymat (String version - for storage). */
	public String getKeymat() {
		Object oo = get_Value(COLUMNNAME_Keymat);
		if (oo != null) {
			if (oo instanceof Boolean)
				return ((Boolean) oo) ? "Y" : "N";
			return oo.toString();
		}
		return "N";
	}
}
