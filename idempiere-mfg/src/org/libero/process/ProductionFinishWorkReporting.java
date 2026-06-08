package org.libero.process;  
import java.sql.Timestamp;  
import java.util.List;   
import org.libero.model.MPPCostCollector;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;  
  
/**  
 * 生产报工完工Process  
 * 手动设置生产报工的完成时间  
 */  
@org.adempiere.base.annotation.Process  
public class ProductionFinishWorkReporting extends SvrProcess {  
      
    private Timestamp p_DateFinish = null;  
      
    protected void prepare() {  
        ProcessInfoParameter[] para = getParameter();  
        for (int i = 0; i < para.length; i++) {  
            String name = para[i].getParameterName();  
            if (name.equals("DateFinish")) {  
                p_DateFinish = (Timestamp) para[i].getParameter();  
            }  
        }  
    }  
      
    protected String doIt() throws Exception {  
        List<Integer> collectorIds = getRecord_IDs();  
        if (collectorIds == null || collectorIds.isEmpty()) {  
            int recordId = getRecord_ID();  
            if (recordId > 0) {  
                collectorIds = List.of(recordId);  
            }  
        }  
          
        if (collectorIds.isEmpty()) {  
            throw new IllegalArgumentException("请选择要完工的生产报工");  
        }  
          
        int updatedCount = updateCollectors(collectorIds);  
          
        return "成功设置完工时间 " + updatedCount + " 个生产报工";  
    }  
      
    private int updateCollectors(List<Integer> collectorIds) {  
        int count = 0;  
        for (Integer collectorId : collectorIds) {  
            MPPCostCollector collector = new MPPCostCollector(getCtx(), collectorId, get_TrxName());  
            if (collector.get_ID() > 0) {  
                // 使用传入的时间参数，如果没有则使用当前时间  
                Timestamp finishTime = p_DateFinish != null ? p_DateFinish : new Timestamp(System.currentTimeMillis());  
                collector.setDateFinish(finishTime);  
                Timestamp startDate = (Timestamp)collector.get_Value("DateStart");  
                collector.updateDurationRealFromDates(startDate, finishTime);
                collector.saveEx();  
                count++;  
            }  
        }  
        return count;  
    }  
}