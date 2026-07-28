package com.hoifu.process;  
  
import java.sql.PreparedStatement;  
import java.sql.ResultSet;  
import java.util.ArrayList;  
import java.util.List;  
import java.util.logging.Level;  
  
import org.compiere.model.MProcessPara;  
import org.compiere.model.MProduct;  
import org.compiere.model.MProductCategory;  
import org.compiere.model.Query;  
import org.compiere.process.ProcessInfoParameter;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import org.compiere.util.Msg;  
import org.eevolution.model.MPPProductBOM;  
import org.eevolution.model.MPPProductBOMLine;  
  
/**  
 * 	Validate BOM  
 *  
 *  @author Jorg Janke  
 *  @version $Id: BOMVerify.java,v 1.1 2007/07/23 05:34:35 mfuggle Exp $  
 */  
@org.adempiere.base.annotation.Process  
public class BOMVerifyProcess extends SvrProcess  
{  
	/** The Product			*/  
	private int		p_M_Product_ID = 0;  
	/** Product Category	*/  
	private int		p_M_Product_Category_ID = 0;  
	/** Re-Validate			*/  
	private boolean	p_IsReValidate = false;  
  
	private boolean	p_fromButton = false;  
  
	/** List of Products	*/  
	private ArrayList<MProduct>	 foundproducts = new ArrayList<MProduct>();  
	private ArrayList<MProduct> validproducts = new ArrayList<MProduct>();  
	private ArrayList<MProduct>	 invalidproducts = new ArrayList<MProduct>();  
	private ArrayList<MProduct> containinvalidproducts = new ArrayList<MProduct>();  
	private ArrayList<MProduct> checkedproducts = new ArrayList<MProduct>();  
  
	/**  
	 * 	Prepare  
	 */  
	protected void prepare ()  
	{  
		ProcessInfoParameter[] para = getParameter();  
		for (int i = 0; i < para.length; i++)  
		{  
			String name = para[i].getParameterName();  
			if (para[i].getParameter() == null)  
				;  
			else if (name.equals("M_Product_ID"))  
				p_M_Product_ID = para[i].getParameterAsInt();  
			else if (name.equals("M_Product_Category_ID"))  
				p_M_Product_Category_ID = para[i].getParameterAsInt();  
			else if (name.equals("IsReValidate"))  
				p_IsReValidate = "Y".equals(para[i].getParameter());  
			else  
				MProcessPara.validateUnknownParameter(getProcessInfo().getAD_Process_ID(), para[i]);  
		}  
		if ( p_M_Product_ID == 0 )  
			p_M_Product_ID = getRecord_ID();  
		p_fromButton = (getRecord_ID() > 0);  
	}	//	prepare  
  
	/**  
	 * 	Process  
	 *	@return Info  
	 *	@throws Exception  
	 */  
	protected String doIt() throws Exception  
	{  
		if (p_M_Product_ID != 0)  
		{  
			if (log.isLoggable(Level.INFO)) log.info("M_Product_ID=" + p_M_Product_ID);  
			checkProduct(new MProduct(getCtx(), p_M_Product_ID, get_TrxName()));  
			return "物料已检查";  
		}  
		if (log.isLoggable(Level.INFO)) log.info("M_Product_Category_ID=" + p_M_Product_Category_ID  
			+ ", IsReValidate=" + p_IsReValidate);  
		//  
		int counter = 0;  
		PreparedStatement pstmt = null;  
		ResultSet rs = null;  
		String sql = "SELECT M_Product_ID FROM M_Product "  
			+ "WHERE IsBOM='Y' AND ";  
		if (p_M_Product_Category_ID == 0)  
			sql += "AD_Client_ID=? ";  
		else  
			sql += "M_Product_Category_ID=? ";  
		if (!p_IsReValidate)  
			sql += "AND IsVerified<>'Y' ";  
		sql += "ORDER BY Name";  
		int AD_Client_ID = Env.getAD_Client_ID(getCtx());  
		try  
		{  
			pstmt = DB.prepareStatement (sql, get_TrxName());  
			if (p_M_Product_Category_ID == 0)  
				pstmt.setInt (1, AD_Client_ID);  
			else  
				pstmt.setInt(1, p_M_Product_Category_ID);  
			rs = pstmt.executeQuery ();  
			while (rs.next ())  
			{  
				p_M_Product_ID = rs.getInt(1);  
				checkProduct(new MProduct(getCtx(), p_M_Product_ID, get_TrxName()));  
				counter++;  
			}  
		}  
		catch (Exception e)  
		{  
			throw e;  
		}  
		finally  
		{  
			DB.close(rs, pstmt);  
			rs = null; pstmt = null;  
		}  
		return "#" + counter;  
	}	//	doIt  
  
