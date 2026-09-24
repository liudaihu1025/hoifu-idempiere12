package com.hoifu.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.compiere.util.CLogger;
import org.json.JSONObject;

/**
 * 企微群机器人消息推送工具（通用）。
 */
public class WeChatRobotUtils {

	private static final CLogger log = CLogger.getCLogger(WeChatRobotUtils.class);

	/**
	 * 发送纯文本消息到指定的企微群机器人。
	 * 
	 * @param webhookUrl 企微机器人 Webhook 地址
	 * @param content    消息文本内容
	 */
	public static void sendText(String webhookUrl, String content) {
		if (webhookUrl == null || webhookUrl.isEmpty()) {
			log.warning("企微机器人 Webhook 地址为空，跳过通知。内容：" + content);
			return;
		}
		if (content == null || content.isEmpty()) {
			log.warning("企微机器人推送内容为空，跳过通知。");
			return;
		}

		try {
			// 企微群机器人官方消息体格式：
			// {"msgtype":"text","text":{"content":"..."}}
			JSONObject text = new JSONObject();
			text.put("content", content);

			JSONObject body = new JSONObject();
			body.put("msgtype", "text");
			body.put("text", text);

			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Type", "application/json; charset=utf-8");

			// 复用仓库统一的 HTTP 工具类，保证超时时间、连接释放、异常处理与其它外部接口调用一致
			String resp = HttpClientUtils.post(webhookUrl, body.toString(), headers);

			// 企微接口即使 HTTP 状态码是200，也不代表业务上发送成功，
			// 必须解析响应体里的 errcode 字段（0 表示成功，非0表示失败，比如 key 不合法、内容过长等）
			JSONObject respJson = new JSONObject(resp);
			int errcode = respJson.optInt("errcode", -1);
			if (errcode != 0) {
				log.warning("企微机器人推送失败，errcode=" + errcode + ", errmsg=" + respJson.optString("errmsg"));
			}
		} catch (Exception e) {
			// 通知失败不应影响调用方的主业务流程，只记录日志
			log.log(Level.WARNING, "企微机器人推送异常，webhookUrl=" + webhookUrl + ", content=" + content, e);
		}
	}
}