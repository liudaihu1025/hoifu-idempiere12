package com.hoifu.service.extsync.wms;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.model.MBPartner;
import org.compiere.model.MInOut;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.model.PO;
import org.compiere.model.X_S_Resource;
import org.compiere.util.Env;
import org.eevolution.model.I_PP_Order;
import org.json.JSONArray;
import org.json.JSONObject;

import com.hoifu.enums.HFSysConfigEnum;
import com.hoifu.service.extsync.ExtSyncResult;
import com.hoifu.service.extsync.IExternalSystemAdapter;
import com.hoifu.utils.HttpClientUtils;

/**
 * WMS系统同步数据适配器实现
 * @ClassName: WmsSystemAdapter
 * @author ldh
 * @date 2026年8月19日
 */
public class WmsSystemAdapter implements IExternalSystemAdapter {

	public static final String SYSTEM_TYPE = "WMS";

	public static final String BT_PRODUCT = "PRODUCT";
	public static final String BT_BPARTNER = "BPARTNER";
	public static final String BT_RESOURCE = "RESOURCE";
	public static final String BT_INOUT = "INOUT";
	public static final String BT_PPORDER = "PPORDER";

	private static final Map<String, String> API_PATH_MAP = new HashMap<>();
	static {
		// TODO 按实际同步的数据类型和接口名进行配置改动
		API_PATH_MAP.put(BT_PRODUCT, "/erp/material/add");
//		API_PATH_MAP.put(BT_BPARTNER, "/api/wms/bpartner/sync");
//		API_PATH_MAP.put(BT_RESOURCE, "/api/wms/resource/sync");
//		API_PATH_MAP.put(BT_INOUT, "/api/wms/inout/sync");
//		API_PATH_MAP.put(BT_PPORDER, "/api/wms/pporder/sync");
	}

	/**
	 * 本系统物料分类(MaterialType参照值) -> WMS materialType 整数枚举 的映射表。
	 * 设计为独立的静态Map而不是switch-case，新增/调整映射关系只需改这一处配置
	 */
	private static final Map<String, Integer> MATERIAL_TYPE_MAP = new ConcurrentHashMap<>();
	static {
		MATERIAL_TYPE_MAP.put("MT01", 10); // 原材料 -> 原材
		MATERIAL_TYPE_MAP.put("MT02", 50); // 半成品 -> 半成品
		MATERIAL_TYPE_MAP.put("MT03", 20); // 成品 -> 成品
		MATERIAL_TYPE_MAP.put("MT05", 30); // 辅料 -> 辅料
		// TODO: MT04(行政物资) 在WMS枚举中无直接对应值，需与WMS对接方确认后再补充映射，
		// 在确认前不加入此Map，未匹配时按下方 resolveWmsMaterialType() 的兜底策略处理
	}

	@Override
	public String getSystemType() {
		return SYSTEM_TYPE;
	}

	@Override
	public boolean supports(String businessType) {
		return API_PATH_MAP.containsKey(businessType);
	}

	@Override
	public String buildRequest(PO po, String businessType, String eventType) {
		JSONObject body = new JSONObject();
		body.put("eventType", eventType);

		switch (businessType) {
		case BT_PRODUCT:
			return buildProductPayload(body, (MProduct) po);
		case BT_BPARTNER:
			return buildBPartnerPayload(body, (MBPartner) po);
		case BT_RESOURCE:
			return buildResourcePayload(body, (X_S_Resource) po);
		case BT_INOUT:
			return buildInOutPayload(body, (MInOut) po);
		case BT_PPORDER:
			return buildPPOrderPayload(body, (I_PP_Order) po);
		default:
			throw new IllegalArgumentException("WmsSystemAdapter不支持的业务类型: " + businessType);
		}
	}

	// ============ 以下 buildXxxPayload 方法留空，由业务实现填充具体字段 ============
	private String buildProductPayload(JSONObject body, MProduct product) {
		JSONObject item = new JSONObject();

		// ---- 必填字段：标准 M_Product 列，可直接取值 ----
		item.put("materialCode", product.getValue()); // Value -> 物料编码
		item.put("materialName", product.getName()); // Name -> 物料名称

		int uomId = product.getC_UOM_ID();
		String uomSymbol = uomId > 0 ? MUOM.get(product.getCtx(), uomId).getUOMSymbol() : "";
		item.put("materialUnit", uomSymbol); // 计量单位，取UOM符号；如需用Name请改getName()

		// materialType 是WMS侧的物料分类枚举(10原材/20成品/30辅料/40在制品/50半成品/90空托/100虚拟物料/110异常物料/120虚拟容器)，
		String materialType = product.get_ValueAsString("MaterialType");
		// 行政物资不需要同步给WMS，直接返回false，调用方据此跳过  
	    if ("MT04".equals(materialType)) {  
	        return null;  
	    }  
		item.put("materialType", resolveWmsMaterialType(materialType)); 

		// ---- 非必填字段：优先复用ProductValueCallout中已确认存在的自定义列 ----
		String spe = product.get_ValueAsString("Specification"); // 规格，自定义列，已在ProductValueCallout中使用
		if (spe != null)
			item.put("materialSpe", spe);

		BigDecimal length = getBD(product, "Length");
		BigDecimal width = getBD(product, "Width");
		BigDecimal height = getBD(product, "Height");
		BigDecimal thickness = getBD(product, "Thickness");
		if (length != null)
			item.put("length", length);
		if (width != null)
			item.put("width", width);
		if (height != null)
			item.put("height", height);
		if (thickness != null)
			item.put("thickness", thickness);

		// 毛重：标准 M_Product.Weight 列可直接使用
		BigDecimal weight = getBD(product, "WeightGross");
		if (weight != null)
			item.put("grossWeight", weight);

		// 净重：标准表无对应列，TODO: 核实自定义列名后替换，例如 get_ValueAsBigDecimal("NetWeight")
		BigDecimal netWeight = getBD(product, "WeightNet");
		if (netWeight != null)
			item.put("netWeight", netWeight);

		// 保质期(天)
		Integer validDays = (Integer) product.get_Value("GuaranteeDays");
		if (validDays != null)
			item.put("validDays", validDays);
		
		// 到期提醒(天)：无对应列，
//		Integer expireDay = (Integer) product.get_Value("ExpireDay");
//		if (expireDay != null)
//			item.put("expireDay", expireDay);

		// 备注：直接用标准 Description 列
		String remark = product.getDescription();
		if (remark != null)
			item.put("remark", remark);

		// 标准托数量：标准 M_Product.UnitsPerPallet 列可直接使用（名称、语义均吻合）
		BigDecimal oddNumber = product.getUnitsPerPallet();
		if (oddNumber != null)
			item.put("oddNumber", oddNumber);

		// 物料类别：TODO: 核实是否直接用 M_Product_Category_ID，还是需要映射成WMS侧类别编码
		int categoryId = product.getM_Product_Category_ID();
		if (categoryId > 0)
			item.put("materialCategory", categoryId);
		
		JSONArray arr = new JSONArray();
		arr.put(item);
		return arr.toString();
	}

