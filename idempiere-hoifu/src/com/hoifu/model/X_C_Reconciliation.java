/** Generated Model - DO NOT CHANGE */  

package com.hoifu.model; 
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;

  
/** Generated Model for C_Reconciliation  
 *  @author iDempiere (generated)  
 *  @version Release 13 - $Id$ */  
@org.adempiere.base.Model(table="c_reconciliation")  
public class X_C_Reconciliation extends PO implements I_C_Reconciliation, I_Persistent  
{  
    private static final long serialVersionUID = 20250102L;  
  
    /** Standard Constructor */  
    public X_C_Reconciliation (Properties ctx, int C_Reconciliation_ID, String trxName)  
    {  
      super (ctx, C_Reconciliation_ID, trxName);  
      if (C_Reconciliation_ID == 0)  
      {  
        setC_BPartner_ID (0);  
        setC_Currency_ID (0);  
        setDocumentNo (null);  
        setIsSOTrx (false);  
        setProcessed (false);  
        setReconciliationDate (new Timestamp( System.currentTimeMillis() ));  
        setReconciliationDay (new Timestamp( System.currentTimeMillis() ));  
      }  
    }  
    
  
    /** Standard Constructor */  
    public X_C_Reconciliation (Properties ctx, int C_Reconciliation_ID, String trxName, String ... virtualColumns)  
    {  
      super (ctx, C_Reconciliation_ID, trxName, virtualColumns);  
    }  
  
    /** Standard Constructor */  
    public X_C_Reconciliation (Properties ctx, String C_Reconciliation_UU, String trxName)  
    {  
      super (ctx, C_Reconciliation_UU, trxName);  
    }  
  
    /** Standard Constructor */  
    public X_C_Reconciliation (Properties ctx, String C_Reconciliation_UU, String trxName, String ... virtualColumns)  
    {  
      super (ctx, C_Reconciliation_UU, trxName, virtualColumns);  
    }  
  
    /** Load Constructor */  
    public X_C_Reconciliation (Properties ctx, ResultSet rs, String trxName)  
    {  
      super (ctx, rs, trxName);  
    }  
  
    /** AccessLevel  
      * @return 3 - Client - Org  
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
      StringBuilder sb = new StringBuilder ("X_C_Reconciliation[")  
        .append(get_ID()).append("]");  
      return sb.toString();  
    }  
  
    /** Set Business Partner .  
        @param C_BPartner_ID  
        Identifies a Business Partner  
      */  
    public void setC_BPartner_ID (int C_BPartner_ID)  
    {  
        if (C_BPartner_ID < 1)  
            set_Value ("C_BPartner_ID", null);  
        else  
            set_Value ("C_BPartner_ID", Integer.valueOf(C_BPartner_ID));  
    }  
  
    /** Get Business Partner .  
        @return Identifies a Business Partner  
      */  
    public int getC_BPartner_ID()  
    {  
        Integer ii = (Integer)get_Value("C_BPartner_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
    
    public int getC_BPartner_Location_ID()  
    {  
        Integer ii = (Integer)get_Value("C_BPartner_Location_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    } 
    
    public int getAD_User_ID()  
    {  
        Integer ii = (Integer)get_Value("AD_User_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    } 
  
    /** Set Currency.  
        @param C_Currency_ID  
        The Currency for this record  
      */  
    public void setC_Currency_ID (int C_Currency_ID)  
    {  
        if (C_Currency_ID < 1)  
            set_Value ("C_Currency_ID", null);  
        else  
            set_Value ("C_Currency_ID", Integer.valueOf(C_Currency_ID));  
    }  
  
    /** Get Currency.  
        @return The Currency for this record  
      */  
    public int getC_Currency_ID()  
    {  
        Integer ii = (Integer)get_Value("C_Currency_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set Document No.  
        @param DocumentNo  
        Document sequence number of the document  
      */  
    public void setDocumentNo (String DocumentNo)  
    {  
        set_Value ("DocumentNo", DocumentNo);  
    }  
  
    /** Get Document No.  
        @return Document sequence number of the document  
      */  
    public String getDocumentNo()  
    {  
        return (String)get_Value("DocumentNo");  
    }  
  
    /** Set Sales Transaction.  
        @param IsSOTrx  
        This is a Sales Transaction  
      */  
    public void setIsSOTrx (boolean IsSOTrx)  
    {  
        set_Value ("IsSOTrx", Boolean.valueOf(IsSOTrx));  
    }  
  
    /** Get Sales Transaction.  
        @return This is a Sales Transaction  
      */  
    public boolean isSOTrx()  
    {  
        Object oo = get_Value("IsSOTrx");  
        if (oo != null)  
        {  
            if (oo instanceof Boolean)  
                return ((Boolean)oo).booleanValue();  
            return "Y".equals(oo);  
        }  
        return false;  
    }  
  
    /** Set Processed.  
        @param Processed  
        The document has been processed  
      */  
    public void setProcessed (boolean Processed)  
    {  
        set_Value ("Processed", Boolean.valueOf(Processed));  
    }  
  
    /** Get Processed.  
        @return The document has been processed  
      */  
    public boolean isProcessed()  
    {  
        Object oo = get_Value("Processed");  
        if (oo != null)  
        {  
            if (oo instanceof Boolean)  
                return ((Boolean)oo).booleanValue();  
            return "Y".equals(oo);  
        }  
        return false;  
    }  
  
    /** Set Reconciliation Date.  
        @param ReconciliationDate Reconciliation Date      */  
    public void setReconciliationDate (Timestamp ReconciliationDate)  
    {  
        set_Value ("ReconciliationDate", ReconciliationDate);  
    }  
  
    /** Get Reconciliation Date.  
        @return Reconciliation Date      */  
    public Timestamp getReconciliationDate()  
    {  
        return (Timestamp)get_Value("ReconciliationDate");  
    }  
    
    /** Get Reconciliation Date.  
    	@return Reconciliation Cutoff      */ 
    public Timestamp getReconciliationcutoff() {
    	return (Timestamp)get_Value("Reconciliationcutoff");
    }
  
    /** Set Reconciliation Day.  
        @param ReconciliationDay Reconciliation Day      */  
    public void setReconciliationDay (Timestamp ReconciliationDay)  
    {  
        set_Value ("ReconciliationDay", ReconciliationDay);  
    }  
  
    /** Get Reconciliation Day.  
        @return Reconciliation Day      */  
    public Timestamp getReconciliationDay()  
    {  
        return (Timestamp)get_Value("ReconciliationDay");  
    }  
  
    /** Set ReconPeroid.  
        @param ReconPeroid ReconPeroid      */  
    public void setReconPeroid (String ReconPeroid)  
    {  
        set_Value ("ReconPeroid", ReconPeroid);  
    }  
  
    /** Get ReconPeroid.  
        @return ReconPeroid      */  
    public String getReconPeroid()  
    {  
        return (String)get_Value("ReconPeroid");  
    }


	@Override
	public void setC_Reconciliation_ID(int C_Reconciliation_ID) {
		set_Value ("C_Reconciliation_ID", C_Reconciliation_ID);  
	}


	@Override
	public int getC_Reconciliation_ID() {
		return (int)get_Value("C_Reconciliation_ID");
	}  
	
    
}