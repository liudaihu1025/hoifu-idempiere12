package org.libero.process;

import java.util.List;
import java.util.stream.Collectors;

import org.compiere.model.MProcess;
import org.compiere.model.MProduct;
import org.compiere.process.DocAction;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ServerProcessCtl;
import org.compiere.process.SvrProcess;
import org.compiere.util.Trx;
import org.eevolution.model.MPPProductBOM;
import org.eevolution.model.MPPProductBOMLine;
import org.libero.model.MPPOrder;
import org.libero.model.MPPOrderBOM;
import org.libero.model.MPPOrderBOMLine;

@org.adempiere.base.annotation.Process
public class PPOrderBomSyncProcess extends SvrProcess {

	protected void prepare() {
		// 此流程没有参数
	}

	protected String doIt() throws Exception {
		List<Integer> orderIds = getRecord_IDs();
		if (orderIds == null || orderIds.isEmpty()) {
			int recordId = getRecord_ID();
			if (recordId > 0) {
				orderIds = List.of(recordId);
			}
		}

		if (orderIds.isEmpty()) {
			throw new IllegalArgumentException("请选择要更新BOM的工单");
		}

		validateOrderStatus(orderIds);
		int updatedCount = updateOrders(orderIds);

		return "成功更新 " + updatedCount + " 个工单的BOM信息";
	}

	private void validateOrderStatus(List<Integer> orderIds) throws Exception {
		List<String> invalidOrders = orderIds.stream().map(orderId -> {
			MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());
			if (order.get_ID() > 0) {
				String orderStatus = (String) order.get_Value("Orderstatus");
				String docStatus = order.getDocStatus();
				// 只允许Orderstatus='Ready'且DocStatus='DR'的工单更新BOM
				if (!"Ready".equals(orderStatus) || !DocAction.STATUS_Drafted.equals(docStatus)) {
					return order.getDocumentNo();
				}
			}
			return null;
		}).filter(docNo -> docNo != null).collect(Collectors.toList());