	private String buildBPartnerPayload(JSONObject body, MBPartner bp) {
		// TODO: 业务伙伴固定字段
		return body.toString();
	}

	private String buildResourcePayload(JSONObject body, X_S_Resource resource) {
		// TODO: 机台/设备固定字段
		return body.toString();
	}

	private String buildInOutPayload(JSONObject body, MInOut inout) {
		// TODO: 出入库单头+明细固定字段
		return body.toString();
	}

	private String buildPPOrderPayload(JSONObject body, I_PP_Order order) {
		// TODO: 工单固定字段
		return body.toString();
	}

	@Override
	public String resolveApiUrl(String businessType, String eventType) {
		String prefix = HFSysConfigEnum.WMS_API_URL_PREFIX.getValue(Env.getAD_Client_ID(Env.getCtx()));
		String path = API_PATH_MAP.get(businessType);
		if (prefix == null || prefix.trim().isEmpty()) {
			throw new IllegalStateException("WMS_API_URL_PREFIX 未在 AD_SysConfig 中配置");
		}
		if (path == null) {
			throw new IllegalStateException("WmsSystemAdapter不支持的businessType: " + businessType);
		}
		return prefix + path;
	}

	@Override
	public ExtSyncResult send(String apiUrl, String request) throws Exception {
		Map<String, String> headers = new HashMap<>();
		// TODO: 若WMS要求鉴权(Token/签名等)，在此处补充：
		// headers.put("Authorization", "Bearer " + WmsTokenUtils.getValidToken());

		String rawResponse = HttpClientUtils.post(apiUrl, request, headers);
		return parseWmsResult(rawResponse);
	}

	/**
	 * WMS自己的成功/失败约定：响应体 {"code":"0"/"1", "msg":"..."}，"0"=成功，"1"=失败。
	 * 这个约定只在WmsSystemAdapter内部生效，其他系统的Adapter有各自独立的解析实现，互不影响。
	 */
	private ExtSyncResult parseWmsResult(String rawResponse) {
		if (rawResponse == null || rawResponse.trim().isEmpty()) {
			// 空响应视为异常场景，交给上层按网络异常处理更合适，这里也可以直接判失败
			return ExtSyncResult.failure(null, "WMS返回空响应", rawResponse);
		}
		try {
			JSONObject json = new JSONObject(rawResponse);
			String code = json.optString("code", "1");
			String msg = json.optString("msg", "");
			if ("0".equals(code)) {
				return ExtSyncResult.success(code, rawResponse);
			}
			return ExtSyncResult.failure(code, msg, rawResponse);
		} catch (Exception e) {
			// 响应体不是预期的JSON格式，视为业务失败并记录原始内容，方便排查WMS接口是否变更
			return ExtSyncResult.failure("-1", "WMS响应解析失败: " + e.getMessage(), rawResponse);
		}
	}

	private BigDecimal getBD(MProduct product, String columnName) {
		Object v = product.get_Value(columnName);
		return v instanceof BigDecimal ? (BigDecimal) v : null;
	}
	
	/**
	 * 将本系统 M_Product.MaterialType 转换为 WMS 要求的 materialType 整数枚举。
	 * 
	 * @throws IllegalStateException 当映射关系未配置时，避免把语义错误/无意义的默认值推给WMS，
	 *                               调用方(buildProductPayload)应捕获该异常并按"跳过该字段/该条同步"处理，
	 *                               同时通过日志/告警提醒需要补充映射配置。
	 */
	private Integer resolveWmsMaterialType(String materialType) {
		if (materialType == null || materialType.trim().isEmpty()) {
			throw new IllegalStateException("物料[MaterialType]为空，无法映射WMS的materialType");
		}
		Integer wmsType = MATERIAL_TYPE_MAP.get(materialType);
		if (wmsType == null) {
			throw new IllegalStateException("本系统物料分类[" + materialType + "]未配置对应的WMS materialType映射，"
					+ "请在 WmsSystemAdapter.MATERIAL_TYPE_MAP 中补充配置后重试");
		}
		return wmsType;
	}
}