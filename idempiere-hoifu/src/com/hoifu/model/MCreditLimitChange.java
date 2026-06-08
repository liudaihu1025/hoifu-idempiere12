package com.hoifu.model;  
  
import java.io.File;  
import java.math.BigDecimal;  
import java.sql.ResultSet;  
import java.util.Properties;  
import java.util.logging.Level;  
  
import org.adempiere.model.DocActionDelegate;  
import org.compiere.model.MBPartner;  
import org.compiere.process.DocAction;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import org.compiere.util.Msg;  
import org.compiere.util.Util;  
  
public class MCreditLimitChange extends X_C_CreditLimitChange implements DocAction {  
  
    private static final long serialVersionUID = 1L;  
  
    private DocActionDelegate<MCreditLimitChange> docActionDelegate;  
  
    // ── 构造函数 ──────────────────────────────────────────────  
  
    public MCreditLimitChange(Properties ctx, String C_CreditLimitChange_UU, String trxName) {  
        super(ctx, C_CreditLimitChange_UU, trxName);  
        if (Util.isEmpty(C_CreditLimitChange_UU))  
            setInitialDefaults();  
        init();  
    }  
  
    public MCreditLimitChange(Properties ctx, int C_CreditLimitChange_ID, String trxName) {  
        super(ctx, C_CreditLimitChange_ID, trxName);  
        if (C_CreditLimitChange_ID == 0)  
            setInitialDefaults();  
        init();  
    }  
  
    public MCreditLimitChange(Properties ctx, ResultSet rs, String trxName) {  
        super(ctx, rs, trxName);  
        init();  
    }  
  
    private void setInitialDefaults() {  
        setProcessed(false);  
        setCreditLimitChange(Env.ZERO);  
    }  
  
    private void init() {  
        docActionDelegate = new DocActionDelegate<>(this);  
        // 只注册 Complete 动作的业务逻辑，其余由 delegate 默认处理  
        docActionDelegate.setActionCallable(DocAction.ACTION_Complete, () -> doComplete());  
    }  
  
    // ── beforeSave：每次保存前校验额度不能变为负数 ───────────  
  
    @Override  
    protected boolean beforeSave(boolean newRecord) {  
       
        return true;  
    }  
  
    // ── doComplete：单据完成时更新 C_BPartner ─────────────────  
  
    private String doComplete() {  
        if (getC_BPartner_ID() == 0)  
            return "未选择客户";  
		MBPartner bp = new MBPartner(getCtx(), getC_BPartner_ID(), get_TrxName());
		// 校验信用变更额度：变更后 SO_CreditLimit 不能 < 0
		BigDecimal creditChange = getCreditLimitChange();
		if (creditChange != null && creditChange.signum() != 0) {
			BigDecimal newCreditLimit = bp.getSO_CreditLimit().add(creditChange);
			if (newCreditLimit.signum() < 0) {

				return "信用额度变更后将小于0（当前=" + bp.getSO_CreditLimit() + "，变更=" + creditChange + "，变更后=" + newCreditLimit
						+ "）";
			}
		}

		// 校验临时信用变更额度：变更后 TempCreditLimit 不能 < 0
		BigDecimal tempChange = getTempCreditLimitChange();
		if (tempChange != null && tempChange.signum() != 0) {
			BigDecimal currentTemp = bp.getTempCreditLimit(); // 需已在 MBPartner 中添加此方法
			BigDecimal newTempLimit = currentTemp.add(tempChange);
			if (newTempLimit.signum() < 0) {
				return "临时信用额度变更后将小于0（当前=" + currentTemp + "，变更=" + tempChange + "，变更后=" + newTempLimit + "）";
			}
		}

        DB.getDatabase().forUpdate(bp, 0); // 行锁，防止并发修改  
  
        // 1. 记录变更前快照（用于审计）  
        setSO_CreditLimit_Old(bp.getSO_CreditLimit());  
        setTempCreditLimit_Old(bp.getTempCreditLimit());  
        
        // 2. 更新永久信用额度：SO_CreditLimit += CreditLimitChange  
        //BigDecimal creditChange = getCreditLimitChange();  
        if (creditChange != null && creditChange.signum() != 0) {  
            bp.setSO_CreditLimit(bp.getSO_CreditLimit().add(creditChange));  
        }  
  
        // 3. 更新临时信用额度及有效期  
        //BigDecimal tempChange = getTempCreditLimitChange();  
        if (tempChange != null && tempChange.signum() != 0) {  
            bp.setTempCreditLimit(bp.getTempCreditLimit().add(tempChange));  
            bp.setTempCreditLimitValidFrom(getTempCreditLimitValidFrom());  
            bp.setTempCreditLimitValidTo(getTempCreditLimitValidTo());  
        }  
  
        // 4. 重新计算信用状态（CreditOK / CreditWatch / CreditHold）  
        bp.setSOCreditStatus();  
  
        // 5. 保存业务伙伴  
        if (!bp.save(get_TrxName())) {  
            return "更新客户信用额度失败（C_BPartner_ID=" + getC_BPartner_ID() + "）";  
        }  
  
        if (log.isLoggable(Level.INFO))  
            log.info("Credit limit updated for BPartner=" + bp.getValue()  
                + ", new SO_CreditLimit=" + bp.getSO_CreditLimit());  
  
        return null; // null 表示成功，非 null 字符串表示错误消息  
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
    @Override public boolean reActivateIt()      { return false; } // 暂不支持重新激活  
    @Override public File    createPDF()         { return docActionDelegate.createPDF(); }  
    @Override public String  getProcessMsg()     { return docActionDelegate.getProcessMsg(); }  
    @Override public int     getC_Currency_ID()  { return docActionDelegate.getC_Currency_ID(); }  
    @Override public String  getDocAction()      { return docActionDelegate.getDocAction(); }  
    @Override public void    setDocStatus(String s) { docActionDelegate.setDocStatus(s); }  
    @Override public String  getDocStatus()      { return docActionDelegate.getDocStatus(); }  
  
    @Override  
    public String getSummary()      { return getDocumentNo(); }  
    @Override  
    public String getDocumentNo()   { return get_ValueAsString("DocumentNo"); }  
    @Override  
    public String getDocumentInfo() { return getDocumentNo(); }  
    @Override  
    public int getDoc_User_ID()     { return getSalesRep_ID(); }  
    @Override  
    public BigDecimal getApprovalAmt() { return getCreditLimitChange(); }  
}