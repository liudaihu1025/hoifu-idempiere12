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

/** Generated Model for dy_samplingreview
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_samplingreview")
public class X_dy_samplingreview extends PO implements I_dy_samplingreview, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260622L;

    /** Standard Constructor */
    public X_dy_samplingreview (Properties ctx, int dy_samplingreview_ID, String trxName)
    {
      super (ctx, dy_samplingreview_ID, trxName);
      /** if (dy_samplingreview_ID == 0)
        {
			setAD_User_ID (0);
			setC_BPartner_ID (0);
			setDocumentNo (null);
			setdy_samplingphase_ID (0);
			setdy_samplingrequest_ID (0);
			setdy_samplingreview_ID (0);
			setreviewers (null);
			setreviewstatus (null);
			setsubmitdate (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreview (Properties ctx, int dy_samplingreview_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingreview_ID, trxName, virtualColumns);
      /** if (dy_samplingreview_ID == 0)
        {
			setAD_User_ID (0);
			setC_BPartner_ID (0);
			setDocumentNo (null);
			setdy_samplingphase_ID (0);
			setdy_samplingrequest_ID (0);
			setdy_samplingreview_ID (0);
			setreviewers (null);
			setreviewstatus (null);
			setsubmitdate (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreview (Properties ctx, String dy_samplingreview_UU, String trxName)
    {
      super (ctx, dy_samplingreview_UU, trxName);
      /** if (dy_samplingreview_UU == null)
        {
			setAD_User_ID (0);
			setC_BPartner_ID (0);
			setDocumentNo (null);
			setdy_samplingphase_ID (0);
			setdy_samplingrequest_ID (0);
			setdy_samplingreview_ID (0);
			setreviewers (null);
			setreviewstatus (null);
			setsubmitdate (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingreview (Properties ctx, String dy_samplingreview_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingreview_UU, trxName, virtualColumns);
      /** if (dy_samplingreview_UU == null)
        {
			setAD_User_ID (0);
			setC_BPartner_ID (0);
			setDocumentNo (null);
			setdy_samplingphase_ID (0);
			setdy_samplingrequest_ID (0);
			setdy_samplingreview_ID (0);
			setreviewers (null);
			setreviewstatus (null);
			setsubmitdate (new Timestamp( System.currentTimeMillis() ));
        } */
    }

    /** Load Constructor */
    public X_dy_samplingreview (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_dy_samplingreview[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getAD_User_ID(), get_TrxName());
	}

	/** Set User/Contact.
		@param AD_User_ID User within the system - Internal or Business Partner Contact
	*/
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1)
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get User/Contact.
		@return User within the system - Internal or Business Partner Contact
	  */
	public int getAD_User_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getApprover() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getApprover_ID(), get_TrxName());
	}

	/** Set Approver_ID.
		@param Approver_ID Approver_ID
	*/
	public void setApprover_ID (int Approver_ID)
	{
		if (Approver_ID < 1)
			set_ValueNoCheck (COLUMNNAME_Approver_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_Approver_ID, Integer.valueOf(Approver_ID));
	}

	/** Get Approver_ID.
		@return Approver_ID	  */
	public int getApprover_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Approver_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_AD_User getBizAssistant() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getBizAssistant_ID(), get_TrxName());
	}

	/** Set BizAssistant_ID.
		@param BizAssistant_ID BizAssistant_ID
	*/
	public void setBizAssistant_ID (int BizAssistant_ID)
	{
		if (BizAssistant_ID < 1)
			set_ValueNoCheck (COLUMNNAME_BizAssistant_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_BizAssistant_ID, Integer.valueOf(BizAssistant_ID));
	}

	/** Get BizAssistant_ID.
		@return BizAssistant_ID	  */
	public int getBizAssistant_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_BizAssistant_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public org.compiere.model.I_C_BPartner getC_BPartner() throws RuntimeException
	{
		return (org.compiere.model.I_C_BPartner)MTable.get(getCtx(), org.compiere.model.I_C_BPartner.Table_ID)
			.getPO(getC_BPartner_ID(), get_TrxName());
	}

	/** Set &#24448;&#26469;&#21333;&#20301;.
		@param C_BPartner_ID Identifies a Business Partner
	*/
	public void setC_BPartner_ID (int C_BPartner_ID)
	{
		if (C_BPartner_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_BPartner_ID, Integer.valueOf(C_BPartner_ID));
	}

	/** Get &#24448;&#26469;&#21333;&#20301;.
		@return Identifies a Business Partner
	  */
	public int getC_BPartner_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_BPartner_ID);
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

	/** Set approvedate.
		@param approvedate approvedate
	*/
	public void setapprovedate (Timestamp approvedate)
	{
		set_ValueNoCheck (COLUMNNAME_approvedate, approvedate);
	}

	/** Get approvedate.
		@return approvedate	  */
	public Timestamp getapprovedate()
	{
		return (Timestamp)get_Value(COLUMNNAME_approvedate);
	}

	public I_dy_samplingphase getdy_samplingphase() throws RuntimeException
	{
		return (I_dy_samplingphase)MTable.get(getCtx(), I_dy_samplingphase.Table_ID)
			.getPO(getdy_samplingphase_ID(), get_TrxName());
	}

	/** Set dy_samplingphase.
		@param dy_samplingphase_ID dy_samplingphase
	*/
	public void setdy_samplingphase_ID (int dy_samplingphase_ID)
	{
		if (dy_samplingphase_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_samplingphase_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingphase_ID, Integer.valueOf(dy_samplingphase_ID));
	}

	/** Get dy_samplingphase.
		@return dy_samplingphase	  */
	public int getdy_samplingphase_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingphase_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	public I_dy_samplingrequest getdy_samplingrequest() throws RuntimeException
	{
		return (I_dy_samplingrequest)MTable.get(getCtx(), I_dy_samplingrequest.Table_ID)
			.getPO(getdy_samplingrequest_ID(), get_TrxName());
	}

	/** Set dy_samplingrequest.
		@param dy_samplingrequest_ID dy_samplingrequest
	*/
	public void setdy_samplingrequest_ID (int dy_samplingrequest_ID)
	{
		if (dy_samplingrequest_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_samplingrequest_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingrequest_ID, Integer.valueOf(dy_samplingrequest_ID));
	}

	/** Get dy_samplingrequest.
		@return dy_samplingrequest	  */
	public int getdy_samplingrequest_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingrequest_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_samplingreview.
		@param dy_samplingreview_ID dy_samplingreview
	*/
	public void setdy_samplingreview_ID (int dy_samplingreview_ID)
	{
		if (dy_samplingreview_ID < 1)
			set_ValueNoCheck (COLUMNNAME_dy_samplingreview_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_dy_samplingreview_ID, Integer.valueOf(dy_samplingreview_ID));
	}

	/** Get dy_samplingreview.
		@return dy_samplingreview	  */
	public int getdy_samplingreview_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_dy_samplingreview_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set dy_samplingreview_UU.
		@param dy_samplingreview_UU dy_samplingreview_UU
	*/
	public void setdy_samplingreview_UU (String dy_samplingreview_UU)
	{
		set_ValueNoCheck (COLUMNNAME_dy_samplingreview_UU, dy_samplingreview_UU);
	}

	/** Get dy_samplingreview_UU.
		@return dy_samplingreview_UU	  */
	public String getdy_samplingreview_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_samplingreview_UU);
	}

	/** Set finalcomment.
		@param finalcomment finalcomment
	*/
	public void setfinalcomment (String finalcomment)
	{
		set_Value (COLUMNNAME_finalcomment, finalcomment);
	}

	/** Get finalcomment.
		@return finalcomment	  */
	public String getfinalcomment()
	{
		return (String)get_Value(COLUMNNAME_finalcomment);
	}

	/** &#24179;&#38754;&#35774;&#35745; = DS */
	public static final String PHASETYPE_平面设计 = "DS";
	/** &#24037;&#33402;&#35774;&#35745; = EN */
	public static final String PHASETYPE_工艺设计 = "EN";
	/** &#29983;&#20135;&#25171;&#26679; = PR */
	public static final String PHASETYPE_生产打样 = "PR";
	/** Set phasetype.
		@param phasetype phasetype
	*/
	public void setphasetype (String phasetype)
	{

		set_ValueNoCheck (COLUMNNAME_phasetype, phasetype);
	}

	/** Get phasetype.
		@return phasetype	  */
	public String getphasetype()
	{
		return (String)get_Value(COLUMNNAME_phasetype);
	}

	/** Set qtydesign.
		@param qtydesign qtydesign
	*/
	public void setqtydesign (BigDecimal qtydesign)
	{
		set_ValueNoCheck (COLUMNNAME_qtydesign, qtydesign);
	}

	/** Get qtydesign.
		@return qtydesign	  */
	public BigDecimal getqtydesign()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtydesign);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** reviewers AD_Reference_ID=110 */
	public static final int REVIEWERS_AD_Reference_ID=110;
	/** Set reviewers.
		@param reviewers reviewers
	*/
	public void setreviewers (String reviewers)
	{

		set_Value (COLUMNNAME_reviewers, reviewers);
	}

	/** Get reviewers.
		@return reviewers	  */
	public String getreviewers()
	{
		return (String)get_Value(COLUMNNAME_reviewers);
	}

	/** &#24050;&#20851;&#38381; = CL */
	public static final String REVIEWSTATUS_已关闭 = "CL";
	/** &#24050;&#32456;&#23457; = FA */
	public static final String REVIEWSTATUS_已终审 = "FA";
	/** &#24050;&#35780;&#23457; = RD */
	public static final String REVIEWSTATUS_已评审 = "RD";
	/** &#35780;&#23457;&#20013; = RV */
	public static final String REVIEWSTATUS_评审中 = "RV";
	/** Set reviewstatus.
		@param reviewstatus reviewstatus
	*/
	public void setreviewstatus (String reviewstatus)
	{

		set_Value (COLUMNNAME_reviewstatus, reviewstatus);
	}

	/** Get reviewstatus.
		@return reviewstatus	  */
	public String getreviewstatus()
	{
		return (String)get_Value(COLUMNNAME_reviewstatus);
	}

	/** Set submitdate.
		@param submitdate submitdate
	*/
	public void setsubmitdate (Timestamp submitdate)
	{
		set_Value (COLUMNNAME_submitdate, submitdate);
	}

	/** Get submitdate.
		@return submitdate	  */
	public Timestamp getsubmitdate()
	{
		return (Timestamp)get_Value(COLUMNNAME_submitdate);
	}

	/** Set submitremark.
		@param submitremark submitremark
	*/
	public void setsubmitremark (String submitremark)
	{
		set_Value (COLUMNNAME_submitremark, submitremark);
	}

	/** Get submitremark.
		@return submitremark	  */
	public String getsubmitremark()
	{
		return (String)get_Value(COLUMNNAME_submitremark);
	}
}