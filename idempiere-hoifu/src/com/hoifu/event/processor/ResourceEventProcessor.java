package com.hoifu.event.processor;

import org.adempiere.base.event.IEventTopics;
import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MResource;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.hoifu.enums.HFSysConfigEnum;
import com.hoifu.utils.WeChatRobotUtils;

/**
 * 资源(S_Resource)事件处理器。 职责1：一个用户在"人力资源"类型的资源中只能绑定一个资源，需要在资源保存前做唯一性校验。
 * 职责2：人力资源类型的资源新建成功后，自动新建了关联的 M_Product， 需要通知财务去产品价格表(M_ProductPrice)里维护该员工的时薪，
 */
public class ResourceEventProcessor implements IEventProcessor {

	private static final CLogger log = CLogger.getCLogger(ResourceEventProcessor.class);

	@Override
	public boolean supports(PO po, String topic) {
		return po instanceof MResource;
	}

	@Override
	public void process(PO po, String topic) {
		MResource resource = (MResource) po;

		// 保存前：唯一性校验
		checkUserBindingUnique(resource, topic);

		// 保存后：新建的人力资源类型资源，通知财务维护产品价格
		notifyFinanceForNewHRResource(resource, topic);
	}

	/**
	 * 校验：同一用户在"人力资源"类型下只能绑定一个资源。 触发时机：PO_BEFORE_NEW / PO_BEFORE_CHANGE
	 */
	private void checkUserBindingUnique(MResource resource, String topic) {
		if (!IEventTopics.PO_BEFORE_NEW.equals(topic) && !IEventTopics.PO_BEFORE_CHANGE.equals(topic)) {
			return;
		}

		int userId = resource.getAD_User_ID();
		if (userId <= 0) {
			return; // 未关联用户的资源（如设备）不需要校验
		}

		String trxName = resource.get_TrxName();

		if (!isHRResourceType(resource, trxName)) {
			return; // 非人力资源类型，不校验
		}

		String hrTypeValue = HFSysConfigEnum.HF_HR_RESOURCE_TYPE_VALUE.getValue(resource.getAD_Client_ID());

		int cnt = DB.getSQLValue(trxName,
				"SELECT COUNT(*) FROM S_Resource r "
						+ "INNER JOIN S_ResourceType rt ON r.S_ResourceType_ID = rt.S_ResourceType_ID "
						+ "WHERE r.AD_User_ID = ? " + "  AND rt.Value = ? " + "  AND r.IsActive = 'Y' "
						+ "  AND r.S_Resource_ID <> ?",
				userId, hrTypeValue, resource.getS_Resource_ID());

		if (cnt > 0) {
			throw new AdempiereException("该用户已绑定人力资源，一个用户只能绑定一个人力资源");
		}
	}

	/**
	 * 新建人力资源类型的资源成功后，通知财务去维护产品价格表（员工时薪）。
	 * 触发时机：PO_AFTER_NEW（此时资源已保存成功，MResource.afterSave 也已自动创建关联的 M_Product）。
	 * 通知渠道：仅企业微信群机器人。Webhook 未配置时跳过，不影响资源创建主流程。
	 */
	private void notifyFinanceForNewHRResource(MResource resource, String topic) {
		if (!IEventTopics.PO_AFTER_NEW.equals(topic)) {
			return;
		}

		int userId = resource.getAD_User_ID();
		if (userId <= 0) {
			return; // 未关联用户的资源（如设备），不属于本次场景
		}

		String trxName = resource.get_TrxName();

		if (!isHRResourceType(resource, trxName)) {
			return; // 非人力资源类型，不通知
		}

		String webhookUrl = HFSysConfigEnum.HF_WECHAT_ROBOT_LABOR_COST_WEBHOOK_URL.getValue(resource.getAD_Client_ID());
		if (webhookUrl == null || webhookUrl.isEmpty()) {
			log.warning(
					"人工成本企微通知未发送：Webhook未配置，S_Resource_ID=" + resource.getS_Resource_ID() + "，AD_User_ID=" + userId);
			return;
		}

		try {
			String content = "【海富ERP】：已新增组员：" + resource.getName() + " ，请前往【直接人工】-【价格】维护人工成本。";

			log.warning("准备发送人工成本企微通知，S_Resource_ID=" + resource.getS_Resource_ID() + "，AD_User_ID=" + userId);

			WeChatRobotUtils.sendText(webhookUrl, content);

			log.warning("人工成本企微通知发送成功，S_Resource_ID=" + resource.getS_Resource_ID() + "，AD_User_ID=" + userId);
		} catch (Exception e) {
			// 通知失败不应影响资源创建这个主流程，只记录日志
			log.warning("人工成本企微通知发送失败，S_Resource_ID=" + resource.getS_Resource_ID() + "，原因：" + e.getMessage());
		}
	}

	/**
	 * 判断当前资源的类型是否为"人力资源"（通过 AD_SysConfig 配置的 S_ResourceType.Value 约定值比对，不硬编码 ID）。
	 */
	private boolean isHRResourceType(MResource resource, String trxName) {
		String hrTypeValue = HFSysConfigEnum.HF_HR_RESOURCE_TYPE_VALUE.getValue(resource.getAD_Client_ID());
		if (hrTypeValue == null || hrTypeValue.isEmpty()) {
			return false;
		}

		String resourceTypeValue = DB.getSQLValueString(trxName,
				"SELECT Value FROM S_ResourceType WHERE S_ResourceType_ID=?", resource.getS_ResourceType_ID());

		return hrTypeValue.equals(resourceTypeValue);
	}
}