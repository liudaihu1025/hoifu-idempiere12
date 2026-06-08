package org.libero.process;

import java.util.logging.Level;

import org.compiere.process.ProcessInfoParameter;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.wf.MWFNode;
import org.compiere.wf.MWorkflow;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.libero.model.MPP_Engineering_Change_Notice;

@org.adempiere.base.annotation.Process
public class ECN_WorkflowVerify extends SvrProcess {

	private int p_PP_Engineering_Change_Notice_ID = 0;

	@Override
	protected void prepare() {
		for (ProcessInfoParameter para : getParameter()) {
			String name = para.getParameterName();
			if (para.getParameter() == null)
				;
			else if (name.equals("PP_Engineering_Change_Notice_ID"))
				p_PP_Engineering_Change_Notice_ID = para.getParameterAsInt();
			else
				log.log(Level.SEVERE, "prepare - Unknown Parameter: " + name);
		}
	}

	@Override
	protected String doIt() throws Exception {
		if (p_PP_Engineering_Change_Notice_ID == 0) {
			p_PP_Engineering_Change_Notice_ID = getRecord_ID();
		}

		MPP_Engineering_Change_Notice ecn = new MPP_Engineering_Change_Notice(getCtx(),
				p_PP_Engineering_Change_Notice_ID, get_TrxName());

		return verifyWorkflowAndBOM(ecn);
	}

	 /**  
     * 验证工艺路线和BOM - 只有两者都通过才设置验证标志  
     */  
    private String verifyWorkflowAndBOM(MPP_Engineering_Change_Notice ecn) {  
        StringBuilder errors = new StringBuilder();  
  
        // 验证工艺路线  
        String workflowResult = verifyWorkflow(ecn);  
        if (!"OK".equals(workflowResult)) {  
            errors.append("工艺路线验证失败: ").append(workflowResult);  
        }  
  
        // 验证BOM  
        String bomResult = verifyBOM(ecn);  
        if (!"OK".equals(bomResult)) {  
            if (errors.length() > 0) errors.append("; ");  
            errors.append("BOM验证失败: ").append(bomResult);  
        }  
  
        // 验证结果  
        boolean valid = errors.length() == 0;  
  
        if (valid) {  
            // 验证通过，设置ECN的工艺路线已验证标志  
            ecn.set_ValueOfColumn("IsWorkflowVerified", "Y");  
            ecn.saveEx();  
            addLog(0, null, null, "ECN工艺路线和BOM验证成功");  
            return "ECN工艺路线和BOM验证成功";  
        } else {  
            addLog(0, null, null, "验证失败: " + errors.toString());  
            return "验证失败: " + errors.toString();  
        }  
    }  
  
    /**  
     * 验证工艺路线 - 获取ECN变更单下的工艺路线  
     */  
    private String verifyWorkflow(MPP_Engineering_Change_Notice ecn) {  
        StringBuilder errors = new StringBuilder();  
  
        if (ecn.getPP_Order_ID() <= 0) {  
            return "ECN未关联工单";  
        }  
  
        // 获取ECN变更单下的工艺路线  
        int workflowId = DB.getSQLValue(get_TrxName(),  
                "SELECT AD_Workflow_ID FROM AD_Workflow WHERE pp_engineering_change_notice_id = ?", ecn.get_ID());  
  
        if (workflowId <= 0) {  
            return "ECN未关联工艺路线";  
        }  
  
        MWorkflow workflow = new MWorkflow(getCtx(), workflowId, get_TrxName());  
  
        // 验证工艺路线 - 参考MWorkflow.validate()逻辑  
        if (workflow.getAD_WF_Node_ID() == 0)  
            errors.append(" - 无起始节点");  
  
        if (workflow.getWorkflowType().equals(MWorkflow.WORKFLOWTYPE_DocumentValue)  
                && (workflow.getDocValueLogic() == null || workflow.getDocValueLogic().length() == 0))  
            errors.append(" - 无文档值逻辑");  
  
        // 验证所有节点  
        MWFNode[] nodes = workflow.getNodes(false, getAD_Client_ID());  
        if (nodes.length == 0) {  
            errors.append(" - 无工作流节点");  
        }  
  
        for (MWFNode node : nodes) {  
            if (node.getName() == null || node.getName().trim().length() == 0) {  
                errors.append(" - 节点无名称: " + node.getAD_WF_Node_ID());  
            }  
        }  
  
        // 验证结果  
        boolean valid = errors.length() == 0;  
  
        if (valid) {  
            // 验证通过，设置工艺路线本身为有效  
            workflow.setIsValid(true);  
            workflow.saveEx();  
            return "OK";  
        } else {  
            return errors.toString();  
        }  
    }  
  
    /**  
     * 验证BOM - 参考BOMVerify.validateProduct()方法  
     */  
    private String verifyBOM(MPP_Engineering_Change_Notice ecn) {  
        StringBuilder errors = new StringBuilder();  
  
        if (ecn.getPP_Order_ID() <= 0) {  
            return "ECN未关联工单";  
        }  
  
        // 获取ECN关联的BOM  
        int bomId = DB.getSQLValue(get_TrxName(),  
                "SELECT PP_Product_BOM_ID FROM PP_Product_BOM WHERE pp_engineering_change_notice_id = ?", ecn.get_ID());  
  
        if (bomId <= 0) {  
            return "ECN未关联BOM";  
        }  
  
        MPPProductBOM bom = new MPPProductBOM(getCtx(), bomId, get_TrxName());  
  
        // 验证BOM结构 - 参考BOMVerify验证逻辑 [1](#211-0)   
        try {  
            // 检查BOM行  
            MPPProductBOMLine[] bomLines = bom.getLines();  
            if (bomLines.length == 0) {  
                errors.append(" - BOM无明细行");  
            }  
  
            // 验证每个BOM行  
            for (MPPProductBOMLine line : bomLines) {  
                if (!line.isActive()) {  
                    continue;  
                }  
  
                // 检查产品  
                if (line.getM_Product_ID() <= 0) {  
                    errors.append(" - BOM行缺少产品: ").append(line.getLine());  
                }  
                  
//                // 检查数量  
//                if (line.getQty().signum() <= 0) {  
//                    errors.append(" - BOM行数量无效: ").append(line.getLine());  
//                }  
            }  
        } catch (Exception e) {  
            errors.append(" - BOM验证异常: ").append(e.getMessage());  
        }  
          
        return errors.length() == 0 ? "OK" : errors.toString();  
    }  
}
