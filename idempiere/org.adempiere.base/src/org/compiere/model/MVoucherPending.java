package org.compiere.model;
import java.io.File;  
import java.math.BigDecimal;  
import java.sql.ResultSet;  
import java.sql.Timestamp;  
import java.util.Properties;  
  
import org.adempiere.model.DocActionDelegate;  
import org.compiere.process.DocAction;  
import org.compiere.util.Env;  
import org.compiere.util.Util;  
  
/**  
 * Voucher Pending Model  
 */  
public class MVoucherPending extends X_Gl_Voucher_Pending implements DocAction {  
  
    private static final long serialVersionUID = 1L;  
  
    private DocActionDelegate<MVoucherPending> docActionDelegate;  
  
    // ── 构造函数 ──────────────────────────────────────────────  
  
    public MVoucherPending(Properties ctx, String Gl_Voucher_Pending_UU, String trxName) {  
        super(ctx, Gl_Voucher_Pending_UU, trxName);  
        init();  // ← 先初始化 delegate  
        if (Util.isEmpty(Gl_Voucher_Pending_UU))  
            setInitialDefaults();  
    }  
      
    public MVoucherPending(Properties ctx, int Gl_Voucher_Pending_ID, String trxName) {  
        super(ctx, Gl_Voucher_Pending_ID, trxName);  
        init();  // ← 先初始化 delegate  
        if (Gl_Voucher_Pending_ID == 0)  
            setInitialDefaults();  
    }  
      
    public MVoucherPending(Properties ctx, ResultSet rs, String trxName) {  
        super(ctx, rs, trxName);  
        init();  
    } 
  
    private void setInitialDefaults() {  
        setDocAction(DOCACTION_Complete);  
        setDocStatus(DOCSTATUS_Drafted);  
        setPostingType(POSTINGTYPE_Actual);  
        setIsApproved(false);  
        setIsPrinted(false);  
        setProcessed(false);  
        setTotalDr(Env.ZERO);  
        setTotalCr(Env.ZERO);  
        setControlAmt(Env.ZERO);  
        setDateDoc(new Timestamp(System.currentTimeMillis()));  
    }  
  
    private void init() {  
        docActionDelegate = new DocActionDelegate<>(this);  
        // 如需注册业务逻辑，在此添加：  
        docActionDelegate.setActionCallable(DocAction.ACTION_Complete, () -> doComplete());  
    }  
  
    private String doComplete() {
    	return null;
    }
    // ── DocAction 接口方法（全部委托给 docActionDelegate）────  
  
    @Override public boolean processIt(String action) throws Exception { return docActionDelegate.processIt(action); }  
    @Override public boolean unlockIt()          { return docActionDelegate.unlockIt(); }  
    @Override public boolean invalidateIt()      { return docActionDelegate.invalidateIt(); }  
    @Override public String  prepareIt()         { return docActionDelegate.prepareIt(); }  
    @Override public boolean approveIt()         { return docActionDelegate.approveIt(); }  
    @Override public boolean rejectIt()          { return docActionDelegate.rejectIt(); }   
    @Override public String  completeIt()        { return docActionDelegate.completeIt(); }   
    @Override public boolean voidIt()            { return docActionDelegate.voidIt(); }  
    @Override public boolean closeIt()           { return docActionDelegate.closeIt(); }  
    @Override public boolean reverseCorrectIt()  { return docActionDelegate.reverseCorrectIt(); }  
    @Override public boolean reverseAccrualIt()  { return docActionDelegate.reverseAccrualIt(); }  
    @Override public boolean reActivateIt()      { return false; }  
    @Override public File    createPDF()         { return docActionDelegate.createPDF(); }  
    @Override public String  getProcessMsg()     { return docActionDelegate.getProcessMsg(); }  
    @Override public int     getC_Currency_ID()  { return docActionDelegate.getC_Currency_ID(); }  
    @Override public String  getDocAction()      { return docActionDelegate.getDocAction(); }  
    @Override public void    setDocStatus(String s) { docActionDelegate.setDocStatus(s); }  
    @Override public String  getDocStatus()      { return docActionDelegate.getDocStatus(); }  
  
    @Override  
    public String getSummary()      { return getDocumentNo(); }  
    @Override  
    public String getDocumentInfo() { return getDocumentNo(); }  
    @Override  
    public int getDoc_User_ID()     { return docActionDelegate.getDoc_User_ID(); }  
    @Override  
    public BigDecimal getApprovalAmt() { return getTotalDr(); }  
  
}   // MVoucherPending