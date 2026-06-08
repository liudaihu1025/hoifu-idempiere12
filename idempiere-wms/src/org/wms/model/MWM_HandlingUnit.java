/**
* Licensed under the KARMA v.1 Law of Sharing. As others have shared freely to you, so shall you share freely back to us.
* If you shall try to cheat and find a loophole in this license, then KARMA will exact your share,
* and your worldly gain shall come to naught and those who share shall gain eventually above you.
* In compliance with previous GPLv2.0 works of Jorg Janke, Low Heng Sin, Carlos Ruiz and contributors.
* This Module Creator is an idea put together and coded by Redhuan D. Oon (red1@red1.org)
*/
package org.wms.model;

import java.io.File;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.model.ModelValidator;
import java.util.logging.Level; 
import org.compiere.model.ModelValidationEngine;
import org.compiere.print.ReportEngine;
import org.compiere.process.DocAction;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CCache;
import org.compiere.util.Msg;

public class MWM_HandlingUnit extends X_WM_HandlingUnit implements DocAction {
	public MWM_HandlingUnit(Properties ctx, int id, String trxName) {
		super(ctx, id, trxName);
		if (id==0){
			setDocStatus(DOCSTATUS_Drafted);
			setDocAction (DOCACTION_Prepare);
			setProcessed(false);  
		}
		docstatus = getDocStatus();		
	}

	public MWM_HandlingUnit(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

	private String docstatus = "";
	private static final long serialVersionUID = 1L;

	/**	Process Message 			*/
	private String			m_processMsg = null;
	private boolean			m_justPrepared = false;

	protected boolean beforeSave (boolean newRecord)
	{
		return super.beforeSave(newRecord);
	} 

	protected boolean beforeDelete() {	 
		return super.beforeDelete();
	} 

	protected boolean afterSave (boolean newRecord, boolean success)
	{
		return super.afterSave(newRecord, success);
	} 
	
	protected boolean afterDelete(boolean success) {		 
		return super.afterDelete(success);
	} 
	
	public boolean processIt(String processAction) throws Exception {
 		DocumentEngine engine = new DocumentEngine (this, getDocStatus());
		return engine.processIt (processAction, getDocAction());
	}
  
	public String prepareIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_PREPARE);
		if (m_processMsg != null)
			return docstatus;
 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_PREPARE);
		if (m_processMsg != null)
			return docstatus;
		return DocAction.STATUS_InProgress; 
	}

 	public boolean approveIt() { 
		setDocStatus(DOCSTATUS_Approved); 
		return true;
 	}
 
 	public String completeIt() {
 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_BEFORE_COMPLETE);
 		if (m_processMsg != null)
			return docstatus;
   		m_processMsg = ModelValidationEngine.get().fireDocValidate(this, ModelValidator.TIMING_AFTER_COMPLETE);
 		if (m_processMsg != null)
			return docstatus; 
 		return DocAction.STATUS_Completed; 
	}
 	
 	public boolean voidIt() {
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_VOID);
 		if (m_processMsg != null)
			return false;
  		setDocStatus(DOCSTATUS_Voided);
 		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_VOID);
 		if (m_processMsg != null)
			return false;
  		return true; 
	}

 	public boolean closeIt() {
		if (log.isLoggable(Level.INFO)) log.info("closeIt - " + toString());
 	// Before Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_CLOSE);
 		if (m_processMsg != null)
			return false;
 		setDocStatus(DOCSTATUS_Closed);
 		// After Close
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_CLOSE);
 		if (m_processMsg != null)
			return false;
 		return true;
 	}

	public static MWM_HandlingUnit get (Properties ctx, int WM_HandlingUnit_ID, String trxName)
	{
		if (s_cache == null)
			s_cache	= new CCache<Integer,MWM_HandlingUnit>(Table_Name, 20);
		Integer key = Integer.valueOf(WM_HandlingUnit_ID);
		MWM_HandlingUnit retValue = (MWM_HandlingUnit) s_cache.get (key);
		if (retValue != null){
			System.out.println("Cache get "+retValue);
			return retValue;
		}
		retValue = new MWM_HandlingUnit (ctx, WM_HandlingUnit_ID, trxName);
		if (retValue.get_ID()!=0) {
			s_cache.put(key,retValue);
		}
		return retValue;
	} //	get

	/**	Cache						*/
	protected volatile static CCache<Integer,MWM_HandlingUnit> s_cache; 
	 	
  	public boolean reverseCorrectIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
 	// Before reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSECORRECT);
 		if (m_processMsg != null)
			return false;
 		setDocStatus(DOCSTATUS_Reversed);
 		// After reverseCorrect
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSECORRECT);
 		if (m_processMsg != null)
			return false;
 		return true;
 	}

 	public boolean reverseAccrualIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
 		// After reverseAccrual
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REVERSEACCRUAL);
		if (m_processMsg != null)
			return false;
 		return true;
	}

 	public boolean reActivateIt() {
		if (log.isLoggable(Level.INFO)) log.info(toString());
		// Before reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_BEFORE_REACTIVATE);
		if (m_processMsg != null)
			return false;
		// After reActivate
		m_processMsg = ModelValidationEngine.get().fireDocValidate(this,ModelValidator.TIMING_AFTER_REACTIVATE);
		if (m_processMsg != null)
			return false;
 		return true;
	}

 	public String getSummary() {
		// TODO Auto-generated method stub
		return null;
	}

 	public String getDocumentNo() {
		return Msg.getElement(getCtx(), X_WM_HandlingUnit.COLUMNNAME_WM_HandlingUnit_ID) + " " + getDocumentNo();
 	}

 	public String getDocumentInfo() {
		// TODO Auto-generated method stub
		return null;
	}

 	public File createPDF() {
		try
		{
			File temp = File.createTempFile(get_TableName()+get_ID()+"_", ".pdf");
			return createPDF (temp);
		}

		catch (Exception e)
		{
			log.severe("Could not create PDF - " + e.getMessage());
		}
		return null;
	}


 	/**
	 * 	Create PDF file
	 *	@param file output file
	 *	@return file if success
	 */
	public File createPDF (File file)
	{
		ReportEngine re = ReportEngine.get (getCtx(), ReportEngine.ORDER, getWM_HandlingUnit_ID());
		if (re == null)
			return null;
		return re.getPDF(file);
	}

	//	createPDF
 	public String getProcessMsg() {
		return m_processMsg;
	}

 	public int getDoc_User_ID() {
		// TODO Auto-generated method stub
		return 0;
	}

 	public int getC_Currency_ID() {
		// TODO Auto-generated method stub
		return 0;
	}

 	public BigDecimal getApprovalAmt() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean unlockIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean invalidateIt() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean rejectIt() {
		// TODO Auto-generated method stub
		return false;
	}
}


