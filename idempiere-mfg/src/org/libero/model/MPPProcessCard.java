package org.libero.model;  
  
import java.math.BigDecimal;  
import java.math.RoundingMode;  
import java.sql.ResultSet;  
import java.sql.Timestamp;  
import java.util.Properties;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.util.DB;  
import org.libero.tables.X_PP_Process_Card;  
  
/**  
 * PP Process Card Model.  
 *  
 * CardNo 编码规则：工单号_总数_序号  
 *   总数(N) = ceil(工单数量 / 卡板数量)   -- 卡板数量为打印流程参数  
 *   序号   = 01 起递增，按同一 PP_Order_ID 下已存在的流程卡数量 + 1  
 *  
 * Qty 编码规则：  
 *   非最后一张：Qty = 卡板数量  
 *   最后一张(序号==N)：Qty = 工单数量 - 卡板数量*(N-1)   -- 吸收余数  
 */  
public class MPPProcessCard extends X_PP_Process_Card  
{  
	private static final long serialVersionUID = 1L;  
  
	/** 本次打印批次的卡板数量参数（用于计算总数/序号对应的 Qty），仅在便捷构造中赋值 */  
	private BigDecimal p_batchQty = null;  
  
	/** Standard Constructor */  
	public MPPProcessCard(Properties ctx, int PP_Process_Card_ID, String trxName)  
	{  
		super(ctx, PP_Process_Card_ID, trxName);  
	}  
  
	/** Load Constructor */  
	public MPPProcessCard(Properties ctx, ResultSet rs, String trxName)  
	{  
		super(ctx, rs, trxName);  
	}  
  
	/**  
	 * 便捷构造：根据工单和卡板数量参数创建一张流程卡。  
	 * Qty/CardNo 均由 beforeSave() 根据 batchQty 自动计算（最后一张自动吸收余数）。  
	 *  
	 * @param order    生产工单  
	 * @param batchQty 卡板数量（流程参数，每张卡的标准容量）  
	 * @param userId   打印人 AD_User_ID  
	 * @param trxName  事务  
	 */  
	public MPPProcessCard(MPPOrder order, BigDecimal batchQty, int userId, String trxName)  
	{  
		this(order.getCtx(), 0, trxName);  
		this.p_batchQty = batchQty;  
		setAD_Org_ID(order.getAD_Org_ID());  
		setPP_Order_ID(order.getPP_Order_ID());  
		setAD_User_ID(userId);  
		setPrintDate(new Timestamp(System.currentTimeMillis()));  
	}  
  
	@Override  
	protected boolean beforeSave(boolean newRecord)  
	{  
		// CardNo/Qty 只在新建时生成一次，更新时不再重新计算/覆盖  
		if (newRecord && (getCardNo() == null || getCardNo().trim().length() == 0))  
		{  
			try  
			{  
				applyCardNoAndQty();  
			}  
			catch (AdempiereException e)  
			{  
				log.saveError("Error", e.getLocalizedMessage());  
				return false;  
			}  
		}  
  
		return true;  
	}  
  
	/**  
	 * 集中封装 CardNo + Qty 的计算与赋值逻辑：  
	 * 校验 → 取工单 → 算总数(N) → 算序号(seq) → 算本卡Qty(非末张=batchQty，末张=余数) → 拼编号。  
	 * 只应在新建记录时调用一次。  
	 */  
	private void applyCardNoAndQty()  
	{  
		if (getPP_Order_ID() <= 0)  
			throw new AdempiereException("PP_Order_ID 不能为空");  
  
		BigDecimal batchQty = p_batchQty != null ? p_batchQty : getQty();  
		if (batchQty == null || batchQty.signum() <= 0)  
			throw new AdempiereException("数量(Qty)必须大于0");  
  
		MPPOrder order = new MPPOrder(getCtx(), getPP_Order_ID(), get_TrxName());  
		if (order.get_ID() <= 0)  
			throw new AdempiereException("找不到生产工单: " + getPP_Order_ID());  
  
		BigDecimal qtyOrdered = order.getQtyOrdered();  
		if (qtyOrdered == null || qtyOrdered.signum() <= 0)  
			throw new AdempiereException("工单数量无效: " + order.getDocumentNo());  
  
		// 总数 N = ceil(工单数量 / 卡板数量)  
		int total = qtyOrdered.divide(batchQty, 0, RoundingMode.CEILING).intValue();  
		if (total <= 0)  
			throw new AdempiereException("计算总数无效");  
  
		// 序号 = 同一工单下已有流程卡数量 + 1（排除自身，兼容极端场景下的重复调用）  
		int existing = DB.getSQLValue(get_TrxName(),  
				"SELECT COUNT(*) FROM PP_Process_Card WHERE PP_Order_ID=? AND PP_Process_Card_ID<>?",  
				getPP_Order_ID(), get_ID());  
		int seq = existing + 1;  
  
		if (seq > total)  
			throw new AdempiereException("工单【" + order.getDocumentNo() + "】流程卡数量已达上限(" + total + ")，无法再生成");  
  
		// 本卡数量：最后一张 = 工单数量 - 卡板数量*(N-1)；否则 = 卡板数量  
		BigDecimal cardQty;  
		if (seq == total)  
			cardQty = qtyOrdered.subtract(batchQty.multiply(BigDecimal.valueOf(total - 1L)));  
		else  
			cardQty = batchQty;  
  
		setQty(cardQty);  
		setCardNo(order.getDocumentNo() + "_" + total + "_" + String.format("%02d", seq));  
	}  
}