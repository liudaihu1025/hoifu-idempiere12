package org.libero.tables;  
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import org.compiere.model.*;  
import org.compiere.util.KeyNamePair;  
  
/** Generated Interface for PP_Material_Requisition  
 *  @author iDempiere (generated)   
 *  @version Release 1.0c  
 */  
@SuppressWarnings("all")  
public interface I_PP_Material_Requisition   
{  
  
    /** TableName=PP_Material_Requisition */  
    public static final String Table_Name = "pp_material_requisition";  
  
    /** AD_Table_ID=XXXXX */ // 请根据实际 AD 中定义的 Table_ID 填写  
    public static final int Table_ID = MTable.getTable_ID(Table_Name);  
  
    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);  
  
    /** AccessLevel = 1 - Org   
     */  
    BigDecimal accessLevel = BigDecimal.valueOf(1);  
  
    /** Load Meta Data */  
  
    /** Column name AD_Client_ID */  
    public static final String COLUMNNAME_AD_Client_ID = "AD_Client_ID";  
  
	/** Get Client.  
	  * Client/Tenant for this installation.  
	  */  
	public int getAD_Client_ID();  
  
    /** Column name AD_Org_ID */  
    public static final String COLUMNNAME_AD_Org_ID = "AD_Org_ID";  
  
	/** Set Organization.  
	  * Organizational entity within client  
	  */  
	public void setAD_Org_ID (int AD_Org_ID);  
  
	/** Get Organization.  
	  * Organizational entity within client  
	  */  
	public int getAD_Org_ID();  
  
    /** Column name C_DocType_ID */  
    public static final String COLUMNNAME_C_DocType_ID = "C_DocType_ID";  
  
	/** Set Document Type.  
	  * Document type or rules  
	  */  
	public void setC_DocType_ID (int C_DocType_ID);  
  
	/** Get Document Type.  
	  * Document type or rules  
	  */  
	public int getC_DocType_ID();  
  
	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException;  
  
    /** Column name Created */  
    public static final String COLUMNNAME_Created = "Created";  
  
	/** Get Created.  
	  * Date this record was created  
	  */  
	public Timestamp getCreated();  
  
    /** Column name CreatedBy */  
    public static final String COLUMNNAME_CreatedBy = "CreatedBy";  
  
	/** Get Created By.  
	  * User who created this records  
	  */  
	public int getCreatedBy();  
  
    /** Column name DocAction */  
    public static final String COLUMNNAME_DocAction = "DocAction";  
  
	/** Set Document Action.  
	  * The targeted status of the document  
	  */  
	public void setDocAction (String DocAction);  
  
	/** Get Document Action.  
	  * The targeted status of the document  
	  */  
	public String getDocAction();  
  
    /** Column name DocStatus */  
    public static final String COLUMNNAME_DocStatus = "DocStatus";  
  
	/** Set Document Status.  
	  * The current status of the document  
	  */  
	public void setDocStatus (String DocStatus);  
  
	/** Get Document Status.  
	  * The current status of the document  
	  */  
	public String getDocStatus();  
  
    /** Column name DocumentNo */  
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";  
  
	/** Set Document No.  
	  * Document sequence number of the document  
	  */  
	public void setDocumentNo (String DocumentNo);  
  
	/** Get Document No.  
	  * Document sequence number of the document  
	  */  
	public String getDocumentNo();  
  
    /** Column name IsActive */  
    public static final String COLUMNNAME_IsActive = "IsActive";  
  
	/** Set Active.  
	  * The record is active in the system  
	  */  
	public void setIsActive (boolean IsActive);  
  
	/** Get Active.  
	  * The record is active in the system  
	  */  
	public boolean isActive();  
  
    /** Column name MovementDate */  
    public static final String COLUMNNAME_MovementDate = "MovementDate";  
  
	/** Set Movement Date.  
	  * Date a product was moved in or out of inventory  
	  */  
	public void setMovementDate (Timestamp MovementDate);  
  
	/** Get Movement Date.  
	  * Date a product was moved in or out of inventory  
	  */  
	public Timestamp getMovementDate();  
  
    /** Column name M_Product_ID */  
    public static final String COLUMNNAME_M_Product_ID = "M_Product_ID";  
  
	/** Set Product.  
	  * Product, Service, Item  
	  */  
	public void setM_Product_ID (int M_Product_ID);  
  
	/** Get Product.  
	  * Product, Service, Item  
	  */  
	public int getM_Product_ID();  
  
	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException;  
  
    /** Column name M_Warehouse_ID */  
    public static final String COLUMNNAME_M_Warehouse_ID = "M_Warehouse_ID";  
  
	/** Set Warehouse.  
	  * Storage Warehouse and Service Point  
	  */  
	public void setM_Warehouse_ID (int M_Warehouse_ID);  
  
	/** Get Warehouse.  
	  * Storage Warehouse and Service Point  
	  */  
	public int getM_Warehouse_ID();  
  
	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException;  
  
    /** Column name PP_Material_Requisition_ID */  
    public static final String COLUMNNAME_PP_Material_Requisition_ID = "PP_Material_Requisition_ID";  
  
	/** Set PP_Material_Requisition_ID	  */  
	public void setPP_Material_Requisition_ID (int PP_Material_Requisition_ID);  
  
	/** Get PP_Material_Requisition_ID	  */  
	public int getPP_Material_Requisition_ID();  
  
    /** Column name PP_Order_ID */  
    public static final String COLUMNNAME_PP_Order_ID = "PP_Order_ID";  
  
	/** Set Manufacturing Order.  
	  * Manufacturing Order  
	  */  
	public void setPP_Order_ID (int PP_Order_ID);  
  
	/** Get Manufacturing Order.  
	  * Manufacturing Order  
	  */  
	public int getPP_Order_ID();  
  
	public org.eevolution.model.I_PP_Order getPP_Order() throws RuntimeException;  
  
    /** Column name PP_Order_Node_ID */  
    public static final String COLUMNNAME_PP_Order_Node_ID = "PP_Order_Node_ID";  
  
	/** Set Manufacturing Order Activity.  
	  * Workflow Node (activity), step or process  
	  */  
	public void setPP_Order_Node_ID (int PP_Order_Node_ID);  
  
	/** Get Manufacturing Order Activity.  
	  * Workflow Node (activity), step or process  
	  */  
	public int getPP_Order_Node_ID();  
  
	public org.libero.tables.I_PP_Order_Node getPP_Order_Node() throws RuntimeException;  
  
    /** Column name Processed */  
    public static final String COLUMNNAME_Processed = "Processed";  
  
	/** Set Processed.  
	  * The document has been processed  
	  */  
	public void setProcessed (boolean Processed);  
  
	/** Get Processed.  
	  * The document has been processed  
	  */  
	public boolean isProcessed();  
  
    /** Column name Processing */  
    public static final String COLUMNNAME_Processing = "Processing";  
  
	/** Set Process Now	  */  
	public void setProcessing (boolean Processing);  
  
	/** Get Process Now	  */  
	public boolean isProcessing();  
  
    /** Column name S_Resource_ID */  
    public static final String COLUMNNAME_S_Resource_ID = "S_Resource_ID";  
  
	/** Set Resource.  
	  * Resource  
	  */  
	public void setS_Resource_ID (int S_Resource_ID);  
  
	/** Get Resource.  
	  * Resource  
	  */  
	public int getS_Resource_ID();  
  
	public org.compiere.model.I_S_Resource getS_Resource() throws RuntimeException;  
  
    /** Column name Updated */  
    public static final String COLUMNNAME_Updated = "Updated";  
  
	/** Get Updated.  
	  * Date this record was updated  
	  */  
	public Timestamp getUpdated();  
  
    /** Column name UpdatedBy */  
    public static final String COLUMNNAME_UpdatedBy = "UpdatedBy";  
  
	/** Get Updated By.  
	  * User who updated this records  
	  */  
	public int getUpdatedBy();  
  
    /** Column name AD_User_ID */  
    public static final String COLUMNNAME_AD_User_ID = "AD_User_ID";  
  
	/** Set User/Contact.  
	  * User within the system - Internal or Business Partner Contact  
	  */  
	public void setAD_User_ID (int AD_User_ID);  
  
	/** Get User/Contact.  
	  * User within the system - Internal or Business Partner Contact  
	  */  
	public int getAD_User_ID();  
  
	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException;  
  
    /** Column name Description */  
    public static final String COLUMNNAME_Description = "Description";  
  
	/** Set Description.  
	  * Optional short description of the record  
	  */  
	public void setDescription (String Description);  
  
	/** Get Description.  
	  * Optional short description of the record  
	  */  
	public String getDescription();  
  
    /** Column name ProcessedOn */  
    public static final String COLUMNNAME_ProcessedOn = "ProcessedOn";  
  
	/** Set Processed On.  
	  * The date+time (expressed in seconds since the epoch) a record was processed.  
	  */  
	public void setProcessedOn (BigDecimal ProcessedOn);  
  
	/** Get Processed On.  
	  * The date+time (expressed in seconds since the epoch) a record was processed.  
	  */  
	public BigDecimal getProcessedOn();  
	
	/** Column name IsApproved */  
	public static final String COLUMNNAME_IsApproved = "IsApproved";  
	  
	/** Set Approved.  
	    @param IsApproved */  
	public void setIsApproved(boolean IsApproved);  
	  
	/** Get Approved.  
	    @return */  
	public boolean isApproved();
	
	
}