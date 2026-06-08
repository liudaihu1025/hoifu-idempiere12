/** Generated Interface for C_Reconciliation  
 *  @author iDempiere (generated)   
 *  @version Release 13 */  
package com.hoifu.model; 
  
import java.math.BigDecimal;  
import java.sql.Timestamp;  
import org.compiere.util.KeyNamePair;  
  
public interface I_C_Reconciliation   
{  
    /** TableName=C_Reconciliation */  
    public static final String Table_Name = "C_Reconciliation";  
  
    /** AD_Table_ID=1000046 */  
    public static final int Table_ID = 1000046;  
  
    KeyNamePair Model = new KeyNamePair(Table_ID, Table_Name);  
  
    /** AccessLevel = 3 - Client - Org */  
    BigDecimal accessLevel = BigDecimal.valueOf(3);  
  
    /** Column name C_Reconciliation_ID */  
    public static final String COLUMNNAME_C_Reconciliation_ID = "C_Reconciliation_ID";  
  
    /** Set Reconciliation.  
        @param C_Reconciliation_ID Reconciliation      */  
    public void setC_Reconciliation_ID (int C_Reconciliation_ID);  
  
    /** Get Reconciliation.  
        @return Reconciliation      */  
    public int getC_Reconciliation_ID();  
  
    /** Column name C_BPartner_ID */  
    public static final String COLUMNNAME_C_BPartner_ID = "C_BPartner_ID";  
  
    /** Set Business Partner.  
        @param C_BPartner_ID Identifies a Business Partner      */  
    public void setC_BPartner_ID (int C_BPartner_ID);  
  
    /** Get Business Partner.  
        @return Identifies a Business Partner      */  
    public int getC_BPartner_ID();  
  
    /** Column name DocumentNo */  
    public static final String COLUMNNAME_DocumentNo = "DocumentNo";  
  
    /** Set Document No.  
        @param DocumentNo Document sequence number of the document      */  
    public void setDocumentNo (String DocumentNo);  
  
    /** Get Document No.  
        @return Document sequence number of the document      */  
    public String getDocumentNo();  
  
    /** Column name ReconPeroid */  
    public static final String COLUMNNAME_ReconPeroid = "ReconPeroid";  
  
    /** Set ReconPeroid.  
        @param ReconPeroid ReconPeroid      */  
    public void setReconPeroid (String ReconPeroid);  
  
    /** Get ReconPeroid.  
        @return ReconPeroid      */  
    public String getReconPeroid();  
  
    /** Column name C_Currency_ID */  
    public static final String COLUMNNAME_C_Currency_ID = "C_Currency_ID";  
  
    /** Set Currency.  
        @param C_Currency_ID The Currency for this record      */  
    public void setC_Currency_ID (int C_Currency_ID);  
  
    /** Get Currency.  
        @return The Currency for this record      */  
    public int getC_Currency_ID();  
  
    /** Column name IsSOTrx */  
    public static final String COLUMNNAME_IsSOTrx = "IsSOTrx";  
  
    /** Set Sales Transaction.  
        @param IsSOTrx This is a Sales Transaction      */  
    public void setIsSOTrx (boolean IsSOTrx);  
  
    /** Get Sales Transaction.  
        @return This is a Sales Transaction      */  
    public boolean isSOTrx();  
  
    /** Column name Processed */  
    public static final String COLUMNNAME_Processed = "Processed";  
  
    /** Set Processed.  
        @param Processed The document has been processed      */  
    public void setProcessed (boolean Processed);  
  
    /** Get Processed.  
        @return The document has been processed      */  
    public boolean isProcessed();  
  
    /** Column name ReconciliationDate */  
    public static final String COLUMNNAME_ReconciliationDate = "ReconciliationDate";  
  
    /** Set Reconciliation Date.  
        @param ReconciliationDate Reconciliation Date      */  
    public void setReconciliationDate (Timestamp ReconciliationDate);  
  
    /** Get Reconciliation Date.  
        @return Reconciliation Date      */  
    public Timestamp getReconciliationDate();  
  
    /** Column name ReconciliationDay */  
    public static final String COLUMNNAME_ReconciliationDay = "ReconciliationDay";  
  
    /** Set Reconciliation Day.  
        @param ReconciliationDay Reconciliation Day      */  
    public void setReconciliationDay (Timestamp ReconciliationDay);  
  
    /** Get Reconciliation Day.  
        @return Reconciliation Day      */  
    public Timestamp getReconciliationDay();  
}