package org.libero.tables;  
  
import java.math.BigDecimal;
import java.sql.ResultSet;  
import java.sql.Timestamp;  
import java.util.Properties;  
import org.compiere.model.*;
import org.compiere.util.Env;  
  
/** Generated Model for PP_Material_Requisition  
 *  @author iDempiere (generated)   
 *  @version Release 1.0c - $Id$ */  
public class X_PP_Material_Requisition extends PO implements I_PP_Material_Requisition, I_Persistent   
{  
	/**  
	 *  
	 */  
	private static final long serialVersionUID = 20260226L;  
	
	
	/** Get PP_Material_Requisition_ID.  
    @return 主键 */  
	public int getPP_Material_Requisition_ID() {  
    Integer ii = (Integer) get_Value(COLUMNNAME_PP_Material_Requisition_ID);  
    return ii == null ? 0 : ii.intValue();  
	}
  
    /** Standard Constructor */  
    public X_PP_Material_Requisition (Properties ctx, int PP_Material_Requisition_ID, String trxName)  
    {  
      super (ctx, PP_Material_Requisition_ID, trxName);  
    }  
  
    /** Load Constructor */  
    public X_PP_Material_Requisition (Properties ctx, ResultSet rs, String trxName)  
    {  
      super (ctx, rs, trxName);  
    }  
  
    /** AccessLevel  
      * @return 1 - Org   
      */  
    protected int get_AccessLevel()  
    {  
      return accessLevel.intValue();  
    }  
  
    /** Load Meta Data */  
    protected POInfo initPO (Properties ctx)  
    {  
      POInfo poi = POInfo.getPOInfo (ctx, Table_ID, get_TrxName());  
      return poi;  
    }  
  
    public String toString()  
    {  
      StringBuffer sb = new StringBuffer ("X_PP_Material_Requisition[")  
        .append(get_ID()).append("]");  
      return sb.toString();  
    }  
  
	/** Set Document No.  
		@param DocumentNo   
		Document sequence number of the document  
	  */  
	public void setDocumentNo (String DocumentNo)  
	{  
		set_Value (COLUMNNAME_DocumentNo, DocumentNo);  
	}  
  
	/** Get Document No.  
		@return Document sequence number of the document  
	  */  
	public String getDocumentNo ()   
	{  
		return (String)get_Value(COLUMNNAME_DocumentNo);  
	}  
  
	public I_AD_User getAD_User() throws RuntimeException  
    {  
		return (I_AD_User)MTable.get(getCtx(), I_AD_User.Table_Name)  
			.getPO(getAD_User_ID(), get_TrxName());	}  
  
