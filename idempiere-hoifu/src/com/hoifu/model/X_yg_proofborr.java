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

import org.compiere.model.I_Persistent;
import org.compiere.model.MTable;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;

/** Generated Model for yg_proofborr
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="yg_proofborr")
public class X_yg_proofborr extends PO implements I_yg_proofborr, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260807L;

    /** Standard Constructor */
    public X_yg_proofborr (Properties ctx, int yg_proofborr_ID, String trxName)
    {
      super (ctx, yg_proofborr_ID, trxName);
      /** if (yg_proofborr_ID == 0)
        {
			setBorrower_ID (0);
			setDocumentNo (null);
			setborrowdate (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setborrowstatus (null);
			setborrowtype (null);
			setisreturned (true);
// Y
			setyg_proofborr_ID (0);
			setyg_proofinventory_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_yg_proofborr (Properties ctx, int yg_proofborr_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, yg_proofborr_ID, trxName, virtualColumns);
      /** if (yg_proofborr_ID == 0)
        {
			setBorrower_ID (0);
			setDocumentNo (null);
			setborrowdate (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setborrowstatus (null);
			setborrowtype (null);
			setisreturned (true);
// Y
			setyg_proofborr_ID (0);
			setyg_proofinventory_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_yg_proofborr (Properties ctx, String yg_proofborr_UU, String trxName)
    {
      super (ctx, yg_proofborr_UU, trxName);
      /** if (yg_proofborr_UU == null)
        {
			setBorrower_ID (0);
			setDocumentNo (null);
			setborrowdate (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setborrowstatus (null);
			setborrowtype (null);
			setisreturned (true);
// Y
			setyg_proofborr_ID (0);
			setyg_proofinventory_ID (0);
        } */
    }

    /** Standard Constructor */
    public X_yg_proofborr (Properties ctx, String yg_proofborr_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, yg_proofborr_UU, trxName, virtualColumns);
      /** if (yg_proofborr_UU == null)
        {
			setBorrower_ID (0);
			setDocumentNo (null);
			setborrowdate (new Timestamp( System.currentTimeMillis() ));
// @SysDate@
			setborrowstatus (null);
			setborrowtype (null);
			setisreturned (true);
// Y
			setyg_proofborr_ID (0);
			setyg_proofinventory_ID (0);
        } */
    }

    /** Load Constructor */
    public X_yg_proofborr (Properties ctx, ResultSet rs, String trxName)
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
      POInfo poi = POInfo.getPOInfo (ctx, MTable.getTable_ID(Table_Name), get_TrxName());
      return poi;
    }

    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_yg_proofborr[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_User getBorrower() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getBorrower_ID(), get_TrxName());
	}

	/** Set Borrower_ID.
		@param Borrower_ID Borrower_ID
	*/
	public void setBorrower_ID (int Borrower_ID)
	{
		if (Borrower_ID < 1)
			set_Value (COLUMNNAME_Borrower_ID, null);
		else
			set_Value (COLUMNNAME_Borrower_ID, Integer.valueOf(Borrower_ID));
	}

	/** Get Borrower_ID.
		@return Borrower_ID	  */
	public int getBorrower_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Borrower_ID);
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

	/** Set Due Date.
		@param DueDate Date when the payment is due
	*/
	public void setDueDate (Timestamp DueDate)
	{
		set_Value (COLUMNNAME_DueDate, DueDate);
	}

	/** Get Due Date.
		@return Date when the payment is due
	  */
	public Timestamp getDueDate()
	{
		return (Timestamp)get_Value(COLUMNNAME_DueDate);
	}

	public org.compiere.model.I_AD_User getReturner() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getReturner_ID(), get_TrxName());
	}

	/** Set Returner_ID.
		@param Returner_ID Returner_ID
	*/
	public void setReturner_ID (int Returner_ID)
	{
		if (Returner_ID < 1)
			set_Value (COLUMNNAME_Returner_ID, null);
		else
			set_Value (COLUMNNAME_Returner_ID, Integer.valueOf(Returner_ID));
	}

	/** Get Returner_ID.
		@return Returner_ID	  */
	public int getReturner_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Returner_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set borrowdate.
		@param borrowdate borrowdate
	*/
	public void setborrowdate (Timestamp borrowdate)
	{
		set_Value (COLUMNNAME_borrowdate, borrowdate);
	}

	/** Get borrowdate.
		@return borrowdate	  */
	public Timestamp getborrowdate()
	{
		return (Timestamp)get_Value(COLUMNNAME_borrowdate);
	}

	/** &#39046;&#29992;&#20013; = BO */
	public static final String BORROWSTATUS_领用中 = "BO";
	/** &#36926;&#26399;&#26410;&#36824; = OD */
	public static final String BORROWSTATUS_逾期未还 = "OD";
	/** &#24050;&#24402;&#36824; = RT */
	public static final String BORROWSTATUS_已归还 = "RT";
	/** Set borrowstatus.
		@param borrowstatus borrowstatus
	*/
	public void setborrowstatus (String borrowstatus)
	{

		set_Value (COLUMNNAME_borrowstatus, borrowstatus);
	}

	/** Get borrowstatus.
		@return borrowstatus	  */
	public String getborrowstatus()
	{
		return (String)get_Value(COLUMNNAME_borrowstatus);
	}

	/** &#22806;&#21457;&#23458;&#25143; = CL */
	public static final String BORROWTYPE_外发客户 = "CL";
	/** &#29983;&#20135;&#39046;&#29992; = PR */
	public static final String BORROWTYPE_生产领用 = "PR";
	/** &#22806;&#21457;&#26816;&#27979;&#26426;&#26500; = TL */
	public static final String BORROWTYPE_外发检测机构 = "TL";
	/** Set borrowtype.
		@param borrowtype borrowtype
	*/
	public void setborrowtype (String borrowtype)
	{

		set_Value (COLUMNNAME_borrowtype, borrowtype);
	}

	/** Get borrowtype.
		@return borrowtype	  */
	public String getborrowtype()
	{
		return (String)get_Value(COLUMNNAME_borrowtype);
	}

	/** Set isreturned.
		@param isreturned isreturned
	*/
	public void setisreturned (boolean isreturned)
	{
		set_Value (COLUMNNAME_isreturned, Boolean.valueOf(isreturned));
	}

	/** Get isreturned.
		@return isreturned	  */
	public boolean isreturned()
	{
		Object oo = get_Value(COLUMNNAME_isreturned);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}


	/** Set processstep.
		@param processstep processstep
	*/
	public void setprocessstep (int processstep)
	{
		set_Value (COLUMNNAME_processstep, Integer.valueOf(processstep));
	}

	/** Get processstep.
		@return processstep	  */
	public int getprocessstep()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_processstep);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set qtyborrowed.
		@param qtyborrowed qtyborrowed
	*/
	public void setqtyborrowed (BigDecimal qtyborrowed)
	{
		set_Value (COLUMNNAME_qtyborrowed, qtyborrowed);
	}

	/** Get qtyborrowed.
		@return qtyborrowed	  */
	public BigDecimal getqtyborrowed()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtyborrowed);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set qtyreturned.
		@param qtyreturned qtyreturned
	*/
	public void setqtyreturned (BigDecimal qtyreturned)
	{
		set_Value (COLUMNNAME_qtyreturned, qtyreturned);
	}

	/** Get qtyreturned.
		@return qtyreturned	  */
	public BigDecimal getqtyreturned()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtyreturned);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set returndate.
		@param returndate returndate
	*/
	public void setreturndate (Timestamp returndate)
	{
		set_Value (COLUMNNAME_returndate, returndate);
	}

	/** Get returndate.
		@return returndate	  */
	public Timestamp getreturndate()
	{
		return (Timestamp)get_Value(COLUMNNAME_returndate);
	}

	/** &#23436;&#22909; = A */
	public static final String RETURNSAMPLESTATUS_完好 = "A";
	/** &#19981;&#33391; = B */
	public static final String RETURNSAMPLESTATUS_不良 = "B";
	/** &#24453;&#25253;&#24223; = C */
	public static final String RETURNSAMPLESTATUS_待报废 = "C";
	/** Set returnsamplestatus.
		@param returnsamplestatus returnsamplestatus
	*/
	public void setreturnsamplestatus (String returnsamplestatus)
	{

		set_Value (COLUMNNAME_returnsamplestatus, returnsamplestatus);
	}

	/** Get returnsamplestatus.
		@return returnsamplestatus	  */
	public String getreturnsamplestatus()
	{
		return (String)get_Value(COLUMNNAME_returnsamplestatus);
	}

	/** Set workorder.
		@param workorder workorder
	*/
	public void setworkorder (int workorder)
	{
		set_Value (COLUMNNAME_workorder, Integer.valueOf(workorder));
	}

	/** Get workorder.
		@return workorder	  */
	public int getworkorder()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_workorder);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set yg_proofborr.
		@param yg_proofborr_ID yg_proofborr
	*/
	public void setyg_proofborr_ID (int yg_proofborr_ID)
	{
		if (yg_proofborr_ID < 1)
			set_ValueNoCheck (COLUMNNAME_yg_proofborr_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_yg_proofborr_ID, Integer.valueOf(yg_proofborr_ID));
	}

	/** Get yg_proofborr.
		@return yg_proofborr	  */
	public int getyg_proofborr_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_yg_proofborr_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set yg_proofborr_UU.
		@param yg_proofborr_UU yg_proofborr_UU
	*/
	public void setyg_proofborr_UU (String yg_proofborr_UU)
	{
		set_ValueNoCheck (COLUMNNAME_yg_proofborr_UU, yg_proofborr_UU);
	}

	/** Get yg_proofborr_UU.
		@return yg_proofborr_UU	  */
	public String getyg_proofborr_UU()
	{
		return (String)get_Value(COLUMNNAME_yg_proofborr_UU);
	}

	public I_yg_proofinventory getyg_proofinventory() throws RuntimeException
	{
		return (I_yg_proofinventory)MTable.get(getCtx(), I_yg_proofinventory.Table_ID)
			.getPO(getyg_proofinventory_ID(), get_TrxName());
	}

	/** Set yg_proofinventory.
		@param yg_proofinventory_ID yg_proofinventory
	*/
	public void setyg_proofinventory_ID (int yg_proofinventory_ID)
	{
		if (yg_proofinventory_ID < 1)
			set_ValueNoCheck (COLUMNNAME_yg_proofinventory_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_yg_proofinventory_ID, Integer.valueOf(yg_proofinventory_ID));
	}

	/** Get yg_proofinventory.
		@return yg_proofinventory	  */
	public int getyg_proofinventory_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_yg_proofinventory_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}