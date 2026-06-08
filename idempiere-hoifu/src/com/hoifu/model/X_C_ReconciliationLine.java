package com.hoifu.model; 
/** Generated Model - DO NOT CHANGE */  
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;

import org.compiere.model.I_Persistent;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;  
  
/** Generated Model for C_ReconciliationLine  
 *  @author iDempiere (generated)  
 *  @version Release 13 - $Id$ */  
@org.adempiere.base.Model(table="c_reconciliationLine")  
public class X_C_ReconciliationLine extends PO implements I_C_ReconciliationLine, I_Persistent  
{  
    private static final long serialVersionUID = 20250102L;  
  
    /** Standard Constructor */  
    public X_C_ReconciliationLine (Properties ctx, int C_ReconciliationLine_ID, String trxName)  
    {  
      super (ctx, C_ReconciliationLine_ID, trxName);  
      if (C_ReconciliationLine_ID == 0)  
      {  
        setAD_Table_ID (0);  
        setC_Reconciliation_ID (0);  
        setLineType (null);  
        setLineNetAmt (Env.ZERO);  
        setLineTotalAmt (Env.ZERO);  
        setProcessed (false);  
        setQtyToReconcile (Env.ZERO);  
        setQtyReconciled (Env.ZERO);  
        setRecord_ID (0);  
      }  
    }  
  
 

	/** Standard Constructor */  
    public X_C_ReconciliationLine (Properties ctx, int C_ReconciliationLine_ID, String trxName, String ... virtualColumns)  
    {  
      super (ctx, C_ReconciliationLine_ID, trxName, virtualColumns);  
    }  
  
    /** Standard Constructor */  
    public X_C_ReconciliationLine (Properties ctx, String C_ReconciliationLine_UU, String trxName)  
    {  
      super (ctx, C_ReconciliationLine_UU, trxName);  
    }  
  
    /** Standard Constructor */  
    public X_C_ReconciliationLine (Properties ctx, String C_ReconciliationLine_UU, String trxName, String ... virtualColumns)  
    {  
      super (ctx, C_ReconciliationLine_UU, trxName, virtualColumns);  
    }  
  
