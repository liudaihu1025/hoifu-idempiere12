/******************************************************************************
 * Product: iDempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2012 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package com.hoifu.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for QC_IQCInspect
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="QC_IQCInspect")
public class X_QC_IQCInspect extends PO implements I_QC_IQCInspect, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260902L;

    /** Standard Constructor */
    public X_QC_IQCInspect (Properties ctx, int QC_IQCInspect_ID, String trxName)
    {
      super (ctx, QC_IQCInspect_ID, trxName);
      /** if (QC_IQCInspect_ID == 0)
        {
			setC_DocType_ID (0);
			setDocumentNo (null);
			setM_InOutLine_ID (0);
			setM_InOut_ID (0);
			setM_Product_ID (0);
			setQC_IQCInspect_ID (0);
			sethandlemethod (null);
			setinspectresult (false);
// N
			setinspectstatus (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_QC_IQCInspect (Properties ctx, int QC_IQCInspect_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, QC_IQCInspect_ID, trxName, virtualColumns);
      /** if (QC_IQCInspect_ID == 0)
        {
			setC_DocType_ID (0);
			setDocumentNo (null);
			setM_InOutLine_ID (0);
			setM_InOut_ID (0);
			setM_Product_ID (0);
			setQC_IQCInspect_ID (0);
			sethandlemethod (null);
			setinspectresult (false);
// N
			setinspectstatus (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_QC_IQCInspect (Properties ctx, String QC_IQCInspect_UU, String trxName)
    {
      super (ctx, QC_IQCInspect_UU, trxName);
      /** if (QC_IQCInspect_UU == null)
        {
			setC_DocType_ID (0);
			setDocumentNo (null);
			setM_InOutLine_ID (0);
			setM_InOut_ID (0);
			setM_Product_ID (0);
			setQC_IQCInspect_ID (0);
			sethandlemethod (null);
			setinspectresult (false);
// N
			setinspectstatus (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_QC_IQCInspect (Properties ctx, String QC_IQCInspect_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, QC_IQCInspect_UU, trxName, virtualColumns);
      /** if (QC_IQCInspect_UU == null)
        {
			setC_DocType_ID (0);
			setDocumentNo (null);
			setM_InOutLine_ID (0);
			setM_InOut_ID (0);
			setM_Product_ID (0);
			setQC_IQCInspect_ID (0);
			sethandlemethod (null);
			setinspectresult (false);
// N
			setinspectstatus (false);
// N
        } */
    }

    /** Load Constructor */
    public X_QC_IQCInspect (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }

    /** AccessLevel
      * @return 4 - System
      */
    protected int get_AccessLevel()
    {
      return accessLevel.intValue();
    }

    /** Load Meta Data */
    protected POInfo initPO (Properties ctx)
    {
      POInfo poi = POInfo.getPOInfo (ctx, MTable.getTable_ID(Table_Name), get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_QC_IQCInspect[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_C_DocType getC_DocType() throws RuntimeException
	{
		return (org.compiere.model.I_C_DocType)MTable.get(getCtx(), org.compiere.model.I_C_DocType.Table_ID)
			.getPO(getC_DocType_ID(), get_TrxName());
	}

	/** Set Document Type.
		@param C_DocType_ID Document type or rules
	*/
	public void setC_DocType_ID (int C_DocType_ID)
	{
		if (C_DocType_ID < 0)
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_DocType_ID, Integer.valueOf(C_DocType_ID));
	}

	/** Get Document Type.
		@return Document type or rules
	  */
	public int getC_DocType_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_DocType_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Description.
		@param Description Optional short description of the record
	*/
	public void setDescription (String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Description.
		@return Optional short description of the record
	  */
	public String getDescription()
	{
		return (String)get_Value(COLUMNNAME_Description);
	}

	/** Set Document No.
		@param DocumentNo Document sequence number of the document
	*/
	public void setDocumentNo (String DocumentNo)
	{
		set_ValueNoCheck (COLUMNNAME_DocumentNo, DocumentNo);
	}

	/** Get Document No.
		@return Document sequence number of the document
	  */
	public String getDocumentNo()
	{
		return (String)get_Value(COLUMNNAME_DocumentNo);
	}

	public org.compiere.model.I_M_InOutLine getM_InOutLine() throws RuntimeException
	{
		return (org.compiere.model.I_M_InOutLine)MTable.get(getCtx(), org.compiere.model.I_M_InOutLine.Table_ID)
			.getPO(getM_InOutLine_ID(), get_TrxName());
	}

	/** Set Shipment/Receipt Line.
		@param M_InOutLine_ID Line on Shipment or Receipt document
	*/
	public void setM_InOutLine_ID (int M_InOutLine_ID)
	{
		if (M_InOutLine_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOutLine_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOutLine_ID, Integer.valueOf(M_InOutLine_ID));
	}

	/** Get Shipment/Receipt Line.
		@return Line on Shipment or Receipt document
	  */
	public int getM_InOutLine_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOutLine_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_InOut getM_InOut() throws RuntimeException
	{
		return (org.compiere.model.I_M_InOut)MTable.get(getCtx(), org.compiere.model.I_M_InOut.Table_ID)
			.getPO(getM_InOut_ID(), get_TrxName());
	}

	/** Set Shipment/Receipt.
		@param M_InOut_ID Material Shipment Document
	*/
	public void setM_InOut_ID (int M_InOut_ID)
	{
		if (M_InOut_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_InOut_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_InOut_ID, Integer.valueOf(M_InOut_ID));
	}

	/** Get Shipment/Receipt.
		@return Material Shipment Document
	  */
	public int getM_InOut_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_InOut_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
	{
		return (org.compiere.model.I_M_Product)MTable.get(getCtx(), org.compiere.model.I_M_Product.Table_ID)
			.getPO(getM_Product_ID(), get_TrxName());
	}

	/** Set Product.
		@param M_Product_ID Product, Service, Item
	*/
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1)
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Product.
		@return Product, Service, Item
	  */
	public int getM_Product_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set &#25910;&#36135;&#29289;&#26009;&#26816;&#39564;&#34920;.
		@param QC_IQCInspect_ID &#25910;&#36135;&#29289;&#26009;&#26816;&#39564;&#34920;
	*/
	public void setQC_IQCInspect_ID (int QC_IQCInspect_ID)
	{
		if (QC_IQCInspect_ID < 1)
			set_ValueNoCheck (COLUMNNAME_QC_IQCInspect_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_QC_IQCInspect_ID, Integer.valueOf(QC_IQCInspect_ID));
	}

	/** Get &#25910;&#36135;&#29289;&#26009;&#26816;&#39564;&#34920;.
		@return &#25910;&#36135;&#29289;&#26009;&#26816;&#39564;&#34920;	  */
	public int getQC_IQCInspect_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_QC_IQCInspect_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set QC_IQCInspect_UU.
		@param QC_IQCInspect_UU QC_IQCInspect_UU
	*/
	public void setQC_IQCInspect_UU (String QC_IQCInspect_UU)
	{
		set_ValueNoCheck (COLUMNNAME_QC_IQCInspect_UU, QC_IQCInspect_UU);
	}

	/** Get QC_IQCInspect_UU.
		@return QC_IQCInspect_UU	  */
	public String getQC_IQCInspect_UU()
	{
		return (String)get_Value(COLUMNNAME_QC_IQCInspect_UU);
	}

	/** &#25918;&#34892; = G */
	public static final String HANDLEMETHOD_放行 = "G";
	/** &#36864;&#36135; = R */
	public static final String HANDLEMETHOD_退货 = "R";
	/** Set &#22788;&#29702;&#26041;&#24335;.
		@param handlemethod &#22788;&#29702;&#26041;&#24335;
	*/
	public void sethandlemethod (String handlemethod)
	{

		set_Value (COLUMNNAME_handlemethod, handlemethod);
	}

	/** Get &#22788;&#29702;&#26041;&#24335;.
		@return &#22788;&#29702;&#26041;&#24335;	  */
	public String gethandlemethod()
	{
		return (String)get_Value(COLUMNNAME_handlemethod);
	}

	/** Set inspectdate.
		@param inspectdate inspectdate
	*/
	public void setinspectdate (Timestamp inspectdate)
	{
		set_Value (COLUMNNAME_inspectdate, inspectdate);
	}

	/** Get inspectdate.
		@return inspectdate	  */
	public Timestamp getinspectdate()
	{
		return (Timestamp)get_Value(COLUMNNAME_inspectdate);
	}

	/** Set inspector.
		@param inspector inspector
	*/
	public void setinspector (String inspector)
	{
		set_Value (COLUMNNAME_inspector, inspector);
	}

	/** Get inspector.
		@return inspector	  */
	public String getinspector()
	{
		return (String)get_Value(COLUMNNAME_inspector);
	}

	/** Set &#26816;&#39564;&#32467;&#26524;.
		@param inspectresult &#26816;&#39564;&#32467;&#26524;
	*/
	public void setinspectresult (boolean inspectresult)
	{
		set_Value (COLUMNNAME_inspectresult, Boolean.valueOf(inspectresult));
	}

	/** Get &#26816;&#39564;&#32467;&#26524;.
		@return &#26816;&#39564;&#32467;&#26524;	  */
	public boolean isinspectresult()
	{
		Object oo = get_Value(COLUMNNAME_inspectresult);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set &#26816;&#39564;&#29366;&#24577;.
		@param inspectstatus &#26816;&#39564;&#29366;&#24577;
	*/
	public void setinspectstatus (boolean inspectstatus)
	{
		set_Value (COLUMNNAME_inspectstatus, Boolean.valueOf(inspectstatus));
	}

	/** Get &#26816;&#39564;&#29366;&#24577;.
		@return &#26816;&#39564;&#29366;&#24577;	  */
	public boolean isinspectstatus()
	{
		Object oo = get_Value(COLUMNNAME_inspectstatus);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set &#19981;&#21512;&#26684;&#25968;.
		@param qtyfailed &#19981;&#21512;&#26684;&#25968;
	*/
	public void setqtyfailed (BigDecimal qtyfailed)
	{
		set_Value (COLUMNNAME_qtyfailed, qtyfailed);
	}

	/** Get &#19981;&#21512;&#26684;&#25968;.
		@return &#19981;&#21512;&#26684;&#25968;	  */
	public BigDecimal getqtyfailed()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtyfailed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set &#21512;&#26684;&#25968;.
		@param qtypassed &#21512;&#26684;&#25968;
	*/
	public void setqtypassed (BigDecimal qtypassed)
	{
		set_Value (COLUMNNAME_qtypassed, qtypassed);
	}

	/** Get &#21512;&#26684;&#25968;.
		@return &#21512;&#26684;&#25968;	  */
	public BigDecimal getqtypassed()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtypassed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}