	private void checkProduct(MProduct product)  
	{  
		if (product.isBOM() && !checkedproducts.contains(product))  
		{  
			validateProduct(product);  
		}  
	}  
  
	/**  
	 * 	Validate Product  
	 *	@param product product  
	 *	@return Info  
	 */  
	private boolean validateProduct (MProduct product)  
	{  
		if (!product.isBOM())  
			return false;  
  
		if (validproducts.contains(product))  
			return true;  
  
		if (log.isLoggable(Level.CONFIG)) log.config(product.getName());  
  
		// 判断物料大类是否为半成品（BC），半成品不校验主物料  
		// M_Product_Category_ID_L1 是产品级别属性，在 BOM 循环外计算一次即可  
		boolean skipKeyMatCheck = false;  
		Object l1IdObj = product.get_Value("M_Product_Category_ID_L1");  
		if (l1IdObj instanceof Integer) {  
			int l1Id = (Integer) l1IdObj;  
			if (l1Id > 0) {  
				MProductCategory l1Cat = MProductCategory.get(getCtx(), l1Id);  
				if (l1Cat != null && "BC".equals(l1Cat.getValue())) {  
					skipKeyMatCheck = true;  
				}  
			}  
		}  
  
		boolean containsinvalid = false;  
		boolean invalid = false;  
		foundproducts.add(product);  
		List<MPPProductBOM> boms = MPPProductBOM.getProductBOMs(product);  
		for(MPPProductBOM bom : boms)  
		{  
			// 每个BOM单独统计主物料数量  
			int keyMaterialCount = 0;  
  
			MPPProductBOMLine[] bomLines = bom.getLines();  
			int lines = 0;  
			for (MPPProductBOMLine bomLine : bomLines)  
			{  
				if (!bomLine.isActive())  
					continue;  
				lines++;  
				// 统计主物料数量  
				try {  
					if ((boolean) bomLine.get_Value("KeyMat")) {  
						keyMaterialCount++;  
					}  
				} catch (Exception e) {  
					// 报错就跳过  
				}  
				MProduct pp = new MProduct(getCtx(), bomLine.getM_Product_ID(), get_TrxName());  
				if (!pp.isBOM()) {  
					if (log.isLoggable(Level.FINER)) log.finer(pp.getName());  
				} else {  
					if (validproducts.contains(pp))  
					{  
						continue;  
					}  
					if (invalidproducts.contains(pp))  
					{  
						containsinvalid = true;  
					}  
					else if (foundproducts.contains(pp))  
					{  
						invalid = true;  
						if (p_fromButton)  
							addLog(0, null, null, Msg.getMsg(getCtx(), "BOMRecursivelyContains", new Object[] {product.getValue(), pp.getValue()}));  
						else  
							addBufferLog(0, null, null, Msg.getMsg(getCtx(), "BOMRecursivelyContains", new Object[] {product.getValue(), pp.getValue()}), MProduct.Table_ID, product.getM_Product_ID());  
					}  
					else  
					{  
						if (!validateProduct(pp))  
						{  
							containsinvalid = true;  
						}  
					}  
				}  
			}  
			if (lines == 0) {  
				invalid = true;  
				if (p_fromButton)  
					addLog(0, null, null, Msg.getMsg(getCtx(), "BOMForProductDoesNotHaveLines", new Object[] {bom.getValue(), product.getValue()}));  
				else  
					addBufferLog(0, null, null, Msg.getMsg(getCtx(), "BOMForProductDoesNotHaveLines", new Object[] {bom.getValue(), product.getValue()}), MProduct.Table_ID, product.getM_Product_ID());  
			} else if (!skipKeyMatCheck) {  
				// 只有非半成品才校验主物料  
				if (keyMaterialCount == 0) {  
					if (p_fromButton)  
						addLog(0, null, null, product.getValue() + " BOM明细没有定义主物料");  
					else  
						addBufferLog(0, null, null, product.getValue() + " BOM明细没有定义主物料", MProduct.Table_ID, product.getM_Product_ID());  
				} else if (keyMaterialCount > 1) {  
					if (p_fromButton)  
						addLog(0, null, null, product.getValue() + " BOM明细定义了多个主物料（共" + keyMaterialCount + "个），有且仅能有1个");  
					else  
						addBufferLog(0, null, null, product.getValue() + " BOM明细定义了多个主物料（共" + keyMaterialCount + "个），有且仅能有1个", MProduct.Table_ID, product.getM_Product_ID());  
				}  
			}  
			if (invalid || containsinvalid)  
				break;  
		}  
  
		if (boms.isEmpty()) {  
			invalid = true;  
			if (p_fromButton)  
				addLog(0, null, null, Msg.getMsg(getCtx(), "BOMMissingForProduct", new Object[] {product.getValue()}));  
			else  
				addBufferLog(0, null, null, Msg.getMsg(getCtx(), "BOMMissingForProduct", new Object[] {product.getValue()}), MProduct.Table_ID, product.getM_Product_ID());  
		} else if (MPPProductBOM.getDefault(product, get_TrxName()) == null  
				&& getDefaultBOM(product) == null) {  
			// 先用标准方法找，找不到再用产品组织兜底，两个都找不到才报错  
			invalid = true;  
			if (p_fromButton)  
				addLog(0, null, null, Msg.getMsg(getCtx(), "BOMNoDefaultBOMForProduct", new Object[] {product.getValue()}));  
			else  
				addBufferLog(0, null, null, Msg.getMsg(getCtx(), "BOMNoDefaultBOMForProduct", new Object[] {product.getValue()}), MProduct.Table_ID, product.getM_Product_ID());  
		}  
  
		checkedproducts.add(product);  
		foundproducts.remove(product);  
		if (invalid)  
		{  
			invalidproducts.add(product);  
			product.setIsVerified(false);  
			product.saveEx();  
			return false;  
		}  
		else if (containsinvalid)  
		{  
			containinvalidproducts.add(product);  
			product.setIsVerified(false);  
			product.saveEx();  
			return false;  
		}  
		else  
		{  
			validproducts.add(product);  
			product.setIsVerified(true);  
			product.saveEx();  
			return true;  
		}  
	}	//	validateProduct  
  
