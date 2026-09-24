package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.compiere.model.PO;
import org.compiere.model.Query;
import org.json.JSONObject;

public class MRestApiCallLog extends X_HF_Rest_Api_Call_Log {

	public MRestApiCallLog(Properties ctx, int X_HF_Rest_Api_Call_Log_ID, String trxName) {
		super(ctx, X_HF_Rest_Api_Call_Log_ID, trxName);
	}

	public MRestApiCallLog(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}



	/**  
	 * 根据 RequestCode 判断是否已经存在调用记录（幂等判断）  
	 */  
	public static boolean isRequestCodeExists(String requestCode, Properties ctx, String trxName) {  
	    if (requestCode == null || requestCode.isEmpty())  
	        return false;  
	    int id = new Query(ctx, "HF_Rest_Api_Call_Log",  
	            "RequestCode=?", trxName)  
	            .setParameters(requestCode)  
	            .setClient_ID()  
	            .firstId();  
	    return id > 0;  
	}  
	  
	/**  
	 * 根据 RequestCode 获取上一次的 ResponseBody  
	 */  
	public static String getLastResponseBodyByRequestCode(String requestCode, Properties ctx, String trxName) {  
	    if (requestCode == null || requestCode.isEmpty())  
	        return null;  
	    PO po = new Query(ctx, "HF_Rest_Api_Call_Log",  
	            "RequestCode=?", trxName)  
	            .setParameters(requestCode)  
	            .setClient_ID()  
	            .setOrderBy("Created DESC")  
	            .first();  
	    return po != null ? po.get_ValueAsString("ResponseBody") : null;  
	}
	
	/**  
	 * 根据 RequestCode 获取上一次的 ResponseBody的jsonData 
	 */  
	public static String getLastResponseBodyDataByRequestCode(String requestCode, Properties ctx, String trxName) {  
	    String lastResponseBody = getLastResponseBodyByRequestCode(requestCode, ctx, trxName);  
	    if (lastResponseBody == null || lastResponseBody.isEmpty())  
	        return lastResponseBody;  
	  
	    try {  
	        JSONObject root = new JSONObject(lastResponseBody);  
	        if (root.has("data")) {  
	            return root.get("data").toString();  
	        }  
	        // 兜底：历史记录本身不是标准envelope（例如以前存的是纯data），直接用原始内容  
	        return lastResponseBody;  
	    } catch (Exception e) {  
	        return lastResponseBody; // 兜底，避免返回 null 造成后续 setJsonData 出问题  
	    }  
	}
	
}