		if (!invalidOrders.isEmpty()) {
			throw new IllegalArgumentException(
					"您选择的工单中包含以下\"非待发布\"状态的条目：" + String.join(", ", invalidOrders) + "。只有状态为【待发布】的工单才允许更新BOM。");
		}
	}

    private int updateOrders(List<Integer> orderIds) {  
        int count = 0;  
          
        for (Integer orderId : orderIds) {  
            if (orderId == null || orderId <= 0) {  
                continue;  
            }  
              
            MPPOrder order = new MPPOrder(getCtx(), orderId, get_TrxName());  
              
            try {  
				// 执行BOM同步
				syncOrderBOMToProductBOM(order);
                count++;  
                  
				log.info("Successfully synced BOM for Order " + orderId);
                  
            } catch (Exception e) {  
				log.severe("Failed to sync BOM for Order " + orderId + ": " + e.getMessage());
                // 继续处理下一个工单  
            }  
        }  
          
        return count;  
	}

	/**
	 * 同步工单BOM到产品BOM
	 */
	private void syncOrderBOMToProductBOM(MPPOrder order) throws Exception {

		// 1. 获取工单BOM
		MPPOrderBOM orderBOM = order.getMPPOrderBOM();
		if (orderBOM == null) {
			throw new Exception("工单BOM不存在");
		}

		// 2. 取消勾选工单BOM的有效字段
		orderBOM.setIsActive(false);
		orderBOM.saveEx();

		// 3. 获取对应的产品BOM
		MPPProductBOM productBOM = MPPProductBOM.getDefault(order.getM_Product(), get_TrxName());
		if (productBOM == null) {
			throw new Exception("产品BOM不存在");
		}

//		// 4. 先申请变更，将产品BOM变为草稿状态
		requestProductBOMChange(productBOM);

		// 5. 复制产品BOM物料字段
		copyMainFields(orderBOM, productBOM);

		// 6. 删除产品BOM明细
		deleteProductBOMLines(productBOM);

		// 7. 复制工单BOM明细到产品BOM
		copyOrderBOMLinesToProductBOM(orderBOM, productBOM);

		// 8. 保存产品BOM
		productBOM.saveEx();


		// 9. 调用提交审核流程
		submitProductBOMForApproval(productBOM);
		
		// 10. 自动设置产品为已验证状态（不进行BOM结构检查）
	    MProduct product = new MProduct(getCtx(), productBOM.getM_Product_ID(), get_TrxName());  
	    if (!product.isVerified()) {  
	        product.setIsVerified(true);  
	        product.saveEx();  
	        log.info("自动设置产品 " + product.getName() + " 为已验证状态");  
		}
	}

	/**
	 * 复制主物料字段
	 */
	private void copyMainFields(MPPOrderBOM orderBOM, MPPProductBOM productBOM) {
		productBOM.setHelp(orderBOM.getHelp());
		productBOM.setName(orderBOM.getName());
		productBOM.setDescription(orderBOM.getDescription());
	}

	/**
	 * 删除产品BOM明细
	 */
	private void deleteProductBOMLines(MPPProductBOM productBOM) throws Exception {
		MPPProductBOMLine[] lines = productBOM.getLines();
		for (MPPProductBOMLine line : lines) {
			line.deleteEx(true);
		}
	}

	/**
	 * 复制工单BOM明细到产品BOM
	 */
	private void copyOrderBOMLinesToProductBOM(MPPOrderBOM orderBOM, MPPProductBOM productBOM) throws Exception {
		MPPOrderBOMLine[] orderLines = orderBOM.getLines();
		for (MPPOrderBOMLine orderLine : orderLines) {
			MPPProductBOMLine productLine = new MPPProductBOMLine(productBOM);

			// 使用完整的字段复制方法
			copyOrderBOMLineToProductBOMLine(orderLine, productLine);

			productLine.saveEx();
		}
	}

	/**
	 * 提交产品BOM审核
	 */
	private void submitProductBOMForApproval(MPPProductBOM productBOM) throws Exception {

		int AD_Process_ID = MProcess.getProcess_ID("PP_Product_BOM_SubmitForApproval", get_TrxName());
		if (AD_Process_ID <= 0) {
			throw new Exception("未找到BOM变更审批进程");
		}
		// 创建提交审核进程信息
		ProcessInfo pi = new ProcessInfo("PP_Product_BOM_SubmitForApproval", AD_Process_ID);
		pi.setAD_Client_ID(getAD_Client_ID());
		pi.setAD_User_ID(getAD_User_ID());
		pi.setRecord_ID(productBOM.getPP_Product_BOM_ID());
		pi.setTransactionName(get_TrxName());

		ServerProcessCtl.process(pi, Trx.get(get_TrxName(), false), false);

		if (pi.isError()) {
			throw new Exception("提交审核失败: " + pi.getSummary());
		}
	}


	/**  
	 * 申请产品BOM变更  
	 */  
	private void requestProductBOMChange(MPPProductBOM productBOM) throws Exception {  
	    // 通过进程名称获取进程ID，避免硬编码  
	    int AD_Process_ID = MProcess.getProcess_ID("PP_Product_BOM_RequestChange", get_TrxName());  
	    if (AD_Process_ID <= 0) {  
	        throw new Exception("未找到BOM变更审批进程");  
	    }  
	      
	    ProcessInfo pi = new ProcessInfo("BOM变更审批", AD_Process_ID);  
	    pi.setAD_Client_ID(getAD_Client_ID());  
	    pi.setAD_User_ID(getAD_User_ID());  
	    pi.setRecord_ID(productBOM.getPP_Product_BOM_ID());  
	    pi.setTransactionName(get_TrxName());  
	  
	    ServerProcessCtl.process(pi, Trx.get(get_TrxName(), false), false);  
	  
	    if (pi.isError()) {  
	        throw new Exception("申请变更失败: " + pi.getSummary());  
	    }  
	  
	    // 重新加载BOM以获取更新后的状态  
	    productBOM.load(get_TrxName());  
	}  

	/**
	 * 复制工单BOM明细到产品BOM明细
	 */
	private void copyOrderBOMLineToProductBOMLine(MPPOrderBOMLine orderLine, MPPProductBOMLine productLine) {
		// 复制基本字段
		productLine.setM_Product_ID(orderLine.getM_Product_ID());
		productLine.setQtyBOM(orderLine.getQtyBOM());
		productLine.setIsCritical(orderLine.isCritical());
		productLine.setQtyBatch(orderLine.getQtyBatch());

		productLine.setComponentType(orderLine.getComponentType());

		productLine.setScrap(orderLine.getScrap());
		productLine.setValidFrom(orderLine.getValidFrom());
		productLine.setValidTo(orderLine.getValidTo());
		productLine.setLeadTimeOffset(orderLine.getLeadTimeOffset());
		productLine.setAssay(orderLine.getAssay());
		productLine.setBackflushGroup(orderLine.getBackflushGroup());
		productLine.setC_UOM_ID(orderLine.getC_UOM_ID());
		productLine.setHelp(orderLine.getHelp());
		productLine.setDescription(orderLine.getDescription());
	}

}
