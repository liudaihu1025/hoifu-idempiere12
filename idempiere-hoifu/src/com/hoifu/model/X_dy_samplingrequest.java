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

/** Generated Model for dy_samplingrequest
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="dy_samplingrequest")
public class X_dy_samplingrequest extends PO implements I_dy_samplingrequest, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260613L;

    /** Standard Constructor */
    public X_dy_samplingrequest (Properties ctx, int dy_samplingrequest_ID, String trxName)
    {
      super (ctx, dy_samplingrequest_ID, trxName);
      /** if (dy_samplingrequest_ID == 0)
        {
			setAD_User_ID (0);
			setC_BPartner_ID (0);
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDocumentNo (null);
			setName (null);
			setdy_samplingrequest_ID (0);
			setischarge (false);
// N
			setsamplingstatus (null);
// DR
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingrequest (Properties ctx, int dy_samplingrequest_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingrequest_ID, trxName, virtualColumns);
      /** if (dy_samplingrequest_ID == 0)
        {
			setAD_User_ID (0);
			setC_BPartner_ID (0);
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDocumentNo (null);
			setName (null);
			setdy_samplingrequest_ID (0);
			setischarge (false);
// N
			setsamplingstatus (null);
// DR
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingrequest (Properties ctx, String dy_samplingrequest_UU, String trxName)
    {
      super (ctx, dy_samplingrequest_UU, trxName);
      /** if (dy_samplingrequest_UU == null)
        {
			setAD_User_ID (0);
			setC_BPartner_ID (0);
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDocumentNo (null);
			setName (null);
			setdy_samplingrequest_ID (0);
			setischarge (false);
// N
			setsamplingstatus (null);
// DR
        } */
    }

    /** Standard Constructor */
    public X_dy_samplingrequest (Properties ctx, String dy_samplingrequest_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, dy_samplingrequest_UU, trxName, virtualColumns);
      /** if (dy_samplingrequest_UU == null)
        {
			setAD_User_ID (0);
			setC_BPartner_ID (0);
			setDatePromised (new Timestamp( System.currentTimeMillis() ));
			setDocumentNo (null);
			setName (null);
			setdy_samplingrequest_ID (0);
			setischarge (false);
// N
			setsamplingstatus (null);
// DR
        } */
    }

    /** Load Constructor */
    public X_dy_samplingrequest (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_dy_samplingrequest[")
        .append(get_ID()).append(",Name=").append(getName()).append("]");
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
			set_Value (COLUMNNAME_Approver_ID, null);
		else
			set_Value (COLUMNNAME_Approver_ID, Integer.valueOf(Approver_ID));
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
			set_Value (COLUMNNAME_BizAssistant_ID, null);
		else
			set_Value (COLUMNNAME_BizAssistant_ID, Integer.valueOf(BizAssistant_ID));
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

	public org.compiere.model.I_C_Order getC_Order() throws RuntimeException
	{
		return (org.compiere.model.I_C_Order)MTable.get(getCtx(), org.compiere.model.I_C_Order.Table_ID)
			.getPO(getC_Order_ID(), get_TrxName());
	}

	/** Set &#35746;&#21333;.
		@param C_Order_ID Order
	*/
	public void setC_Order_ID (int C_Order_ID)
	{
		if (C_Order_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_Order_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_Order_ID, Integer.valueOf(C_Order_ID));
	}

	/** Get &#35746;&#21333;.
		@return Order
	  */
	public int getC_Order_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_Order_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Charge amount.
		@param ChargeAmt Charge Amount
	*/
	public void setChargeAmt (BigDecimal ChargeAmt)
	{
		set_Value (COLUMNNAME_ChargeAmt, ChargeAmt);
	}

	/** Get Charge amount.
		@return Charge Amount
	  */
	public BigDecimal getChargeAmt()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_ChargeAmt);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set Date Promised.
		@param DatePromised Date Order was promised
	*/
	public void setDatePromised (Timestamp DatePromised)
	{
		set_ValueNoCheck (COLUMNNAME_DatePromised, DatePromised);
	}

	/** Get Date Promised.
		@return Date Order was promised
	  */
	public Timestamp getDatePromised()
	{
		return (Timestamp)get_Value(COLUMNNAME_DatePromised);
	}

	public org.compiere.model.I_AD_User getDesignReceiver() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getDesignReceiver_ID(), get_TrxName());
	}

	/** Set DesignReceiver_ID.
		@param DesignReceiver_ID DesignReceiver_ID
	*/
	public void setDesignReceiver_ID (int DesignReceiver_ID)
	{
		if (DesignReceiver_ID < 1)
			set_Value (COLUMNNAME_DesignReceiver_ID, null);
		else
			set_Value (COLUMNNAME_DesignReceiver_ID, Integer.valueOf(DesignReceiver_ID));
	}

	/** Get DesignReceiver_ID.
		@return DesignReceiver_ID	  */
	public int getDesignReceiver_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_DesignReceiver_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
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

	public org.compiere.model.I_AD_User getEngReceiver() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getEngReceiver_ID(), get_TrxName());
	}

	/** Set EngReceiver_ID.
		@param EngReceiver_ID EngReceiver_ID
	*/
	public void setEngReceiver_ID (int EngReceiver_ID)
	{
		if (EngReceiver_ID < 1)
			set_Value (COLUMNNAME_EngReceiver_ID, null);
		else
			set_Value (COLUMNNAME_EngReceiver_ID, Integer.valueOf(EngReceiver_ID));
	}

	/** Get EngReceiver_ID.
		@return EngReceiver_ID	  */
	public int getEngReceiver_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_EngReceiver_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name Alphanumeric identifier of the entity
	*/
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName()
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	public org.compiere.model.I_AD_User getSalesRep() throws RuntimeException
	{
		return (org.compiere.model.I_AD_User)MTable.get(getCtx(), org.compiere.model.I_AD_User.Table_ID)
			.getPO(getSalesRep_ID(), get_TrxName());
	}

	/** Set Sales Representative.
		@param SalesRep_ID Sales Representative or Company Agent
	*/
	public void setSalesRep_ID (int SalesRep_ID)
	{
		if (SalesRep_ID < 1)
			set_ValueNoCheck (COLUMNNAME_SalesRep_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_SalesRep_ID, Integer.valueOf(SalesRep_ID));
	}

	/** Get Sales Representative.
		@return Sales Representative or Company Agent
	  */
	public int getSalesRep_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_SalesRep_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set approvedate.
		@param approvedate approvedate
	*/
	public void setapprovedate (Timestamp approvedate)
	{
		set_Value (COLUMNNAME_approvedate, approvedate);
	}

	/** Get approvedate.
		@return approvedate	  */
	public Timestamp getapprovedate()
	{
		return (Timestamp)get_Value(COLUMNNAME_approvedate);
	}

	/** Set approveresult.
		@param approveresult approveresult
	*/
	public void setapproveresult (String approveresult)
	{
		set_Value (COLUMNNAME_approveresult, approveresult);
	}

	/** Get approveresult.
		@return approveresult	  */
	public String getapproveresult()
	{
		return (String)get_Value(COLUMNNAME_approveresult);
	}

	/** Set bizassistant_phone.
		@param bizassistant_phone bizassistant_phone
	*/
	public void setbizassistant_phone (String bizassistant_phone)
	{
		set_Value (COLUMNNAME_bizassistant_phone, bizassistant_phone);
	}

	/** Get bizassistant_phone.
		@return bizassistant_phone	  */
	public String getbizassistant_phone()
	{
		return (String)get_Value(COLUMNNAME_bizassistant_phone);
	}

	/** &#21512;&#20316;&#21360;&#21047;&#21378; = CP */
	public static final String CUSTOMERCATEGORY_合作印刷厂 = "CP";
	/** &#22806;&#28895;&#23458;&#25143; = WY */
	public static final String CUSTOMERCATEGORY_外烟客户 = "WY";
	/** &#20013;&#28895;&#23458;&#25143; = ZY */
	public static final String CUSTOMERCATEGORY_中烟客户 = "ZY";
	/** Set customercategory.
		@param customercategory customercategory
	*/
	public void setcustomercategory (String customercategory)
	{

		set_Value (COLUMNNAME_customercategory, customercategory);
	}

	/** Get customercategory.
		@return customercategory	  */
	public String getcustomercategory()
	{
		return (String)get_Value(COLUMNNAME_customercategory);
	}

	/** Set customerrequirement.
		@param customerrequirement customerrequirement
	*/
	public void setcustomerrequirement (String customerrequirement)
	{
		set_Value (COLUMNNAME_customerrequirement, customerrequirement);
	}

	/** Get customerrequirement.
		@return customerrequirement	  */
	public String getcustomerrequirement()
	{
		return (String)get_Value(COLUMNNAME_customerrequirement);
	}

	/** Set designreceivedate.
		@param designreceivedate designreceivedate
	*/
	public void setdesignreceivedate (Timestamp designreceivedate)
	{
		set_Value (COLUMNNAME_designreceivedate, designreceivedate);
	}

	/** Get designreceivedate.
		@return designreceivedate	  */
	public Timestamp getdesignreceivedate()
	{
		return (Timestamp)get_Value(COLUMNNAME_designreceivedate);
	}

	/** Set designtype.
		@param designtype designtype
	*/
	public void setdesigntype (String designtype)
	{
		set_Value (COLUMNNAME_designtype, designtype);
	}

	/** Get designtype.
		@return designtype	  */
	public String getdesigntype()
	{
		return (String)get_Value(COLUMNNAME_designtype);
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

	/** Set dy_samplingrequest_UU.
		@param dy_samplingrequest_UU dy_samplingrequest_UU
	*/
	public void setdy_samplingrequest_UU (String dy_samplingrequest_UU)
	{
		set_ValueNoCheck (COLUMNNAME_dy_samplingrequest_UU, dy_samplingrequest_UU);
	}

	/** Get dy_samplingrequest_UU.
		@return dy_samplingrequest_UU	  */
	public String getdy_samplingrequest_UU()
	{
		return (String)get_Value(COLUMNNAME_dy_samplingrequest_UU);
	}

	/** Set engreceivedate.
		@param engreceivedate engreceivedate
	*/
	public void setengreceivedate (Timestamp engreceivedate)
	{
		set_Value (COLUMNNAME_engreceivedate, engreceivedate);
	}

	/** Get engreceivedate.
		@return engreceivedate	  */
	public Timestamp getengreceivedate()
	{
		return (Timestamp)get_Value(COLUMNNAME_engreceivedate);
	}

	/** Set ischarge.
		@param ischarge ischarge
	*/
	public void setischarge (boolean ischarge)
	{
		set_Value (COLUMNNAME_ischarge, Boolean.valueOf(ischarge));
	}

	/** Get ischarge.
		@return ischarge	  */
	public boolean ischarge()
	{
		Object oo = get_Value(COLUMNNAME_ischarge);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set publishsampling.
		@param publishsampling publishsampling
	*/
	public void setpublishsampling (String publishsampling)
	{
		set_Value (COLUMNNAME_publishsampling, publishsampling);
	}

	/** Get publishsampling.
		@return publishsampling	  */
	public String getpublishsampling()
	{
		return (String)get_Value(COLUMNNAME_publishsampling);
	}

	/** Set qtydesign.
		@param qtydesign qtydesign
	*/
	public void setqtydesign (BigDecimal qtydesign)
	{
		set_Value (COLUMNNAME_qtydesign, qtydesign);
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

	/** Set qtydesignplan.
		@param qtydesignplan qtydesignplan
	*/
	public void setqtydesignplan (BigDecimal qtydesignplan)
	{
		set_Value (COLUMNNAME_qtydesignplan, qtydesignplan);
	}

	/** Get qtydesignplan.
		@return qtydesignplan	  */
	public BigDecimal getqtydesignplan()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_qtydesignplan);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}

	/** Set salesrep_phone.
		@param salesrep_phone salesrep_phone
	*/
	public void setsalesrep_phone (String salesrep_phone)
	{
		set_ValueNoCheck (COLUMNNAME_salesrep_phone, salesrep_phone);
	}

	/** Get salesrep_phone.
		@return salesrep_phone	  */
	public String getsalesrep_phone()
	{
		return (String)get_Value(COLUMNNAME_salesrep_phone);
	}

	/** &#33609;&#31295; = DR */
	public static final String SAMPLINGSTATUS_草稿 = "DR";
	/** &#24050;&#21457;&#24067; = PB */
	public static final String SAMPLINGSTATUS_已发布 = "PB";
	/** Set samplingstatus.
		@param samplingstatus samplingstatus
	*/
	public void setsamplingstatus (String samplingstatus)
	{

		set_Value (COLUMNNAME_samplingstatus, samplingstatus);
	}

	/** Get samplingstatus.
		@return samplingstatus	  */
	public String getsamplingstatus()
	{
		return (String)get_Value(COLUMNNAME_samplingstatus);
	}
}