/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/

package org.wms.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.util.CCache;

public class MWM_EmptyStorage extends X_WM_EmptyStorage{

	private static final long serialVersionUID = -1L;

	public MWM_EmptyStorage(Properties ctx, int id, String trxName) {
		super(ctx,id,trxName);
		}

	public MWM_EmptyStorage(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
	

	public static MWM_EmptyStorage get (Properties ctx, int WM_EmptyStorage_ID, String trxName)
	{
		if (s_cache == null)
			s_cache	= new CCache<Integer,MWM_EmptyStorage>(Table_Name, 20);
		Integer key = Integer.valueOf(WM_EmptyStorage_ID);
		MWM_EmptyStorage retValue = (MWM_EmptyStorage) s_cache.get (key);
		if (retValue != null) {
			System.out.println("Cache get "+retValue);
			return retValue;
		}
		retValue = new MWM_EmptyStorage (ctx, WM_EmptyStorage_ID, trxName);
		if (retValue.get_ID () != 0) {
			s_cache.put (key, retValue); 
		}
		return retValue;
	} //	get

	/**	Cache						*/
	protected volatile static CCache<Integer,MWM_EmptyStorage> s_cache; 
	
}
