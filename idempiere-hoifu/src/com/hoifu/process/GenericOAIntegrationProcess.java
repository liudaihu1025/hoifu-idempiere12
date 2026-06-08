package com.hoifu.process;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.compiere.model.MAttachment;
import org.compiere.model.MSysConfig;
import org.compiere.model.MTable;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.process.SvrProcess;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hoifu.model.FieldMapping;
import com.hoifu.model.OAIntegrationConfig;
import com.hoifu.service.LookupService;
import com.hoifu.service.OAAttachmentService;
import com.hoifu.service.OAIntegrationService;
import com.hoifu.service.TypeConversionService;
import com.hoifu.utils.OATokenUtils;

/**
 * @Description: 通用OA集成Process
 * @author ldh
 * @date 2025年11月5日
 */
@org.adempiere.base.annotation.Process
public class GenericOAIntegrationProcess extends SvrProcess {

	//OA系统返回单号字段
	private String OA_SYS_ORDER_NO = "recID";
	
	//ERP系统OA单号字段
	private String ERP_SYS_OA_ORDER_NO = "OAID";
	
	// OA流程配置ID
	private int P_OA_INTEGRATION_CONFIG_ID = 0;
	
	// 业务表ID
	private int P_RECORD_ID = 0;
	
	// 工作流审批节点ID
	private int P_AD_WF_NODE_ID = 0;
	
	// OA流程配置
	private OAIntegrationConfig config = null;
	
	// OA接口前缀, 测试环境:http://192.168.1.96:8088
	private String OA_API_URL_PREFIX = null;
	
	// 全部附件数组
	private JSONArray attachmentJsonArray = new JSONArray();
	
	// OA流程发起API接口名
	private final String OA_START_PROCESS_API = "/api/startProcess";
	
	// OA流程发起API接口名
	private final String OA_UPLOAD_ATTACHMENTS_API = "/api/uploadAttachments";
	
	private final LookupService lookupService = new LookupService();
	private final OAAttachmentService oAAttachmentService = new OAAttachmentService();
	private final OAIntegrationService oAIntegrationService = new OAIntegrationService();
	private final TypeConversionService conversionService = new TypeConversionService();

	@Override
	protected void prepare() {
		Arrays.stream(getParameter()).filter(p -> p.getParameter() != null).forEach(p -> {
			if ("OA_Integration_Config_ID".equals(p.getParameterName())) {
				P_OA_INTEGRATION_CONFIG_ID = p.getParameterAsInt();
			} else if ("AD_WF_Node_ID".equals(p.getParameterName())) {
				P_AD_WF_NODE_ID = p.getParameterAsInt();
			}
		});

		if (P_RECORD_ID == 0) {
			P_RECORD_ID = getRecord_ID();
		}

		OA_API_URL_PREFIX = MSysConfig.getValue(MSysConfig.OA_API_URL_PREFIX, null, 0);

	}

	@Override
	protected String doIt() throws Exception {
		// 校验参数
		this.validateParameters();
		
		boolean isSuccess = false;
		String errorMessage = null;
		String response = null;
		JSONObject request = null;
		long startTime = System.currentTimeMillis();

		try {
			// 1. 加载配置
			config = loadConfig(P_OA_INTEGRATION_CONFIG_ID);

			// 2. 构建完整JSON请求
			request = buildCompleteRequest(config, P_RECORD_ID);

			// 3. 调用OA接口发起流程
			response = oAIntegrationService.sendToOAWithToken(OA_API_URL_PREFIX + OA_START_PROCESS_API, request,
					OATokenUtils.getValidToken(P_OA_INTEGRATION_CONFIG_ID));

			// 3.1 解析响应，更新OA单号到业务记录  
			if (response != null && !response.trim().isEmpty()) {  
			    JSONObject respJson = new JSONObject(response);  
			    if (respJson.optBoolean("success", false) && respJson.optInt("code", -1) == 0) {  
			        JSONObject data = respJson.optJSONObject("data");  
			        if (data != null && data.has(OA_SYS_ORDER_NO)) {  
			            String oaId = data.getString(OA_SYS_ORDER_NO);  
			            MTable table = MTable.get(getCtx(), config.getTableID());  
			            PO po = table.getPO(P_RECORD_ID, get_TrxName());  
			            if (po != null && po.get_ColumnIndex(ERP_SYS_OA_ORDER_NO) >= 0) {  
			                po.set_ValueOfColumn(ERP_SYS_OA_ORDER_NO, oaId);  
			                po.saveEx(get_TrxName());  
			                log.info("已更新OA单号到记录: " + po.get_TableName() + "_ID=" + po.get_ID() + ", OAID=" + oaId);  
			            } else {  
			                log.warning("表 " + table.getTableName() + " 不存在 OAID 字段，跳过更新");  
			            }  
			        }  
			    }  
			}  
			
			isSuccess = true;

			return "成功发送到OA系统: " + response;
		} catch (Exception e) {
			errorMessage = e.getMessage();
			throw new AdempiereException("调用OA接口发起流程异常" + errorMessage);
		} finally {
			// 5. 记录接口调用日志
			long executionTime = System.currentTimeMillis() - startTime;
			if (request != null && config != null) {
				logIntegrationCall(config, P_RECORD_ID, request, response != null ? response : "", isSuccess,
						errorMessage, executionTime, "OA", OA_API_URL_PREFIX + OA_START_PROCESS_API);
			}
		}
	}

