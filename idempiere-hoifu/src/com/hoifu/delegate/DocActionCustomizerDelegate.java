package com.hoifu.delegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.adempiere.base.annotation.EventTopicDelegate;
import org.adempiere.base.event.EventHelper;
import org.adempiere.base.event.annotations.EventDelegate;
import org.adempiere.base.event.annotations.doc.DocAction;
import org.compiere.model.MDocType;
import org.compiere.process.DocActionEventData;
import org.compiere.process.DocumentEngine;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.osgi.service.event.Event;

/**
 * 通用单据动作自定义委托类
 * 基于 IEventTopics.DOCACTION 事件，在 DocumentEngine.getValidActions() 最后阶段
 * 按表名和单据状态过滤 options[]，控制单据动作弹窗显示的动作按钮。
 *
 */
@EventTopicDelegate
public class DocActionCustomizerDelegate extends EventDelegate {

    private static final CLogger log = CLogger.getCLogger(DocActionCustomizerDelegate.class);

    // 单据类型名称常量
    private static final String DOCTYPE_NAME_MATERIAL_ISSUE = "领用单";

    private static final String DOCTYPE_NAME_MATERIAL_TRANSFER = "调拨单";

	/** 表名 → 处理方法映射，新增单据类型需在此注册一行 */
    private static final Map<String, Consumer<DocActionEventData>> DISPATCH = Map.ofEntries(
            Map.entry("PP_Order",                  DocActionCustomizerDelegate::handlePPOrder),
            Map.entry("C_Order",                   DocActionCustomizerDelegate::handleOrder),
            Map.entry("M_InOut",                   DocActionCustomizerDelegate::handleInOut),
            Map.entry("M_Requisition",             DocActionCustomizerDelegate::handleRequisition),
            Map.entry("pp_material_requisition",   DocActionCustomizerDelegate::handlePPMaterialRequisition),
            Map.entry("PP_Cost_Collector",         DocActionCustomizerDelegate::handlePPCostCollector),
            Map.entry("M_Movement",                DocActionCustomizerDelegate::handleMovement),
            Map.entry("M_Inventory",               DocActionCustomizerDelegate::handleInventory),
            Map.entry("M_Logistics",               DocActionCustomizerDelegate::handleLogistics),
            Map.entry("PP_Order_Repair_Request",   DocActionCustomizerDelegate::handlePPOrderRepairRequest),
            // 财务通用单据
            Map.entry("C_Invoice",                 DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("C_Payment",                 DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("GL_Journal",                DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("GL_JournalBatch",           DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("C_Reconciliation",          DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("C_PaymentRequest",          DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("S_TimeExpense",             DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("C_BankStatement",           DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("A_Asset_Addition",          DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("A_Depreciation_Entry",      DocActionCustomizerDelegate::handleFinanceDoc),
            Map.entry("A_Asset_Disposed",          DocActionCustomizerDelegate::handleFinanceDoc)
    );

    public DocActionCustomizerDelegate(Event event) {
        super(event);
    }

    @DocAction
    public void onDocAction() {
        DocActionEventData data = EventHelper.getEventData(event);
        if (data == null || data.po == null) {
            return;
        }

        String tableName = data.po.get_TableName();
        Consumer<DocActionEventData> handler = DISPATCH.get(tableName);
        if (handler != null) {
            handler.accept(data);
        }
    }

	/**
	 * 财务通用单据（应付单/应收单/付款单/收款单/手工凭证） 草稿/处理中/无效：只保留 完成（处理中额外保留 解锁）
	 * 已完成/已反冲：维持系统默认（红字更正+借贷更正+重新激活 / 无操作），不做改动 其余未列出状态：清空
	 */
	private static void handleFinanceDoc(DocActionEventData data) {

		String docStatus = data.docStatus;

		if (DocumentEngine.STATUS_Drafted.equals(docStatus) || DocumentEngine.STATUS_Invalid.equals(docStatus)) {
			keepOnly(data, DocumentEngine.ACTION_Complete);
			return;
		}

		if (DocumentEngine.STATUS_InProgress.equals(docStatus)) {
			keepOnly(data, DocumentEngine.ACTION_Complete, DocumentEngine.ACTION_Unlock);
			addIfMissing(data, DocumentEngine.ACTION_Unlock);
			return;
		}

		if (DocumentEngine.STATUS_Completed.equals(docStatus) || DocumentEngine.STATUS_Reversed.equals(docStatus)) {
			// 已完成 / 已反冲
			// 排除已关闭
			removeAction(data, DocumentEngine.ACTION_Close);
			return;
		}

		// 其余未列出状态（Approved/NotApproved/WaitingPayment/WaitingConfirmation 等）：清空
		clearOptions(data);
    }

    /**
     * 生产工单 (PP_Order) 单据动作过滤
     * 已完成 → 作废 + 重新激活
     * 已作废 → 无
     * 其余未列出状态 → 无
     */
    private static void handlePPOrder(DocActionEventData data) {
        String docStatus = data.docStatus;

        if ("CO".equals(docStatus)) {
            // 已完成：保留作废 + 重新激活
            keepOnly(data, DocumentEngine.ACTION_Void, DocumentEngine.ACTION_ReActivate);
        } else if ("VO".equals(docStatus)) {
            // 已作废：清空
            clearOptions(data);
        } else {
            // 其余未列出状态：清空
            clearOptions(data);
        }
    }

    /**
     * 销售订单/采购订单 (C_Order) 单据动作过滤
     * 草稿 → 只保留完成
     * 处理中 → 完成 + 解锁 + 作废（去掉准备）
     * 已完成 → 作废 + 重新激活（去掉关闭）
     * 未审批（仅采购订单）→ 只保留准备
     * 已作废 → 清空
     * 其余未列出状态 → 清空
     */
    private static void handleOrder(DocActionEventData data) {
        String docStatus = data.docStatus;
        boolean isSalesOrder = "Y".equals(data.isSOTrx);

        if ("DR".equals(docStatus)) {
            // 草稿：只保留完成
            keepOnly(data, DocumentEngine.ACTION_Complete);
        } else if ("IP".equals(docStatus)) {
            // 处理中：保留完成 + 解锁 + 作废（去掉准备）
            keepOnly(data, DocumentEngine.ACTION_Complete, DocumentEngine.ACTION_Unlock, DocumentEngine.ACTION_Void);
        } else if ("CO".equals(docStatus)) {
            // 已完成：保留作废 + 重新激活（去掉关闭）
            keepOnly(data, DocumentEngine.ACTION_Void, DocumentEngine.ACTION_ReActivate);
            // 若 ReActivate 因 canReactivateThisDocType 未开启而缺失，强制补上
            addIfMissing(data, DocumentEngine.ACTION_ReActivate);
        } else if (!isSalesOrder && "NA".equals(docStatus)) {
            // 未审批（仅采购订单）：只保留准备
            keepOnly(data, DocumentEngine.ACTION_Prepare);
        } else if ("VO".equals(docStatus)) {
            // 已作废：清空
            clearOptions(data);
        } else {
            // 其余未列出状态：清空
            clearOptions(data);
        }
    }

    /**
     * 收货单/发货单 (M_InOut) 单据动作过滤
     * 草稿/处理中/无效/已审批 → 只保留完成
     * 已完成 → 去掉关闭，保留其它（红字更正/借贷更正等）
     * 其余状态 → 保持默认
     */
    private static void handleInOut(DocActionEventData data) {
        String docStatus = data.docStatus;

        if ("DR".equals(docStatus) || "IP".equals(docStatus)
                || "IN".equals(docStatus) || "AP".equals(docStatus)) {
            // 草稿/处理中/无效/已审批：只保留完成
            keepOnly(data, DocumentEngine.ACTION_Complete);
        } else if ("CO".equals(docStatus)) {
            // 已完成：去掉关闭，保留其它
            removeAction(data, DocumentEngine.ACTION_Close);
        }
        // 其余状态保持默认行为
    }

    /**
     * 申购单 (M_Requisition) 单据动作过滤
     * 草稿/无效 → 只保留完成
     * 处理中 → 完成 + 作废（去掉准备）
     * 已完成 → 只保留作废（去掉关闭）
     * 未审批 → 只保留准备
     * 已作废 → 清空
     * 其余未列出状态 → 清空
     */
    private static void handleRequisition(DocActionEventData data) {
        String docStatus = data.docStatus;

        if ("DR".equals(docStatus) || "IN".equals(docStatus)) {
            // 草稿/无效：只保留完成
            keepOnly(data, DocumentEngine.ACTION_Complete);
        } else if ("IP".equals(docStatus)) {
            // 处理中：保留完成 + 作废（去掉准备）
            keepOnly(data, DocumentEngine.ACTION_Complete, DocumentEngine.ACTION_Void);
        } else if ("CO".equals(docStatus)) {
            // 已完成：只保留作废（去掉关闭）
            keepOnly(data, DocumentEngine.ACTION_Void);
			addIfMissing(data, DocumentEngine.ACTION_Void);
        } else if ("NA".equals(docStatus)) {
            // 未审批：只保留准备
            keepOnly(data, DocumentEngine.ACTION_Prepare);
        } else if ("VO".equals(docStatus)) {
            // 已作废：清空
            clearOptions(data);
        } else {
            // 其余未列出状态：清空
            clearOptions(data);
        }
    }

    /**
     * 领退料 (PP_Material_Requisition) 单据动作过滤
     * 草稿 → 完成
     * 处理中 → 完成 + 作废
     * 已完成 → 作废
     * 已审批 → 完成
     * 已作废 → 无
     * 其余未列出状态 → 无
     */
    private static void handlePPMaterialRequisition(DocActionEventData data) {
        String docStatus = data.docStatus;

        if ("DR".equals(docStatus) || "AP".equals(docStatus)) {
            // 草稿/已审批：只保留完成
            keepOnly(data, DocumentEngine.ACTION_Complete);
        } else if ("IP".equals(docStatus)) {
            // 处理中：保留完成 + 作废
            keepOnly(data, DocumentEngine.ACTION_Complete, DocumentEngine.ACTION_Void);
        } else if ("CO".equals(docStatus)) {
            // 已完成：只保留作废
            keepOnly(data, DocumentEngine.ACTION_Void);
        } else {
            // 已作废及其余未列出状态：清空
            clearOptions(data);
        }
    }

    /**
     * 生产入库/生产报工 (PP_Cost_Collector) 单据动作过滤
     * 草稿/处理中/无效 → 完成
     * 已完成 → 红字更正
     * 已反冲 → 无
     * 其余未列出状态 → 无
     */
    private static void handlePPCostCollector(DocActionEventData data) {
        String docStatus = data.docStatus;

        if ("DR".equals(docStatus) || "IP".equals(docStatus) || "IN".equals(docStatus)) {
            // 草稿/处理中/无效：只保留完成
            keepOnly(data, DocumentEngine.ACTION_Complete);
        } else if ("CO".equals(docStatus)) {
            // 已完成：只保留红字更正
            keepOnly(data, DocumentEngine.ACTION_Reverse_Correct);
        } else {
            // 已反冲及其余未列出状态：清空
            clearOptions(data);
        }
    }

	/**
	 * 调拨单 (M_Movement) 单据动作过滤 草稿/处理中 → 完成 已完成 → 红字更正、借贷更正 已反冲及其余未列出状态 → 无
	 */
	private static void handleMovement(DocActionEventData data) {
		String docStatus = data.docStatus;

		if ("DR".equals(docStatus) || "IP".equals(docStatus)) {
			keepOnly(data, DocumentEngine.ACTION_Complete);
		} else if ("CO".equals(docStatus)) {
			keepOnly(data, DocumentEngine.ACTION_Reverse_Correct, DocumentEngine.ACTION_Reverse_Accrual);
		} else {
			clearOptions(data);
		}
	}

	/**
	 * M_Inventory 单据动作过滤：领用单 (DocSubTypeInv=IU) 与 盘点单 (DocSubTypeInv=PI) 通过
	 * C_DocType.DocSubTypeInv 区分，而不是表名（两者是同一张表）
	 * 
	 * 领用单 (IU)： 草稿/处理中/无效/已审批 → 完成 已完成 → 红字更正、借贷更正 未审批 → 准备 已反冲 → 无 其余未列出状态 → 无
	 * 
	 * 盘点单 (PI)： 草稿/无效 → 完成 处理中 → 完成 + 作废 已作废及其余未列出状态（含已完成）→ 无
	 */
	private static void handleInventory(DocActionEventData data) {
		String docStatus = data.docStatus;
		String docSubTypeInv = getDocSubTypeInv(data);

		if (MDocType.DOCSUBTYPEINV_InternalUseInventory.equals(docSubTypeInv)) {
			// 领用单
			if ("DR".equals(docStatus) || "IP".equals(docStatus) || "IN".equals(docStatus) || "AP".equals(docStatus)) {
				keepOnly(data, DocumentEngine.ACTION_Complete);
			} else if ("CO".equals(docStatus)) {
				keepOnly(data, DocumentEngine.ACTION_Reverse_Correct, DocumentEngine.ACTION_Reverse_Accrual);
			} else if ("NA".equals(docStatus)) {
				keepOnly(data, DocumentEngine.ACTION_Prepare);
			} else {
				clearOptions(data);
			}
		} else if (MDocType.DOCSUBTYPEINV_PhysicalInventory.equals(docSubTypeInv)) {
			// 盘点单
			if ("DR".equals(docStatus) || "IN".equals(docStatus)) {
				keepOnly(data, DocumentEngine.ACTION_Complete);
			} else if ("IP".equals(docStatus)) {
				keepOnly(data, DocumentEngine.ACTION_Complete, DocumentEngine.ACTION_Void);
			} else {
				clearOptions(data);
			}
		} else {
			// 未识别的 DocSubTypeInv（如 Cost Adjustment），保持默认行为，记录日志
			log.warning(
					"Unrecognized DocSubTypeInv for M_Inventory: " + docSubTypeInv + ", record_id=" + data.po.get_ID());
		}
	}

	/**
	 * 物流单 (M_Logistics) 单据动作过滤
	 * 草稿 → 完成
	 * 处理中 → 完成 + 解锁 + 作废
	 * 已完成 → 重新激活 + 作废
	 * 已作废 → 无
	 * 其余未列出状态 → 无
	 */
	private static void handleLogistics(DocActionEventData data) {
		String docStatus = data.docStatus;

		if ("DR".equals(docStatus)) {
			keepOnly(data, DocumentEngine.ACTION_Complete);
		} else if ("IP".equals(docStatus)) {

			keepOnly(data, DocumentEngine.ACTION_Complete, DocumentEngine.ACTION_Unlock, DocumentEngine.ACTION_Void);
			addIfMissing(data, DocumentEngine.ACTION_Void);
		} else if ("CO".equals(docStatus)) {

			keepOnly(data, DocumentEngine.ACTION_ReActivate, DocumentEngine.ACTION_Void);
			addIfMissing(data, DocumentEngine.ACTION_ReActivate);
			addIfMissing(data, DocumentEngine.ACTION_Void);
		} else {
			clearOptions(data);
		}
	}

	/**
	 * 生产补数申请单 (PP_Order_Repair_Request) 单据动作过滤
	 * 草稿 → 完成
	 * 处理中 → 完成 + 作废
	 * 无效 → 完成
	 * 已作废 → 无
	 * 其余未列出状态 → 无
	 */
	private static void handlePPOrderRepairRequest(DocActionEventData data) {
		String docStatus = data.docStatus;

		if ("DR".equals(docStatus) || "IN".equals(docStatus)) {
			// 草稿/无效：只保留完成
			keepOnly(data, DocumentEngine.ACTION_Complete);
		} else if ("IP".equals(docStatus)) {
			// 处理中：保留完成 + 作废
			keepOnly(data, DocumentEngine.ACTION_Complete, DocumentEngine.ACTION_Void);
//			addIfMissing(data, DocumentEngine.ACTION_Void);
		} else {
			// 已作废及其余未列出状态：清空
			clearOptions(data);
		}
	}

	/**
	 * 获取单据类型的 DocSubTypeInv（区分 M_Inventory 下的领用单/盘点单/成本调整）
	 */
	private static String getDocSubTypeInv(DocActionEventData data) {
		int docTypeId = data.po.get_ValueAsInt("C_DocType_ID");
		if (docTypeId <= 0) {
			return "";
		}
		MDocType docType = MDocType.get(Env.getCtx(), docTypeId);
		return docType != null ? docType.getDocSubTypeInv() : "";
    }

    // ========== 工具方法 ==========

    /**
     * 获取单据类型名称
     */
    private static String getDocTypeName(DocActionEventData data) {
        int docTypeId = data.po.get_ValueAsInt("C_DocType_ID");
        if (docTypeId <= 0) {
            return "";
        }
        MDocType docType = MDocType.get(Env.getCtx(), docTypeId);
        return docType != null ? docType.getName() : "";
    }

    /**
     * 只保留指定的动作，移除其它
     */
    private static void keepOnly(DocActionEventData data, String... keep) {
        int index = data.indexObj.get();
        List<String> kept = new ArrayList<>();

        for (int i = 0; i < index && i < data.options.size(); i++) {
            String opt = data.options.get(i);
            for (String k : keep) {
                if (k.equals(opt)) {
                    kept.add(opt);
                    break;
                }
            }
        }

        // 保持 options 大小不变，只修改前缀内容
        data.options.clear();
        data.options.addAll(kept);
        data.indexObj.set(kept.size());
    }

    /**
     * 从 options 中移除指定动作
     */
    private static void removeAction(DocActionEventData data, String action) {
        int index = data.indexObj.get();
        List<String> kept = new ArrayList<>();

        for (int i = 0; i < index && i < data.options.size(); i++) {
            String opt = data.options.get(i);
            if (!action.equals(opt)) {
                kept.add(opt);
            }
        }

        data.options.clear();
        data.options.addAll(kept);
        data.indexObj.set(kept.size());
    }

    /**
     * 清空所有选项
     */
    private static void clearOptions(DocActionEventData data) {
        data.options.clear();
        data.indexObj.set(0);
    }

    /**
     * 如果 action 不在 options 中，强制添加
     */
    private static void addIfMissing(DocActionEventData data, String action) {
        int index = data.indexObj.get();
        for (int i = 0; i < index && i < data.options.size(); i++) {
            if (action.equals(data.options.get(i))) {
                return; // 已存在
            }
        }
        data.options.add(action);
        data.indexObj.set(data.options.size());
    }
}