	/** Set User/Contact.  
		@param AD_User_ID   
		User within the system - Internal or Business Partner Contact  
	  */  
	public void setAD_User_ID (int AD_User_ID)  
	{  
		if (AD_User_ID < 1)   
			set_Value (COLUMNNAME_AD_User_ID, null);  
		else   
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));  
	}  
  
	/** Get User/Contact.  
		@return User within the system - Internal or Business Partner Contact  
	  */  
	public int getAD_User_ID ()   
	{  
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);  
		if (ii == null)  
			 return 0;  
		return ii.intValue();  
	}  
  
	/** Set Description.  
		@param Description   
		Optional short description of the record  
	  */  
	public void setDescription (String Description)  
	{  
		set_Value (COLUMNNAME_Description, Description);  
	}  
  
	/** Get Description.  
		@return Optional short description of the record  
	  */  
	public String getDescription ()   
	{  
		return (String)get_Value(COLUMNNAME_Description);  
	}  
  
	/** Set Movement Date.  
		@param MovementDate   
		Date a product was moved in or out of inventory  
	  */  
	public void setMovementDate (Timestamp MovementDate)  
	{  
		set_Value (COLUMNNAME_MovementDate, MovementDate);  
	}  
  
	/** Get Movement Date.  
		@return Date a product was moved in or out of inventory  
	  */  
	public Timestamp getMovementDate ()   
	{  
		return (Timestamp)get_Value(COLUMNNAME_MovementDate);  
	}  
  
	public I_M_Product getM_Product() throws RuntimeException  
    {  
		return (I_M_Product)MTable.get(getCtx(), I_M_Product.Table_Name)  
			.getPO(getM_Product_ID(), get_TrxName());	}  
  
	/** Set Product.  
		@param M_Product_ID   
		Product, Service, Item  
	  */  
	public void setM_Product_ID (int M_Product_ID)  
	{  
		if (M_Product_ID < 1)   
			set_Value (COLUMNNAME_M_Product_ID, null);  
		else   
			set_Value (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));  
	}  
  
	/** Get Product.  
		@return Product, Service, Item  
	  */  
	public int getM_Product_ID ()   
	{  
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);  
		if (ii == null)  
			 return 0;  
		return ii.intValue();  
	}  
  
	public I_M_Warehouse getM_Warehouse() throws RuntimeException  
    {  
		return (I_M_Warehouse)MTable.get(getCtx(), I_M_Warehouse.Table_Name)  
			.getPO(getM_Warehouse_ID(), get_TrxName());	}  
  
	/** Set Warehouse.  
		@param M_Warehouse_ID   
		Storage Warehouse and Service Point  
	  */  
	public void setM_Warehouse_ID (int M_Warehouse_ID)  
	{  
		if (M_Warehouse_ID < 1)   
			set_Value (COLUMNNAME_M_Warehouse_ID, null);  
		else   
			set_Value (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));  
	}  
  
	/** Get Warehouse.  
		@return Storage Warehouse and Service Point  
	  */  
	public int getM_Warehouse_ID ()   
	{  
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);  
		if (ii == null)  
			 return 0;  
		return ii.intValue();  
	}  
  
	public org.eevolution.model.I_PP_Order getPP_Order() throws RuntimeException 
	{  
	    return  (org.eevolution.model.I_PP_Order) MTable.get(getCtx(), org.eevolution.model.I_PP_Order.Table_Name)  
	            .getPO(getPP_Order_ID(), get_TrxName());  
	
	}
	/** Set Manufacturing Order.  
		@param PP_Order_ID   
		Manufacturing Order  
	  */  
	public void setPP_Order_ID (int PP_Order_ID)  
	{  
		if (PP_Order_ID < 1)   
			set_ValueNoCheck (COLUMNNAME_PP_Order_ID, null);  
		else   
			set_ValueNoCheck (COLUMNNAME_PP_Order_ID, Integer.valueOf(PP_Order_ID));  
	}  
  
	/** Get Manufacturing Order.  
		@return Manufacturing Order  
	  */  
	public int getPP_Order_ID ()   
	{  
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_ID);  
		if (ii == null)  
			 return 0;  
		return ii.intValue();  
	}  
  
	public I_PP_Order_Node getPP_Order_Node() throws RuntimeException  
    {  
		return (I_PP_Order_Node)MTable.get(getCtx(), I_PP_Order_Node.Table_Name)  
			.getPO(getPP_Order_Node_ID(), get_TrxName());	}  
  
	/** Set Manufacturing Order Activity.  
		@param PP_Order_Node_ID   
		Workflow Node (activity), step or process  
	  */  
	public void setPP_Order_Node_ID (int PP_Order_Node_ID)  
	{  
		if (PP_Order_Node_ID < 1)   
			set_Value (COLUMNNAME_PP_Order_Node_ID, null);  
		else   
			set_Value (COLUMNNAME_PP_Order_Node_ID, Integer.valueOf(PP_Order_Node_ID));  
	}  
  
	/** Get Manufacturing Order Activity.  
		@return Workflow Node (activity), step or process  
	  */  
	public int getPP_Order_Node_ID ()   
	{  
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Order_Node_ID);  
		if (ii == null)  
			 return 0;  
		return ii.intValue();  
	}  
  
	public I_S_Resource getS_Resource() throws RuntimeException  
    {  
		return (I_S_Resource)MTable.get(getCtx(), I_S_Resource.Table_Name)  
			.getPO(getS_Resource_ID(), get_TrxName());	}  
  
	/** Set Resource.  
		@param S_Resource_ID   
		Resource  
	  */  
	public void setS_Resource_ID (int S_Resource_ID)  
	{  
		if (S_Resource_ID < 1)   
			set_Value (COLUMNNAME_S_Resource_ID, null);  
		else   
			set_Value (COLUMNNAME_S_Resource_ID, Integer.valueOf(S_Resource_ID));  
	}  
  
	/** Get Resource.  
		@return Resource  
	  */  
	public int getS_Resource_ID ()   
	{  
		Integer ii = (Integer)get_Value(COLUMNNAME_S_Resource_ID);  
		if (ii == null)  
			 return 0;  
		return ii.intValue();  
	}  
  
	/** Set Document Action.  
		@param DocAction   
		The targeted status of the document  
	  */  
	public void setDocAction (String DocAction)  
	{  
		set_Value (COLUMNNAME_DocAction, DocAction);  
	}  
  
	/** Get Document Action.  
		@return The targeted status of the document  
	  */  
	public String getDocAction ()   
	{  
		return (String)get_Value(COLUMNNAME_DocAction);  
	}  
  
	/** Set Document Status.  
		@param DocStatus   
		The current status of the document  
	  */  
	public void setDocStatus (String DocStatus)  
	{  
		set_Value (COLUMNNAME_DocStatus, DocStatus);  
	}  
  
	/** Get Document Status.  
		@return The current status of the document  
	  */  
	public String getDocStatus ()   
	{  
		return (String)get_Value(COLUMNNAME_DocStatus);  
	}  
  
	/** Set Processed.  
		@param Processed   
		The document has been processed  
	  */  
	public void setProcessed (boolean Processed)  
	{  
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));  
	}  
  
	/** Get Processed.  
		@return The document has been processed  
	  */  
	public boolean isProcessed ()   
	{  
		Object oo = get_Value(COLUMNNAME_Processed);  
		if (oo != null)   
		{  
			 if (oo instanceof Boolean)   
				 return ((Boolean)oo).booleanValue();   
			return "Y".equals(oo);  
		}  
		return false;  
	}  
  
	/** Set Processing.  
		@param Processing   
		@param Processing   
	  */  
	public void setProcessing (boolean Processing)  
	{  
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));  
	}  
  
	/** Get Processing.  
		@return Processing   
	  */  
	public boolean isProcessing ()   
	{  
		Object oo = get_Value(COLUMNNAME_Processing);  
		if (oo != null)   
		{  
			 if (oo instanceof Boolean)   
				 return ((Boolean)oo).booleanValue();   
			return "Y".equals(oo);  
		}  
		return false;  
	}  
	/** Set Processed On.  
    @param ProcessedOn */  
	/** Set Processed On.  
    @param ProcessedOn */  
	public void setProcessedOn (BigDecimal ProcessedOn)  
	{  
		set_Value (COLUMNNAME_ProcessedOn, ProcessedOn);  
	}  
  
	/** Get Processed On.  
    	@return */  
	public BigDecimal getProcessedOn()  
	{  
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ProcessedOn);  
		if (bd == null)  
			return Env.ZERO;  
		return bd;  
	}	
	
	/** Set Document Type.  
    @param C_DocType_ID */  
	public void setC_DocType_ID(int C_DocType_ID) {  
		if (C_DocType_ID < 0)  
			set_Value(COLUMNNAME_C_DocType_ID, null);  
		else  
			set_Value(COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));  
}  
  
	/** Get Document Type.  
    	@return */  
	public int getC_DocType_ID() {  
		Integer ii = (Integer) get_Value(COLUMNNAME_C_DocType_ID);  
		return ii == null ? 0 : ii.intValue();  
	}
	
	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException {  
	    return (org.compiere.model.I_C_DocType) MTable.get(getCtx(), org.compiere.model.I_C_DocType.Table_Name)  
	            .getPO(getC_DocType_ID(), get_TrxName());  
	}
	/** Set PP_Material_Requisition_ID.  
    @param PP_Material_Requisition_ID */  
	public void setPP_Material_Requisition_ID(int PP_Material_Requisition_ID) 
	{  
		if (PP_Material_Requisition_ID < 1)
			 set_ValueNoCheck(COLUMNNAME_PP_Material_Requisition_ID, null); 
       
 
		else 
		{
			set_ValueNoCheck(COLUMNNAME_PP_Material_Requisition_ID, Integer.valueOf(PP_Material_Requisition_ID));
		}
          
	}
	/** Set Approved.  
    @param IsApproved */  
	public void setIsApproved(boolean IsApproved) {  
		set_Value(COLUMNNAME_IsApproved, Boolean.valueOf(IsApproved));  
	}  
  
	/** Get Approved.  
    	@return */  
	public boolean isApproved() {  
		Object oo = get_Value(COLUMNNAME_IsApproved);  
		if (oo != null) {  
			if (oo instanceof Boolean) return ((Boolean) oo).booleanValue();  
			return "Y".equals(oo);  
		}  
		return false;  
	}
	
}