	/**
	 * 
	 * @Description: 校验参数
	 */
	private void validateParameters() {
		if (P_OA_INTEGRATION_CONFIG_ID == 0) {
			throw new AdempiereException("未指定OA集成配置");
		}
		if (P_RECORD_ID == 0) {
			throw new AdempiereException("未指定记录ID");
		}
		if (P_AD_WF_NODE_ID == 0) {
			throw new AdempiereException("未指定工作流节点ID");
		}
		if (Objects.isNull(OA_API_URL_PREFIX) || "".equals(OA_API_URL_PREFIX)) {
			throw new AdempiereException("未指定系统配置OA_API_URL_PREFIX");
		}
	}

	/**
	 * 
	 * @Description: 读取OA集成配置内容
	 * @param configID
	 * @return
	 */
	private OAIntegrationConfig loadConfig(int configID) {
		String sql = "SELECT Name, AD_Table_ID, OA_Process_ID, "
				+ "Detail_Table_ID, Detail_Link_Column, Main_Form_Key, Detail_Array_Key, Has_Data_Wrapper "
				+ "FROM OA_Integration_Config WHERE OA_Integration_Config_ID=? AND IsActive='Y'";

		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, configID);
			try (ResultSet rs = pstmt.executeQuery()) {
				if (rs.next()) {
					OAIntegrationConfig config = new OAIntegrationConfig();
					config.setName(rs.getString(1));
					config.setTableID(rs.getInt(2));
					config.setOAProcessID(rs.getString(3));
					config.setDetailTableID(rs.getInt(4));
					config.setDetailLinkColumn(rs.getString(5));
					config.setMainFormKey(rs.getString(6));
					config.setDetailArrayKey(rs.getString(7));
					config.setHasDataWrapper("Y".equals(rs.getString(8)));
					config.setFieldMappings(loadFieldMappings(configID));
					return config;
				}
			}
		} catch (SQLException e) {
			throw new DBException("读取OA集成配置内容DB异常", e);
		}

		throw new AdempiereException("配置不存在或未激活: " + configID);
	}

	/**
	 * 
	 * @Description: 读取字段配置数据
	 * @param configID
	 * @return
	 */
	private List<FieldMapping> loadFieldMappings(int configID) {
		String sql = "SELECT m.ColumnName, m.OA_Field_Name, m.DefaultValue, "
				+ "m.ConversionType, m.DateFormat, m.MaxLength, m.DecimalScale, "
				+ "m.TruncateIfTooLong, m.IsLookup, m.LookupTable, m.LookupKeyColumn, "
				+ "m.LookupDisplayColumns, m.LookupSeparator, m.FieldLevel, "
				+ " m.IsStaticValue, m.StaticValue, m.LookupAndParam, m.LookupOrderBy  " + "FROM OA_Field_Mapping m "
				+ "WHERE m.OA_Integration_Config_ID=? AND m.IsActive='Y' " + "ORDER BY m.SeqNo";

		List<FieldMapping> mappings = new ArrayList<>();

		try (PreparedStatement pstmt = DB.prepareStatement(sql, get_TrxName())) {
			pstmt.setInt(1, configID);
			try (ResultSet rs = pstmt.executeQuery()) {
				while (rs.next()) {
					mappings.add(createFieldMapping(rs));
				}
			}
		} catch (SQLException e) {
			throw new DBException("读取字段配置数据异常", e);
		}

		return mappings;
	}

	/**
	 * 
	 * @Description: 组装数据
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private FieldMapping createFieldMapping(ResultSet rs) throws SQLException {
		FieldMapping mapping = new FieldMapping();
		mapping.setColumnName(rs.getString(1));
		mapping.setOAFieldName(rs.getString(2));
		mapping.setDefaultValue(rs.getString(3));
		mapping.setConversionType(rs.getString(4));
		mapping.setDateFormat(rs.getString(5));
		mapping.setMaxLength(rs.getInt(6));
		mapping.setDecimalScale(rs.getInt(7));
		mapping.setTruncateIfTooLong("Y".equals(rs.getString(8)));
		mapping.setLookup("Y".equals(rs.getString(9)));
		mapping.setLookupTable(rs.getString(10));
		mapping.setLookupKeyColumn(rs.getString(11));
		mapping.setLookupDisplayColumns(rs.getString(12));
		mapping.setLookupSeparator(rs.getString(13));
		mapping.setFieldLevel(rs.getString(14)); // ROOT/MAIN/DETAIL
		mapping.setStaticValue("Y".equals(rs.getString(15)));
		mapping.setStaticValueContent(rs.getString(16));
		mapping.setLookupAndParam(rs.getString(17));
		mapping.setLookupOrderBy(rs.getString(18));
		return mapping;
	}

	/**
	 * 
	 * @Description: 构建完整的OA请求JSON
	 * @param config
	 * @param recordId
	 * @return
	 */
	private JSONObject buildCompleteRequest(OAIntegrationConfig config, int recordId) {
		JSONObject request = new JSONObject();
		request.put("templateCode", config.getOAProcessID());
		request.put("loginName", MUser.get(Env.getCtx()).getName());
		request.put("requestId", getTable_ID() + "-" + recordId + "-" + P_AD_WF_NODE_ID + "-"
				+ (ThreadLocalRandom.current().nextInt(1000, 10000)));

		// 获取主表PO
		MTable table = MTable.get(getCtx(), config.getTableID());
		PO po = table.getPO(recordId, get_TrxName());

		// 1. 构建根级别字段
		buildRootLevelFields(request, config, po);

		// 查询主表关联附件并上传附件到OA
		JSONArray mainAttachments = this.buildOAAttachments(config, po);
		buildRootLevelFields(request, config, po);
		
		// 构建主表数据
		JSONObject mainForm = buildMainForm(config, po);
		
		// 主表数据添加附件
		if (mainAttachments != null && mainAttachments.length() > 0) {
			for (int i = 0; i < mainAttachments.length(); i++) {
				JSONObject attachment = mainAttachments.getJSONObject(i);
				attachmentJsonArray.put(attachment);
			}
			mainForm.put("主附件", mainAttachments.getJSONObject(0).getString("subReference"));
		}

		// 2. 构建data对象(如果配置了data包装层)
		if (config.isHasDataWrapper()) {
			// 2.1 构建主表数据
			JSONObject data = new JSONObject();
			data.put(config.getMainFormKey(), mainForm);

			// 2.2 构建明细数据
			if (config.getDetailTableID() > 0) {
				JSONArray detailArray = buildDetailArray(config, recordId);
				data.put(config.getDetailArrayKey(), detailArray);
			}

			// 2.3 构建附件数据
			if (attachmentJsonArray != null && attachmentJsonArray.length() > 0) {
				data.put("thirdAttachments", attachmentJsonArray);
			}

			request.put("data", data);
		} else {
            // 直接在根级别构建主表和明细
			for (String key : mainForm.keySet()) {
				request.put(key, mainForm.get(key));
			}

			if (config.getDetailTableID() > 0) {
				JSONArray detailArray = buildDetailArray(config, recordId);
				request.put(config.getDetailArrayKey(), detailArray);
			}

			// 添加附件数据到根级别
			if (attachmentJsonArray != null && attachmentJsonArray.length() > 0) {
				request.put("thirdAttachments", attachmentJsonArray);
			}
		}

		return request;
	}

	/**
	 * 调用OA上传附件并构建附件数据参数
	 * 
	 * @param config OA集成配置
	 * @param po     业务对象
	 * @return 附件JSON数组
	 */
	private JSONArray buildOAAttachments(OAIntegrationConfig config, PO po) {
		MAttachment attachment = po.getAttachment();
		if (attachment == null || attachment.getEntryCount() == 0) {
			log.info("业务表没有附件: " + po.get_ID());
			return null;
		}

		boolean isSuccess = false;
		String errorMessage = null;
		String uploadResponse = null;
		long startTime = System.currentTimeMillis();

		try {
			// 上传附件并获取响应
			uploadResponse = oAAttachmentService.uploadAttachmentsAndGetResponse(po, config);

			// 记录OA上传附件接口调用日志
			long executionTime = System.currentTimeMillis() - startTime;
			logIntegrationCall(config, P_RECORD_ID, new JSONObject(), uploadResponse != null ? uploadResponse : "",
					isSuccess, errorMessage, executionTime, "OA-ATT", OA_API_URL_PREFIX + OA_UPLOAD_ATTACHMENTS_API);

			if (uploadResponse == null || uploadResponse.trim().isEmpty()) {
				log.warning("OA上传附件接口返回为空");
				return null;
			}

			// 解析响应获取data数组
			JSONObject responseJson = new JSONObject(uploadResponse);
			if (responseJson.has("success") && responseJson.getBoolean("success") && responseJson.has("data")) {
				isSuccess = true;
				return responseJson.getJSONArray("data");
			} else {
				log.warning("OA上传附件接口返回异常" + responseJson.toString());
			}
		} catch (Exception e) {
			log.warning("构建附件数据失败: " + e.getMessage());
			throw new AdempiereException("构建附件数据失败", e);
		}
		return null;
	}

	/**
	 * 
	 * @Description: 构建根级别字段(如loginName, templateCode等)
	 * @param request
	 * @param config
	 * @param po
	 */
	private void buildRootLevelFields(JSONObject request, OAIntegrationConfig config, PO po) {
		List<FieldMapping> rootMappings = config.getFieldMappings().stream()
				.filter(m -> "ROOT".equals(m.getFieldLevel())).toList();

		for (FieldMapping mapping : rootMappings) {
			Object value;

			// 判断是静态值还是动态值
			if (mapping.isStaticValue()) {
				value = mapping.getStaticValueContent();
			} else {
				value = getFieldValue(po, mapping);
			}

			request.put(mapping.getOAFieldName(), value);
		}
	}

	/**
	 * 
	 * @Description: 构建主表数据(mainData)
	 * @param config
	 * @param po
	 * @return
	 */
	private JSONObject buildMainForm(OAIntegrationConfig config, PO po) {
		JSONObject mainForm = new JSONObject();

		List<FieldMapping> mainMappings = config.getFieldMappings().stream()
				.filter(m -> "MAIN".equals(m.getFieldLevel())).toList();

		for (FieldMapping mapping : mainMappings) {
			Object value = getFieldValue(po, mapping);
			mainForm.put(mapping.getOAFieldName(), value);
		}

		return mainForm;
	}

	/**
	 * 
	 * @Description: 构建明细数组(detailData)
	 * @param config
	 * @param recordId
	 * @return
	 */
	private JSONArray buildDetailArray(OAIntegrationConfig config, int recordId) {
		JSONArray detailArray = new JSONArray();

		List<FieldMapping> detailMappings = config.getFieldMappings().stream()
				.filter(m -> "DETAIL".equals(m.getFieldLevel())).toList();

		// 查询明细记录
		MTable detailTable = MTable.get(getCtx(), config.getDetailTableID());
		String sql = "SELECT " + detailTable.getTableName() + "_ID FROM " + detailTable.getTableName() + " WHERE "
				+ config.getDetailLinkColumn() + " = ? " + " AND IsActive = 'Y' ORDER BY Line";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			pstmt.setInt(1, recordId);
			rs = pstmt.executeQuery();

			while (rs.next()) {
				int detailId = rs.getInt(1);
				PO detailPO = detailTable.getPO(detailId, get_TrxName());

				JSONObject detailItem = new JSONObject();
				for (FieldMapping mapping : detailMappings) {
					Object value = getFieldValue(detailPO, mapping);
					detailItem.put(mapping.getOAFieldName(), value);
				}

//				// 查询明细表关联附件并上传附件到OA
//				JSONArray detailAttachments = this.buildOAAttachments(config, detailPO);
//				// 明细表数据添加附件
//				if (detailAttachments != null && detailAttachments.length() > 0) {
//					for (int i = 1; i <= detailAttachments.length(); i++) {
//						JSONObject attachment = detailAttachments.getJSONObject(i - 1);
//						if (attachment.has("fileUrl")) {
//							String subReference = attachment.getString("fileUrl");
//							detailItem.put("明细附件" + i, subReference);
//							attachmentJsonArray.put(attachment);
//						}
//					}
//				}

				detailArray.put(detailItem);
			}
		} catch (SQLException e) {
			log.warning("构建明细数组(detailData)失败:" + e.getMessage());
			throw new DBException("构建明细数组(detailData)失败:" + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}

		return detailArray;
	}

	/**
	 * 
	 * @Description: 获取字段值 - 处理关联查询和类型转换
	 * @param po
	 * @param mapping
	 * @return
	 */
	private String getFieldValue(PO po, FieldMapping mapping) {
		Object value = po.get_Value(mapping.getColumnName());

		// 处理关联查询
		if (mapping.isLookup() && value != null) {
			value = lookupService.performLookup(mapping, value, get_TrxName()).orElse(null);
		}

		// 类型转换
		return conversionService.convertValue(value, mapping);
	}

	/**
	 * 
	 * @Description: 记录接口调用日志
	 * @param config
	 * @param recordId
	 * @param request
	 * @param response
	 * @param isSuccess
	 * @param errorMessage
	 * @param executionTime
	 */
	private void logIntegrationCall(OAIntegrationConfig config, int recordId, JSONObject request, String response,
			boolean isSuccess, String errorMessage, long executionTime, String systemType, String oaApiUrl) {
		String sql = "INSERT INTO OA_Integration_Log (" + "OA_Integration_Log_ID, AD_Client_ID, AD_Org_ID, IsActive, "
				+ "Created, CreatedBy, Updated, UpdatedBy, " + "OA_Integration_Config_ID, AD_Table_ID, Record_ID, "
				+ "External_System_Type, API_URL, Business_Type, "
				+ "Request_JSON, Response_JSON, IsSuccess, Error_Message, Execution_Time" + ") VALUES ("
				+ "NEXTVAL('OA_Integration_Log_SQ'), ?, ?, 'Y', " + "NOW(), ?, NOW(), ?, " + "?, ?, ?, " + "?, ?, ?, "
				+ "?, ?, ?, ?, ?" + ")";

		PreparedStatement pstmt = null;
		try {
			pstmt = DB.prepareStatement(sql, get_TrxName());
			// 使用 Env 类获取 AD_Client_ID 和 AD_Org_ID
			pstmt.setInt(1, Env.getAD_Client_ID(getCtx()));
			pstmt.setInt(2, Env.getAD_Org_ID(getCtx()));
			pstmt.setInt(3, Env.getAD_User_ID(getCtx()));
			pstmt.setInt(4, Env.getAD_User_ID(getCtx()));

			pstmt.setInt(5, P_OA_INTEGRATION_CONFIG_ID);
			pstmt.setInt(6, config.getTableID());
			pstmt.setInt(7, recordId);

			pstmt.setString(8, systemType); // External_System_Type
			pstmt.setString(9, oaApiUrl);
			pstmt.setString(10, config.getName());

			pstmt.setString(11, request.toString());
			pstmt.setString(12, response);
			pstmt.setString(13, isSuccess ? "Y" : "N");
			pstmt.setString(14, errorMessage);
			pstmt.setLong(15, executionTime);

			pstmt.executeUpdate();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Failed to log integration call", e);
		} finally {
			DB.close(pstmt);
		}
	}
}