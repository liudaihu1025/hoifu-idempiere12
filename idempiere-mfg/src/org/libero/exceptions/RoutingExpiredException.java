/**
 * 
 */
package org.libero.exceptions;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.I_AD_Workflow;

/**
 * Thrown when Routing(Workflow) is not valid on given date
 * @author Teo Sarca
 */
public class RoutingExpiredException extends AdempiereException
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -7522979292063177848L;

	public RoutingExpiredException(I_AD_Workflow wf, Timestamp date)
	{
		super(buildMessage(wf, date));
	}
	
	private static final String buildMessage(I_AD_Workflow wf, Timestamp date)
	{
		 SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); 
		//return "@NotValid@ @AD_Workflow_ID@:"+wf.getValue()+" -当前 @Date@:"+date;
		return "当前工单开始时间-" + df.format(date) + "-不在该工艺路线（" + wf.getValue() + "）有效期内";
	}

}