	/**  
	 * 按产品自身组织查找默认BOM，作为 MPPProductBOM.getDefault 的保底方案。  
	 * MPPProductBOM.getDefault 使用登录组织过滤，可能导致跨组织场景下找不到BOM。  
	 */  
	private MPPProductBOM getDefaultBOM(MProduct product) {  
		int AD_Org_ID = product.getAD_Org_ID(); // 用产品组织，不用登录组织  
		String filter = "M_Product_ID=? AND BOMUse=? AND BOMType=? ";  
		if (AD_Org_ID > 0) {  
			filter += "AND AD_Org_ID IN (0, " + AD_Org_ID + ") ";  
		}  
		Query query = new Query(product.getCtx(), MPPProductBOM.Table_Name, filter, get_TrxName())  
				.setParameters(product.getM_Product_ID(),  
						MPPProductBOM.BOMUSE_Master,  
						MPPProductBOM.BOMTYPE_CurrentActive)  
				.setOnlyActiveRecords(true)  
				.setClient_ID();  
		if (AD_Org_ID > 0)  
			query.setOrderBy("AD_Org_ID Desc");  
  
		List<MPPProductBOM> list = query.list();  
		if (!list.isEmpty()) {  
			if (AD_Org_ID > 0 || list.size() == 1) {  
				return list.get(0);  
			}  
		}  
		return null;  
	}  
  
}	//	BOMVerifyProcess