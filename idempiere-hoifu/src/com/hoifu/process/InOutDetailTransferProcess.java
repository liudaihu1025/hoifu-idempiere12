package com.hoifu.process;  
  
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MInOut;
import org.compiere.model.MInOutLine;
import org.compiere.model.PO;
import org.compiere.model.Query;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Trx;
  
/**  
 * 转下月Process  
 */  
@org.adempiere.base.annotation.Process
public class InOutDetailTransferProcess extends SvrProcess {  

	private String reconciliationMonth;  
	
    @Override  
    protected void prepare() {  
		// 准备Process参数（如果需要）
		ProcessInfoParameter[] para = getParameter();
		for (int i = 0; i < para.length; i++) {
			String name = para[i].getParameterName();
			if (name.equals("reconciliationmonth"))
				reconciliationMonth = para[i].getParameterAsString();
		}
    }  
      
    @Override  
    protected String doIt() throws Exception {  
    	if (Objects.isNull(reconciliationMonth) || reconciliationMonth.isEmpty()) {
    		throw new AdempiereException("对账月不能为空！");
    	}
    	
    	if (!validateYearMonth(reconciliationMonth)) {
    		throw new AdempiereException("对账月格式不正确！请按照【202108】类似格式进行填写");
    	}
          
        // 获取选中的记录ID  
        int pInstanceID = getAD_PInstance_ID();  
        int[] selectedIds = DB.getIDsEx(get_TrxName(), 
        	    "SELECT t_selection_id FROM T_Selection WHERE AD_PInstance_ID = ?", 
        	    pInstanceID); 
          
        List<Integer> idList = Arrays.stream(selectedIds)  
                                    .boxed()  
                                    .collect(Collectors.toList()); 
        
        if (idList.isEmpty()) {  
            return "没有选择要操作的记录";  
        }  
          
        return executeUpdateToNextMonth(idList);  
    }  
      
    /**  
     * 执行转下月操作  
     */  
    private String executeUpdateToNextMonth(List<Integer> selectedIds) {  
        Trx trx = Trx.get(Trx.createTrxName("ReconciliationUpdate"), true);  
        try {  
            long updatedCount = selectedIds.stream()  
                .map(inOutId -> updateInOutLineToNextMonth(inOutId, trx))  
                .filter(Optional::isPresent)  
                .count();  
  
            trx.commit();  
            return String.format("已选择%d笔，已更新%d笔", selectedIds.size(), updatedCount);  
        } catch (Exception e) {  
            trx.rollback();  
            throw e;  
        } finally {  
            trx.close();  
        }  
    }  
      
    /**  
     * 更新单个收发行到下月  
     */  
    private Optional<MInOutLine> updateInOutLineToNextMonth(Integer inOutId, Trx trx) {  
        try {  
            MInOutLine inOutLine = new MInOutLine(Env.getCtx(), inOutId, trx.getTrxName());  
            MInOut inOut = new MInOut(Env.getCtx(), inOutLine.get_ValueAsInt("M_Inout_ID"), trx.getTrxName());  
  
			if ("CO".equals(inOutLine.get_ValueAsString("ReconciliationStatus"))) {
                log.warning(String.format("不能对[%s]记录[%s]操作",  
                    inOut.get_ValueAsString("DocumentNo"),   
                    inOutLine.get_ValueAsString("ReconciliationStatus")));  
                return Optional.empty();  
            }  
            
            // 删除对账明细记录  
			// deleteReconciliationLine(inOutId,
			// inOutLine.get_ValueAsString("ReconciliationMonth"), trx);
  
			// 计算并设置下月信息
//            calculateAndSetNextMonth(inOutLine, inOut);
            // 根据参数设置对账月
			inOutLine.set_ValueOfColumn("ReconciliationMonth", reconciliationMonth);
			inOutLine.set_ValueOfColumn("ReconciliationStatus", "NX");
  
            return inOutLine.save() ? Optional.of(inOutLine) : Optional.empty();  
        } catch (Exception e) {  
            log.warning("更新记录失败: " + e.getMessage());  
            return Optional.empty();  
        }  
    }  
    
    /**  
     * 删除对账明细记录  
     */  
    private void deleteReconciliationLine(Integer mInOutLineId, String reconciliationMonth, Trx trx) {  
    	// 参数验证：检查必要参数是否为空
    	if (mInOutLineId == null || reconciliationMonth == null || reconciliationMonth.isEmpty()) {  
            return;  
        }  
        
    	// 转换月份格式从 yyyy-MM 到 yyyyMM，用于数据库查询 
        String reconPeriod = reconciliationMonth.replace("-", "");  
          
        //构建查询条件：查找指定收发行和对账期间的对账明细记录   
        String whereClause = "M_InOutLine_ID = ? AND ReconPeroid = ?";  
        List<PO> records = new Query(Env.getCtx(), "C_ReconciliationLine", whereClause, trx.getTrxName())  
                .setParameters(mInOutLineId, reconPeriod)  
                .list();  
        
        // 遍历查询结果并删除记录
        int deletedCount = 0;  
        for (PO record : records) {  
            if (record.delete(true)) {  // 使用 PO 的 delete 方法    
                deletedCount++;  
            }  
        }  
        
        // 记录删除结果日志 
        if (deletedCount > 0) {  
            log.info(String.format("通过PO对象删除了%d条对账明细记录", deletedCount));  
        }  
    }
      
    /**  
     * 计算并设置下月信息  
     */  
    private void calculateAndSetNextMonth(MInOutLine inOutLine, MInOut inOut) {  
        String reconciliationMonth = inOutLine.get_ValueAsString("ReconciliationMonth");  
        String movementDate = inOut.get_ValueAsString("MovementDate");  
  
        String currentMonth = Optional.ofNullable(reconciliationMonth)  
            .filter(month -> !month.isEmpty())  
            .orElseGet(() -> Optional.ofNullable(movementDate)  
                .filter(date -> date.length() > 7)  
                .map(date -> date.substring(0, 7))  
                .orElse(null));  
  
        if (currentMonth != null) {  
            String nextMonth = calculateNextMonth(currentMonth);  
            inOutLine.set_ValueOfColumn("ReconciliationMonth", nextMonth);  
            inOutLine.set_ValueOfColumn("ReconciliationStatus", "转下月");  
        }  
    }  
      
    /**  
     * 计算下一个月份  
     */  
    private String calculateNextMonth(String currentMonth) {  
        try {  
            java.time.YearMonth yearMonth = java.time.YearMonth.parse(currentMonth,   
                java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));  
            return yearMonth.plusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));  
        } catch (Exception e) {  
            return currentMonth;  
        }  
    }  

	public static boolean validateYearMonth(String input) {
		// 1. 基础格式校验：必须是6位数字
		if (input == null || !input.matches("^\\d{6}$")) {
			return false;
		}

		// 2. 提取年份和月份数字
		int year = Integer.parseInt(input.substring(0, 4));
		int month = Integer.parseInt(input.substring(4, 6));

		// 3. 月份范围校验：必须是01-12
		if (month < 1 || month > 12) {
			return false;
		}

		// 4. 使用Year进一步验证
		if (year < 1900 || year > 2500) return false;

		return true;
	}
}
