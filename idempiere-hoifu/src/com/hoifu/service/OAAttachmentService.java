package com.hoifu.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.util.CLogger;
import org.compiere.util.Env;
import org.osgi.service.component.annotations.Component;

import com.hoifu.model.OAIntegrationConfig;
import com.hoifu.utils.HttpClientUtils;
import com.hoifu.utils.OATokenUtils;

/**
 * 
 * @Description: OA系统附件上传服务
 * @author ldh
 * @date 2025年12月11日
 */
@Component(immediate = true, service = OAAttachmentService.class)
public class OAAttachmentService {

	private static CLogger log = CLogger.getCLogger(OAAttachmentService.class);

	private static final String OA_API_UPLOAD_URL = "/api/uploadAttachments";

	/**
	 * 上传业务表的所有附件到OA系统并返回完整响应
	 * 
	 * @param po       业务表对象
	 * @param configId 配置ID
	 * @return OA系统返回的完整响应
	 */
	public String uploadAttachmentsAndGetResponse(PO po, OAIntegrationConfig config) {
		String oaApiUrlPrefix = MSysConfig.getValue(MSysConfig.OA_API_URL_PREFIX, null, 0);
		if (Objects.isNull(oaApiUrlPrefix) || "".equals(oaApiUrlPrefix)) {
			throw new AdempiereException("接口URL配置不完整,请检查MSysConfig中的配置");
		}

		// 当前登录用户
		MUser loginUser = MUser.get(Env.getCtx());
		if (Objects.isNull(loginUser)) {
			throw new AdempiereException("当前登录用户为空, 请检查登录状态");
		}

		try {
			MAttachment attachment = po.getAttachment();
			if (attachment == null || attachment.getEntryCount() == 0) {
				log.info("业务表没有附件: " + po.get_ID());
				return null;
			}

			// 构建 URL，添加 loginName 参数
			String uploadUrl = oaApiUrlPrefix + OA_API_UPLOAD_URL + "?loginName="
					+ URLEncoder.encode(loginUser.getName(), "UTF-8");

			String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
			String lineEnd = "\r\n";
			String twoHyphens = "--";

			// 构建请求头
			Map<String, String> headers = new HashMap<>();
			headers.put("Authorization", "Bearer " + OATokenUtils.getValidToken(config.getOAIntegrationConfigID()));
			headers.put("User-Agent", "iDempiere-OA-Integration/1.0");

			ByteArrayOutputStream requestBody = new ByteArrayOutputStream();

			// 添加每个附件 - 使用 "files" (复数)
			for (int i = 0; i < attachment.getEntryCount(); i++) {
				MAttachmentEntry entry = attachment.getEntry(i);
				String fileName = entry.getName();
				InputStream inputStream = entry.getInputStream();

				// 读取文件数据
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					baos.write(buffer, 0, bytesRead);
				}
				byte[] fileData = baos.toByteArray();
				inputStream.close();

				// 文件字段 - 使用 "files" 而不是 "file"
				StringBuilder formData = new StringBuilder();
				formData.append(twoHyphens).append(boundary).append(lineEnd);
				formData.append("Content-Disposition: form-data; name=\"files\"; filename=\"").append(fileName)
						.append("\"").append(lineEnd);
				formData.append("Content-Type: ").append(this.getContentType(fileName)).append(lineEnd);
				formData.append(lineEnd);

				requestBody.write(formData.toString().getBytes(StandardCharsets.UTF_8));
				requestBody.write(fileData);
				requestBody.write(lineEnd.getBytes(StandardCharsets.UTF_8));
			}

			// 结束边界
			String footer = twoHyphens + boundary + twoHyphens + lineEnd;
			requestBody.write(footer.getBytes(StandardCharsets.UTF_8));

			// 设置请求头
			Map<String, String> requestHeaders = new HashMap<>(headers);
			requestHeaders.put("Content-Type", "multipart/form-data; boundary=" + boundary);

			// 发送请求到包含 loginName 的 URL
			return HttpClientUtils.postUpload(uploadUrl, requestHeaders, requestBody.toByteArray());

		} catch (Exception e) {
			log.warning("上传业务表附件失败: " + e.getMessage());
			throw new AdempiereException("上传业务表附件异常" + e.getMessage());
		}
	}

	/**
	 * 根据文件名获取Content-Type
	 * 
	 * @param fileName 文件名
	 * @return MIME类型
	 */
	private String getContentType(String fileName) {
		if (fileName == null) {
			return "application/octet-stream";
		}

		String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

		switch (extension) {
		case "pdf":
			return "application/pdf";
		case "doc":
		case "docx":
			return "application/msword";
		case "xls":
		case "xlsx":
			return "application/vnd.ms-excel";
		case "jpg":
		case "jpeg":
			return "image/jpeg";
		case "png":
			return "image/png";
		case "txt":
			return "text/plain";
		case "xml":
			return "application/xml";
		default:
			return "application/octet-stream";
		}
	}
}