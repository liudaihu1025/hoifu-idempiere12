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
package org.wms.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.*;

/** Generated Model for WM_RouteLocation
 *  @author iDempiere (generated) 
 *  @version Release 4.1 - $Id$ */
public class X_WM_RouteLocation extends PO implements I_WM_RouteLocation, I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 20170422L;

    /** Standard Constructor */
    public X_WM_RouteLocation (Properties ctx, int WM_RouteLocation_ID, String trxName)
    {
      super (ctx, WM_RouteLocation_ID, trxName);
      /** if (WM_RouteLocation_ID == 0)
        {
			setWM_RouteLocation_ID (0);
        } */
    }

    /** Load Constructor */
    public X_WM_RouteLocation (Properties ctx, ResultSet rs, String trxName)
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
      StringBuffer sb = new StringBuffer ("X_WM_RouteLocation[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException
    {
		return (org.compiere.model.I_M_Warehouse)MTable.get(getCtx(), org.compiere.model.I_M_Warehouse.Table_Name)
			.getPO(getM_Warehouse_ID(), get_TrxName());	}

	/** Set Warehouse.
		@param M_Warehouse_ID 
		Storage Warehouse and Service Point
	  */
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_Warehouse_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Warehouse.
		@return Storage Warehouse and Service Point
	  */
	public int getM_Warehouse_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Name.
		@param Name 
		Alphanumeric identifier of the entity
	  */
	public void setName (String Name)
	{
		set_Value (COLUMNNAME_Name, Name);
	}

	/** Get Name.
		@return Alphanumeric identifier of the entity
	  */
	public String getName () 
	{
		return (String)get_Value(COLUMNNAME_Name);
	}

	/** Flow = Fl */
	public static final String ROUTEFLOW_Flow = "Fl";
	/** Layout = La */
	public static final String ROUTEFLOW_Layout = "La";
	/** Set RouteFLow.
		@param RouteFLow RouteFLow	  */
	public void setRouteFLow (String RouteFLow)
	{

		set_Value (COLUMNNAME_RouteFLow, RouteFLow);
	}

	/** Get RouteFLow.
		@return RouteFLow	  */
	public String getRouteFLow () 
	{
		return (String)get_Value(COLUMNNAME_RouteFLow);
	}

	/** ABL = AB */
	public static final String ROUTEORDER_ABL = "AB";
	/** ALB = AL */
	public static final String ROUTEORDER_ALB = "AL";
	/** BL = BL */
	public static final String ROUTEORDER_BL = "BL";
	/** Set RouteOrder.
		@param RouteOrder RouteOrder	  */
	public void setRouteOrder (String RouteOrder)
	{

		set_Value (COLUMNNAME_RouteOrder, RouteOrder);
	}

	/** Get RouteOrder.
		@return RouteOrder	  */
	public String getRouteOrder () 
	{
		return (String)get_Value(COLUMNNAME_RouteOrder);
	}

	/** Forkflit = Fo */
	public static final String VEHICLE_Forkflit = "Fo";
	/** Person = Pe */
	public static final String VEHICLE_Person = "Pe";
	/** Robot = Ro */
	public static final String VEHICLE_Robot = "Ro";
	/** Set Vehicle.
		@param Vehicle Vehicle	  */
	public void setVehicle (String Vehicle)
	{

		set_Value (COLUMNNAME_Vehicle, Vehicle);
	}

	/** Get Vehicle.
		@return Vehicle	  */
	public String getVehicle () 
	{
		return (String)get_Value(COLUMNNAME_Vehicle);
	}

	/** Set RouteLocation.
		@param WM_RouteLocation_ID RouteLocation	  */
	public void setWM_RouteLocation_ID (int WM_RouteLocation_ID)
	{
		if (WM_RouteLocation_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_WM_RouteLocation_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_WM_RouteLocation_ID, Integer.valueOf(WM_RouteLocation_ID));
	}

	/** Get RouteLocation.
		@return RouteLocation	  */
	public int getWM_RouteLocation_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_WM_RouteLocation_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}
}