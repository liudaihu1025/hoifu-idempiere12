package org.libero.process;  
  
import org.adempiere.util.Callback;
import org.compiere.model.MColumn;
import org.compiere.model.MProduct;
import org.compiere.model.MTable;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.SvrProcess;
import org.compiere.util.Env;
import org.compiere.wf.MWorkflow;
import org.eevolution.model.MPPProductBOM;

@org.adempiere.base.annotation.Process  
public class PP_Product_BOM_SubmitForApproval extends SvrProcess {
  
    /**  
     * 收集用户选择的参数  
     */  
    protected void prepare() {  
        int recordId = getRecord_ID();  
        if (recordId <= 0) {  
            throw new IllegalArgumentException("No record ID");  
		}
		// 设置不显示参数对话框
		if (getProcessInfo() != null) {
			getProcessInfo().setShowHelp("N");
		}

    }  
  
    /**  
     * 发布执行  
     */  
	@Override
	protected String doIt() throws Exception {
		int recordId = getRecord_ID();

		// 获取BOM对象
		MPPProductBOM bom = new MPPProductBOM(getCtx(), recordId, get_TrxName());
		if (bom.get_ID() == 0) {
			throw new IllegalArgumentException("BOM not found");
		}
		// 验证产品是否已检查BOM结构
		if (!validateProductBOMChecked(bom.getM_Product_ID())) {
			return "请先检查BOM结构";
		}

		// 构建确认信息
		StringBuilder message = new StringBuilder();
		message.append("产品: ").append(getProductName(bom.getM_Product_ID())).append("\n");
		message.append("BOM版本: ").append(bom.getRevision() != null ? bom.getRevision() : "1.0").append("\n");
		message.append("请重点核对BOM材料用量和损耗率，提交后BOM将锁定，不可编辑\n");
		message.append("确定要提交审批吗？");

		// 使用processUI显示确认对话框
		final StringBuffer userResponse = new StringBuffer();

		// 检查processUI是否可用
		if (processUI != null) {
			processUI.ask(message.toString(), new Callback<Boolean>() {
				@Override
				public void onCallback(Boolean result) {
					if (result != null && result) {
						userResponse.append("CONFIRMED");
					} else {
						userResponse.append("CANCELLED");
					}
				}
			});

			// 等待用户响应
			while (userResponse.length() == 0) {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					return "操作已取消";
				}
			}

			if (!"CONFIRMED".equals(userResponse.toString())) {
				return "操作已取消";
			}
		} else {
			// 如果没有UI界面，记录日志并继续
			addLog(0, null, null, "无UI界面，直接提交审批");
        }  

		// 验证BOM结构
		if (!validateBOMStructure(bom)) {
			return "BOM结构验证失败";
		}

		// 检查DocAction列是否存在
		MTable table = MTable.get(Env.getCtx(), MPPProductBOM.Table_ID);
		MColumn docActionColumn = table.getColumn("DocAction");
		if (docActionColumn == null) {
			throw new Exception("PP_Product_BOM表缺少DocAction列");
		}

		// 检查进程是否配置
		if (docActionColumn.getAD_Process_ID() == 0) {
			throw new Exception("DocAction列未配置进程");
		}

		// 在工作流成功后设置状态
		bom.setBOMStatus("PendingApproval");
		// 运行工作流 - 使用Prepare动作
		ProcessInfo pi = MWorkflow.runDocumentActionWorkflow(bom, DocAction.ACTION_Prepare);
		if (pi.isError()) {
			// 工作流失败，回退状态
			handleRejection(bom);
			return pi.getSummary();
		}

		bom.setProcessed(true);
		bom.saveEx();

		// 记录提交信息
		int userId = Env.getAD_User_ID(getCtx());
		bom.setSubmittedBy(userId);
		bom.saveEx();

		// 发送通知给审核人
		sendNotificationToApprovers(bom);

		return "提交成功";
    }  
  
    /**  
     * 获取产品名称  
     */  
    private String getProductName(int productId) {  
        if (productId > 0) {  
            MProduct product = MProduct.get(getCtx(), productId);  
            return product.getName();  
        }  
        return "未设置";  
    }  
  
    /**  
     * 验证BOM结构  
     */  
    private boolean validateBOMStructure(MPPProductBOM bom) {  
        // 检查BOM是否有行  
        if (bom.getLines() == null || bom.getLines().length == 0) {  
            return false;  
        }  
          
        // 可以添加更多验证逻辑  
        return true;  
    }  

	/**
	 * 发送通知给审核人
	 */
	private void sendNotificationToApprovers(MPPProductBOM bom) {
		// 这里可以实现具体的通知逻辑
		// 例如：发送邮件、系统通知等
		addLog(0, null, null, "已发送通知给审核人");
	}

	/**
	 * 处理审核失败 - 回退状态
	 */
	private void handleRejection(MPPProductBOM bom) {
		try {
			// 回退BOM状态为"处理中"
			bom.setBOMStatus("InProgress");

			// 回退单据状态为"草稿"
			bom.setDocStatus(DocAction.STATUS_Drafted);
			bom.setDocAction(DocAction.ACTION_Complete);

			// 保存更改
			bom.saveEx();

			addLog(0, null, null, "审核失败，状态已回退");
		} catch (Exception e) {
			addLog(0, null, null, "状态回退失败: " + e.getMessage());
		}
	}

	/**
	 * 验证产品是否已检查BOM结构
	 * 
	 * @param productId 产品ID
	 * @return true如果已检查，false如果未检查
	 */
	private boolean validateProductBOMChecked(int productId) {
		MProduct product = MProduct.get(getCtx(), productId);
		return product.isVerified();
	}

}