    /** Load Constructor */  
    public X_C_ReconciliationLine (Properties ctx, ResultSet rs, String trxName)  
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
      StringBuilder sb = new StringBuilder ("X_C_ReconciliationLine[")  
        .append(get_ID()).append("]");  
      return sb.toString();  
    }  
  
    /** Set Table.  
        @param AD_Table_ID Database Table information      */  
    public void setAD_Table_ID (int AD_Table_ID)  
    {  
        if (AD_Table_ID < 1)  
            set_Value ("AD_Table_ID", null);  
        else  
            set_Value ("AD_Table_ID", Integer.valueOf(AD_Table_ID));  
    }  
  
    /** Get Table.  
        @return Database Table information      */  
    public int getAD_Table_ID()  
    {  
        Integer ii = (Integer)get_Value("AD_Table_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set Reconciliation.  
        @param C_Reconciliation_ID Reconciliation      */  
    public void setC_Reconciliation_ID (int C_Reconciliation_ID)  
    {  
        if (C_Reconciliation_ID < 1)  
            set_Value ("C_Reconciliation_ID", null);  
        else  
            set_Value ("C_Reconciliation_ID", Integer.valueOf(C_Reconciliation_ID));  
    }  
  
    /** Get Reconciliation.  
        @return Reconciliation      */  
    public int getC_Reconciliation_ID()  
    {  
        Integer ii = (Integer)get_Value("C_Reconciliation_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set Line Type.  
        @param LineType Line Type      */  
    public void setLineType (String LineType)  
    {  
        set_Value ("LineType", LineType);  
    }  
  
    /** Get Line Type.  
        @return Line Type      */  
    public String getLineType()  
    {  
        return (String)get_Value("LineType");  
    }  
  
    /** Set Document No.  
        @param DocumentNo Document sequence number of the document      */  
    public void setDocumentNo (String DocumentNo)  
    {  
        set_Value ("DocumentNo", DocumentNo);  
    }  
  
    /** Get Document No.  
        @return Document sequence number of the document      */  
    public String getDocumentNo()  
    {  
        return (String)get_Value("DocumentNo");  
    }  
  
    /** Set Document Date.  
        @param DateDoc Date of the Document      */  
    public void setDateDoc (Timestamp DateDoc)  
    {  
        set_Value ("DateDoc", DateDoc);  
    }  
  
    /** Get Document Date.  
        @return Date of the Document      */  
    public Timestamp getDateDoc()  
    {  
        return (Timestamp)get_Value("DateDoc");  
    }  
  
    /** Set Due Date.  
        @param DateDue Date when the payment is due      */  
    public void setDateDue (Timestamp DateDue)  
    {  
        set_Value ("DateDue", DateDue);  
    }  
  
    /** Get Due Date.  
        @return Date when the payment is due      */  
    public Timestamp getDateDue()  
    {  
        return (Timestamp)get_Value("DateDue");  
    }  
  
    /** Set Line.  
        @param Line Line No      */  
    public void setLine (int Line)  
    {  
        set_Value ("Line", Integer.valueOf(Line));  
    }  
  
    /** Get Line.  
        @return Line No      */  
    public int getLine()  
    {  
        Integer ii = (Integer)get_Value("Line");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set Description.  
        @param Description Description      */  
    public void setDescription (String Description)  
    {  
        set_Value ("Description", Description);  
    }  
  
    /** Get Description.  
        @return Description      */  
    public String getDescription()  
    {  
        return (String)get_Value("Description");  
    }  
  
    /** Set Note.  
        @param Note Note      */  
    public void setNote (String Note)  
    {  
        set_Value ("Note", Note);  
    }  
  
    /** Get Note.  
        @return Note      */  
    public String getNote()  
    {  
        return (String)get_Value("Note");  
    }  
  
    /** Set Product.  
        @param M_Product_ID Product, Service, Item      */  
    public void setM_Product_ID (int M_Product_ID)  
    {  
        if (M_Product_ID < 1)  
            set_Value ("M_Product_ID", null);  
        else  
            set_Value ("M_Product_ID", Integer.valueOf(M_Product_ID));  
    }  
  
    /** Get Product.  
        @return Product, Service, Item      */  
    public int getM_Product_ID()  
    {  
        Integer ii = (Integer)get_Value("M_Product_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set UOM.  
        @param C_UOM_ID Unit of Measure      */  
    public void setC_UOM_ID (int C_UOM_ID)  
    {  
        if (C_UOM_ID < 1)  
            set_Value ("C_UOM_ID", null);  
        else  
            set_Value ("C_UOM_ID", Integer.valueOf(C_UOM_ID));  
    }  
  
    /** Get UOM.  
        @return Unit of Measure      */  
    public int getC_UOM_ID()  
    {  
        Integer ii = (Integer)get_Value("C_UOM_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set Attribute Set Instance.  
        @param M_AttributeSetInstance_ID Product Attribute Set Instance      */  
    public void setM_AttributeSetInstance_ID (int M_AttributeSetInstance_ID)  
    {  
        if (M_AttributeSetInstance_ID < 0)  
            set_Value ("M_AttributeSetInstance_ID", null);  
        else  
            set_Value ("M_AttributeSetInstance_ID", Integer.valueOf(M_AttributeSetInstance_ID));  
    }  
  
    /** Get Attribute Set Instance.  
        @return Product Attribute Set Instance      */  
    public int getM_AttributeSetInstance_ID()  
    {  
        Integer ii = (Integer)get_Value("M_AttributeSetInstance_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set Ordered Quantity.  
        @param QtyOrdered Ordered Quantity      */  
    public void setQtyOrdered (BigDecimal QtyOrdered)  
    {  
        set_Value ("QtyOrdered", QtyOrdered);  
    }  
  
    /** Get Ordered Quantity.  
        @return Ordered Quantity      */  
    public BigDecimal getQtyOrdered()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("QtyOrdered");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Delivered Quantity.  
        @param QtyDelivered Delivered Quantity      */  
    public void setQtyDelivered (BigDecimal QtyDelivered)  
    {  
        set_Value ("QtyDelivered", QtyDelivered);  
    }  
  
    /** Get Delivered Quantity.  
        @return Delivered Quantity      */  
    public BigDecimal getQtyDelivered()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("QtyDelivered");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Invoiced Quantity.  
        @param QtyInvoiced Invoiced Quantity      */  
    public void setQtyInvoiced (BigDecimal QtyInvoiced)  
    {  
        set_Value ("QtyInvoiced", QtyInvoiced);  
    }  
  
    /** Get Invoiced Quantity.  
        @return Invoiced Quantity      */  
    public BigDecimal getQtyInvoiced()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("QtyInvoiced");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Qty To Reconcile.  
        @param QtyToReconcile Qty To Reconcile      */  
    public void setQtyToReconcile (BigDecimal QtyToReconcile)  
    {  
        set_Value ("QtyToReconcile", QtyToReconcile);  
    }  
  
    /** Get Qty To Reconcile.  
        @return Qty To Reconcile      */  
    public BigDecimal getQtyToReconcile()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("QtyToReconcile");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Qty Reconciled.  
        @param QtyReconciled Qty Reconciled      */  
    public void setQtyReconciled (BigDecimal QtyReconciled)  
    {  
        set_Value ("QtyReconciled", QtyReconciled);  
    }  

	/**
	 * Get PriceToReconcile
	 * 
	 * @return PriceToReconcile
	 */
	public BigDecimal getPriceToReconcile() {
		BigDecimal bd = (BigDecimal) get_Value("PriceToReconcile");
		if (bd == null)
			return Env.ZERO;
		return bd;
	}

	/**
	 * Set PriceToReconcile
	 * 
	 * @param PriceToReconcile
	 */
	public void setPriceToReconcile(BigDecimal PriceToReconcile) {
		set_Value("PriceToReconcile", PriceToReconcile);
	}
  
    /** Get Qty Reconciled.  
        @return Qty Reconciled      */  
    public BigDecimal getQtyReconciled()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("QtyReconciled");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Price Entered.  
        @param PriceEntered Price Entered      */  
    public void setPriceEntered (BigDecimal PriceEntered)  
    {  
        set_Value ("PriceEntered", PriceEntered);  
    }  
  
    /** Get Price Entered.  
        @return Price Entered      */  
    public BigDecimal getPriceEntered()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("PriceEntered");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Unit Price.  
        @param PriceActual Actual Price      */  
    public void setPriceActual (BigDecimal PriceActual)  
    {  
        set_Value ("PriceActual", PriceActual);  
    }  
  
    /** Get Unit Price.  
        @return Actual Price      */  
    public BigDecimal getPriceActual()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("PriceActual");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set List Price.  
        @param PriceList List Price      */  
    public void setPriceList (BigDecimal PriceList)  
    {  
        set_Value ("PriceList", PriceList);  
    }  
  
    /** Get List Price.  
        @return List Price      */  
    public BigDecimal getPriceList()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("PriceList");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Limit Price.  
        @param PriceLimit Limit Price      */  
    public void setPriceLimit (BigDecimal PriceLimit)  
    {  
        set_Value ("PriceLimit", PriceLimit);  
    }  
  
    /** Get Limit Price.  
        @return Limit Price      */  
    public BigDecimal getPriceLimit()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("PriceLimit");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Discount %.  
        @param Discount Discount in percent      */  
    public void setDiscount (BigDecimal Discount)  
    {  
        set_Value ("Discount", Discount);  
    }  
  
    /** Get Discount %.  
        @return Discount in percent      */  
    public BigDecimal getDiscount()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("Discount");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Currency.  
        @param C_Currency_ID The Currency for this record      */  
    public void setC_Currency_ID (int C_Currency_ID)  
    {  
        if (C_Currency_ID < 1)  
            set_Value ("C_Currency_ID", null);  
        else  
            set_Value ("C_Currency_ID", Integer.valueOf(C_Currency_ID));  
    }  
  
    /** Get Currency.  
        @return The Currency for this record      */  
    public int getC_Currency_ID()  
    {  
        Integer ii = (Integer)get_Value("C_Currency_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set Tax.  
        @param C_Tax_ID Tax identifier      */  
    public void setC_Tax_ID (int C_Tax_ID)  
    {  
        if (C_Tax_ID < 1)  
            set_Value ("C_Tax_ID", null);  
        else  
            set_Value ("C_Tax_ID", Integer.valueOf(C_Tax_ID));  
    }  
  
    /** Get Tax.  
        @return Tax identifier      */  
    public int getC_Tax_ID()  
    {  
        Integer ii = (Integer)get_Value("C_Tax_ID");  
        if (ii == null)  
            return 0;  
        return ii.intValue();  
    }  
  
    /** Set Tax Rate.  
        @param TaxRate Tax Rate      */  
    public void setTaxRate (BigDecimal TaxRate)  
    {  
        set_Value ("TaxRate", TaxRate);  
    }  
  
    /** Get Tax Rate.  
        @return Tax Rate      */  
    public BigDecimal getTaxRate()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("TaxRate");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Price includes Tax.  
        @param IsTaxIncluded Tax is included in the price      */  
    public void setIsTaxIncluded (boolean IsTaxIncluded)  
    {  
        set_Value ("IsTaxIncluded", Boolean.valueOf(IsTaxIncluded));  
    }  
  
    /** Get Price includes Tax.  
        @return Tax is included in the price      */  
    public boolean isTaxIncluded()  
    {  
        Object oo = get_Value("IsTaxIncluded");  
        if (oo != null)  
        {  
            if (oo instanceof Boolean)  
                return ((Boolean)oo).booleanValue();  
            return "Y".equals(oo);  
        }  
        return false;  
    }  
  
    /** Set Line Net Amount.  
        @param LineNetAmt Line net amount (quantity * actual price) without discount and charges      */  
    public void setLineNetAmt (BigDecimal LineNetAmt)  
    {  
        set_Value ("LineNetAmt", LineNetAmt);  
    }  
  
    /** Get Line Net Amount.  
        @return Line net amount (quantity * actual price) without discount and charges      */  
    public BigDecimal getLineNetAmt()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("LineNetAmt");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Line Total Amount.  
        @param LineTotalAmt Total line amount incl. taxes      */  
    public void setLineTotalAmt (BigDecimal LineTotalAmt)  
    {  
        set_Value ("LineTotalAmt", LineTotalAmt);  
    }  
  
    /** Get Line Total Amount.  
        @return Total line amount incl. taxes      */  
    public BigDecimal getLineTotalAmt()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("LineTotalAmt");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }  
  
    /** Set Reconciled Amount.  
        @param ReconciledAmt Reconciled Amount      */  
    public void setReconciledAmt (BigDecimal ReconciledAmt)  
    {  
        set_Value ("ReconciledAmt", ReconciledAmt);  
    }  
  
    /** Get Reconciled Amount.  
        @return Reconciled Amount      */  
    public BigDecimal getReconciledAmt()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("ReconciledAmt");  
        if (bd == null)  
            return Env.ZERO;  
        return   bd;
    }
    public void setDifferenceAmt (BigDecimal DifferenceAmt)  
    {  
        set_Value ("DifferenceAmt", DifferenceAmt);  
    }  
      
    /** Get Difference Amount.  
        @return Difference Amount      */  
    public BigDecimal getDifferenceAmt()  
    {  
        BigDecimal bd = (BigDecimal)get_Value("DifferenceAmt");  
        if (bd == null)  
            return Env.ZERO;  
        return bd;  
    }
  



/** Set Shipment/Receipt Line.  
@param M_InOutLine_ID Line on Shipment or Receipt document      */  
public void setM_InOutLine_ID (int M_InOutLine_ID)  
{  
if (M_InOutLine_ID < 1)  
    set_Value ("M_InOutLine_ID", null);  
else  
    set_Value ("M_InOutLine_ID", Integer.valueOf(M_InOutLine_ID));  
}  

/** Get Shipment/Receipt Line.  
@return Line on Shipment or Receipt document      */  
public int getM_InOutLine_ID()  
{  
Integer ii = (Integer)get_Value("M_InOutLine_ID");  
if (ii == null)  
    return 0;  
return ii.intValue();  
}  

/** Set Reconciliation Status.  
@param ReconStatus Reconciliation Status      */  
public void setReconStatus (String ReconStatus)  
{  
set_Value ("ReconStatus", ReconStatus);  
}  

/** Get Reconciliation Status.  
@return Reconciliation Status      */  
public String getReconStatus()  
{  
return (String)get_Value("ReconStatus");  
}  

/** Set Is Reconciled.  
@param IsReconciled Is this line completely reconciled      */  
public void setIsReconciled (boolean IsReconciled)  
{  
set_Value ("IsReconciled", Boolean.valueOf(IsReconciled));  
}  

/** Get Is Reconciled.  
@return Is this line completely reconciled      */  
public boolean isReconciled()  
{  
Object oo = get_Value("IsReconciled");  
if (oo != null)  
{  
    if (oo instanceof Boolean)  
        return ((Boolean)oo).booleanValue();  
    return "Y".equals(oo);  
}  
return false;  
}
/** Set Processed.  
@param Processed The document has been processed      */  
public void setProcessed (boolean Processed)  
{  
set_Value ("Processed", Boolean.valueOf(Processed));  
}  

/** Get Processed.  
@return The document has been processed      */  
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

/** Set Processing.  
@param Processing The document is being processed      */  
public void setProcessing (boolean Processing)  
{  
set_Value ("Processing", Boolean.valueOf(Processing));  
}  

/** Get Processing.  
@return The document is being processed      */  
public boolean isProcessing()  
{  
Object oo = get_Value("Processing");  
if (oo != null)  
{  
    if (oo instanceof Boolean)  
        return ((Boolean)oo).booleanValue();  
    return "Y".equals(oo);  
}  
return false;  
}  

/** Set Approved.  
@param IsApproved Indicates if this document is approved      */  
public void setIsApproved (boolean IsApproved)  
{  
set_Value ("IsApproved", Boolean.valueOf(IsApproved));  
}  

/** Get Approved.  
@return Indicates if this document is approved      */  
public boolean isApproved()  
{  
Object oo = get_Value("IsApproved");  
if (oo != null)  
{  
    if (oo instanceof Boolean)  
        return ((Boolean)oo).booleanValue();  
    return "Y".equals(oo);  
}  
return false;  
}  

/** Set Sales Order Line.  
@param C_OrderLine_ID Sales Order Line      */  
public void setC_OrderLine_ID (int C_OrderLine_ID)  
{  
if (C_OrderLine_ID < 1)  
    set_Value ("C_OrderLine_ID", null);  
else  
    set_Value ("C_OrderLine_ID", Integer.valueOf(C_OrderLine_ID));  
}  

/** Get Sales Order Line.  
@return Sales Order Line      */  
public int getC_OrderLine_ID()  
{  
Integer ii = (Integer)get_Value("C_OrderLine_ID");  
if (ii == null)  
    return 0;  
return ii.intValue();  
}

@Override
public void setC_ReconciliationLine_ID(int C_ReconciliationLine_ID) {
	set_Value ("C_ReconciliationLine_ID", C_ReconciliationLine_ID);  
}

@Override
public int getC_ReconciliationLine_ID() {
	return (int)get_Value("C_ReconciliationLine_ID");
}

@Override
public void setRecord_ID(int Record_ID) {
	// TODO Auto-generated method stub
	
}

@Override
public int getRecord_ID() {
	// TODO Auto-generated method stub
	return 0;
}

@Override
public void setMovementQty(BigDecimal MovementQty) {
	// TODO Auto-generated method stub
	
}

@Override
public BigDecimal getMovementQty() {
	// TODO Auto-generated method stub
	return null;
}

@Override
public void setReconPeroid(String ReconPeroid) {
	// TODO Auto-generated method stub
	
}

@Override
public String getReconPeroid() {
	// TODO Auto-generated method stub
	return null;
}

}