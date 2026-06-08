
package com.hoifu.model; 
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import org.compiere.util.KeyNamePair;  
  
public interface I_C_ReconciliationLine   
{  
    /** TableName=C_ReconciliationLine */  
    public static final String Table_Name = "C_ReconciliationLine";  
  
    /** AD_Table_ID=1000047 */  
    public static final int Table_ID = 1000047;  
  
    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);  
  
    /** AccessLevel = 3 - Client - Org */  
    BigDecimal accessLevel = BigDecimal.valueOf(3);  
  
    /** Column name C_ReconciliationLine_ID */  
    public static final String COLUMNNAME_C_ReconciliationLine_ID = "C_ReconciliationLine_ID";  
  
    /** Set Reconciliation Line.  
        @param C_ReconciliationLine_ID Reconciliation Line      */  
    public void setC_ReconciliationLine_ID (int C_ReconciliationLine_ID);  
  
    /** Get Reconciliation Line.  
        @return Reconciliation Line      */  
    public int getC_ReconciliationLine_ID();  
  
    /** Column name C_Reconciliation_ID */  
    public static final String COLUMNNAME_C_Reconciliation_ID = "C_Reconciliation_ID";  
  
    /** Set Reconciliation.  
        @param C_Reconciliation_ID Reconciliation      */  
    public void setC_Reconciliation_ID (int C_Reconciliation_ID);  
  
    /** Get Reconciliation.  
        @return Reconciliation      */  
    public int getC_Reconciliation_ID();  
  
    /** Column name AD_Table_ID */  
    public static final String COLUMNNAME_AD_Table_ID = "AD_Table_ID";  
  
    /** Set Table.  
        @param AD_Table_ID Database Table information      */  
    public void setAD_Table_ID (int AD_Table_ID);  
  
    /** Get Table.  
        @return Database Table information      */  
    public int getAD_Table_ID();  
  
    /** Column name Record_ID */  
    public static final String COLUMNNAME_Record_ID = "Record_ID";  
  
    /** Set Record ID.  
        @param Record_ID Direct internal record ID      */  
    public void setRecord_ID (int Record_ID);  
  
    /** Get Record ID.  
        @return Direct internal record ID      */  
    public int getRecord_ID();  
  
    /** Column name LineType */  
    public static final String COLUMNNAME_LineType = "LineType";  
  
    /** Set Line Type.  
        @param LineType Line Type      */  
    public void setLineType (String LineType);  
  
    /** Get Line Type.  
        @return Line Type      */  
    public String getLineType();  
  
    /** Column name DocumentNo */  
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";  
  
    /** Set Document No.  
        @param DocumentNo Document sequence number of the document      */  
    public void setDocumentNo (String DocumentNo);  
  
    /** Get Document No.  
        @return Document sequence number of the document      */  
    public String getDocumentNo();  
  
    /** Column name DateDoc */  
    public static final String COLUMNNAME_DateDoc = "DateDoc";  
  
    /** Set Document Date.  
        @param DateDoc Date of the Document      */  
    public void setDateDoc (Timestamp DateDoc);  
  
    /** Get Document Date.  
        @return Date of the Document      */  
    public Timestamp getDateDoc();  
  
    /** Column name MovementQty */  
    public static final String COLUMNNAME_MovementQty = "MovementQty";  
  
    /** Set Movement Quantity.  
        @param MovementQty Quantity of a product moved.      */  
    public void setMovementQty (BigDecimal MovementQty);  
  
    /** Get Movement Quantity.  
        @return Quantity of a product moved.      */  
    public BigDecimal getMovementQty();  
  
    /** Column name LineNetAmt */  
    public static final String COLUMNNAME_LineNetAmt = "LineNetAmt";  
  
    /** Set Line Net Amount.  
        @param LineNetAmt Line net amount (quantity * actual price) without discount and charges      */  
    public void setLineNetAmt (BigDecimal LineNetAmt);  
  
    /** Get Line Net Amount.  
        @return Line net amount (quantity * actual price) without discount and charges      */  
    public BigDecimal getLineNetAmt();  
  
    /** Column name LineTotalAmt */  
    public static final String COLUMNNAME_LineTotalAmt = "LineTotalAmt";  
  
    /** Set Line Total Amount.  
        @param LineTotalAmt Total line amount incl. taxes      */  
    public void setLineTotalAmt (BigDecimal LineTotalAmt);  
  
    /** Get Line Total Amount.  
        @return Total line amount incl. taxes      */  
    public BigDecimal getLineTotalAmt();  
  
    /** Column name ReconPeroid */  
    public static final String COLUMNNAME_ReconPeroid = "ReconPeroid";  
  
    /** Set ReconPeroid.  
        @param ReconPeroid ReconPeroid      */  
    public void setReconPeroid (String ReconPeroid);  
  
    /** Get ReconPeroid.  
        @return ReconPeroid      */  
    public String getReconPeroid();  
}