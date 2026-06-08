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
package org.libero.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;
import org.compiere.util.Env;

/** Generated Model for C_WorkTeamMember
 *  @author iDempiere (generated)
 *  @version Release 12 - $Id$ */
@org.adempiere.base.Model(table="C_WorkTeamMember")
public class X_C_WorkTeamMember extends PO implements I_C_WorkTeamMember, I_Persistent
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20260518L;

    /** Standard Constructor */
    public X_C_WorkTeamMember (Properties ctx, int C_WorkTeamMember_ID, String trxName)
    {
      super (ctx, C_WorkTeamMember_ID, trxName);
      /** if (C_WorkTeamMember_ID == 0)
        {
			setAD_User_ID (0);
			setC_WorkTeamMember_ID (0);
			setC_WorkTeam_ID (0);
			setIsDefault (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_WorkTeamMember (Properties ctx, int C_WorkTeamMember_ID, String trxName, String ... virtualColumns)
    {
      super (ctx, C_WorkTeamMember_ID, trxName, virtualColumns);
      /** if (C_WorkTeamMember_ID == 0)
        {
			setAD_User_ID (0);
			setC_WorkTeamMember_ID (0);
			setC_WorkTeam_ID (0);
			setIsDefault (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_WorkTeamMember (Properties ctx, String C_WorkTeamMember_UU, String trxName)
    {
      super (ctx, C_WorkTeamMember_UU, trxName);
      /** if (C_WorkTeamMember_UU == null)
        {
			setAD_User_ID (0);
			setC_WorkTeamMember_ID (0);
			setC_WorkTeam_ID (0);
			setIsDefault (false);
// N
        } */
    }

    /** Standard Constructor */
    public X_C_WorkTeamMember (Properties ctx, String C_WorkTeamMember_UU, String trxName, String ... virtualColumns)
    {
      super (ctx, C_WorkTeamMember_UU, trxName, virtualColumns);
      /** if (C_WorkTeamMember_UU == null)
        {
			setAD_User_ID (0);
			setC_WorkTeamMember_ID (0);
			setC_WorkTeam_ID (0);
			setIsDefault (false);
// N
        } */
    }

    /** Load Constructor */
    public X_C_WorkTeamMember (Properties ctx, ResultSet rs, String trxName)
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
      StringBuilder sb = new StringBuilder ("X_C_WorkTeamMember[")
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

	/** Set C_WorkTeamMember.
		@param C_WorkTeamMember_ID C_WorkTeamMember
	*/
	public void setC_WorkTeamMember_ID (int C_WorkTeamMember_ID)
	{
		if (C_WorkTeamMember_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_WorkTeamMember_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_WorkTeamMember_ID, Integer.valueOf(C_WorkTeamMember_ID));
	}

	/** Get C_WorkTeamMember.
		@return C_WorkTeamMember	  */
	public int getC_WorkTeamMember_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_WorkTeamMember_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set C_WorkTeamMember_UU.
		@param C_WorkTeamMember_UU C_WorkTeamMember_UU
	*/
	public void setC_WorkTeamMember_UU (String C_WorkTeamMember_UU)
	{
		set_ValueNoCheck (COLUMNNAME_C_WorkTeamMember_UU, C_WorkTeamMember_UU);
	}

	/** Get C_WorkTeamMember_UU.
		@return C_WorkTeamMember_UU	  */
	public String getC_WorkTeamMember_UU()
	{
		return (String)get_Value(COLUMNNAME_C_WorkTeamMember_UU);
	}

	public I_C_WorkTeam getC_WorkTeam() throws RuntimeException
	{
		return (I_C_WorkTeam)MTable.get(getCtx(), I_C_WorkTeam.Table_ID)
			.getPO(getC_WorkTeam_ID(), get_TrxName());
	}

	/** Set C_WorkTeam.
		@param C_WorkTeam_ID C_WorkTeam
	*/
	public void setC_WorkTeam_ID (int C_WorkTeam_ID)
	{
		if (C_WorkTeam_ID < 1)
			set_ValueNoCheck (COLUMNNAME_C_WorkTeam_ID, null);
		else
			set_ValueNoCheck (COLUMNNAME_C_WorkTeam_ID, Integer.valueOf(C_WorkTeam_ID));
	}

	/** Get C_WorkTeam.
		@return C_WorkTeam	  */
	public int getC_WorkTeam_ID()
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_C_WorkTeam_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Default.
		@param IsDefault Default value
	*/
	public void setIsDefault (boolean IsDefault)
	{
		set_Value (COLUMNNAME_IsDefault, Boolean.valueOf(IsDefault));
	}

	/** Get Default.
		@return Default value
	  */
	public boolean isDefault()
	{
		Object oo = get_Value(COLUMNNAME_IsDefault);
		if (oo != null)
		{
			 if (oo instanceof Boolean)
				 return ((Boolean)oo).booleanValue();
			return "Y".equals(oo);
		}
		return false;
	}

	/** 2&#25163; = 2H */
	public static final String POSITION_2手 = "2H";
	/** 3&#25163; = 3H */
	public static final String POSITION_3手 = "3H";
	/** &#23398;&#24466; = AP */
	public static final String POSITION_学徒 = "AP";
	/** &#26426;&#38271; = CL */
	public static final String POSITION_机长 = "CL";
	/** Set Position.
		@param Position Position
	*/
	public void setPosition (String Position)
	{

		set_Value (COLUMNNAME_Position, Position);
	}

	/** Get Position.
		@return Position	  */
	public String getPosition()
	{
		return (String)get_Value(COLUMNNAME_Position);
	}

	/** Set pieceratio.
		@param pieceratio pieceratio
	*/
	public void setpieceratio (BigDecimal pieceratio)
	{
		set_Value (COLUMNNAME_pieceratio, pieceratio);
	}

	/** Get pieceratio.
		@return pieceratio	  */
	public BigDecimal getpieceratio()
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_pieceratio);
		return bd;
	}
}