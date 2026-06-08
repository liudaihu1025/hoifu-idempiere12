package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;
  
/**  
 *  GL Voucher Model  
 *  @author iDempiere  
 *  @version $Id$  
 */  
public class MGlVoucher extends X_Gl_Voucher  
{  
    /**  
     *   
     */  
    private static final long serialVersionUID = 20250401L;  
  
    /**  
     *  Standard Constructor  
     *  @param ctx context  
     *  @param Gl_Voucher_ID id  
     *  @param trxName transaction  
     */  
    public MGlVoucher (Properties ctx, int Gl_Voucher_ID, String trxName)  
    {  
        super (ctx, Gl_Voucher_ID, trxName);  
    }  
  
    /**  
     *  Load Constructor  
     *  @param ctx context  
     *  @param rs result set  
     *  @param trxName transaction  
     */  
    public MGlVoucher (Properties ctx, ResultSet rs, String trxName)  
    {  
        super (ctx, rs, trxName);  
    }  
  
    /**  
     *  UUID Constructor  
     *  @param ctx context  
     *  @param Gl_Voucher_UU uuid  
     *  @param trxName transaction  
     */  
    public MGlVoucher (Properties ctx, String Gl_Voucher_UU, String trxName)  
    {  
        super (ctx, Gl_Voucher_UU, trxName);  
    }  
  
    /**  
     *  Before Save  
     *  @param newRecord new  
     *  @return true  
     */  
    protected boolean beforeSave (boolean newRecord)  
	{
        return true;  
	}
}