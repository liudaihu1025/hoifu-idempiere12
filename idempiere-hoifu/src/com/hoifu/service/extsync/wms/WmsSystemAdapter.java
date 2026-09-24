package com.hoifu.service.extsync.wms;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.compiere.model.MBPartner;
import org.compiere.model.MBPartnerLocation;
import org.compiere.model.MInOut;
import org.compiere.model.MLocation;
import org.compiere.model.MProduct;
import org.compiere.model.MUOM;
import org.compiere.model.MUser;
import org.compiere.model.PO;
import org.compiere.model.X_S_Resource;
import org.compiere.util.Env;
import org.eevolution.model.I_PP_Order;
import org.json.JSONArray;
import org.json.JSONObject;
import com.hoifu.enums.HFSysConfigEnum;
import com.hoifu.model.MInOutRequisition;
import com.hoifu.service.extsync.ExtSyncResult;
import com.hoifu.service.extsync.IExternalSystemAdapter;
import com.hoifu.utils.HttpClientUtils;

import org.compiere.model.MWarehouse;  
import org.compiere.model.MLocator;

/**
 * WMS系统同步数据适配器实现
 * @ClassName: WmsSystemAdapter
 * @author ldh
 * @date 2026年8月19日
 */
public class WmsSystemAdapter implements IExternalSystemAdapter {

	// 类成员变量区域新增  
	private final WmsInOutRequisitionService orderService = new WmsInOutRequisitionService();
	
	public static final String SYSTEM_TYPE = "WMS";

	public static final String BT_PRODUCT = "PRODUCT";
	public static final String BT_BPARTNER = "BPARTNER";
	public static final String BT_BPARTNER_LOCATION = "BPARTNER_LOCATION";
	public static final String BT_RESOURCE = "RESOURCE";
	public static final String BT_INOUT = "INOUT";
	public static final String BT_PPORDER = "PPORDER";
	public static final String BT_WAREHOUSE = "WAREHOUSE"; // M_Warehouse  
	public static final String BT_LOCATOR = "LOCATOR";     // M_Locator
	public static final String BT_STOCK_IN = "STOCK_IN";   // 入库单，对应 MInOut.isSOTrx()==false（采购收货）  
	public static final String BT_STOCK_OUT = "STOCK_OUT"; // 出库单，对应 MInOut.isSOTrx()==true（销售发货）

	
	// 支持的业务类型集合，独立维护
	private static final Set<String> SUPPORTED_BUSINESS_TYPES = new HashSet<>(Arrays.asList(  
	    BT_PRODUCT,
	    BT_BPARTNER,
	    BT_BPARTNER_LOCATION,
	    BT_WAREHOUSE, 
	    BT_LOCATOR, 
	    BT_STOCK_IN, 
	    BT_STOCK_OUT
	));
	
	private static final String EVENT_ANY = "ANY"; // 未单独配置eventType时的兜底路径  
	  
	private static final Map<String, String> API_PATH_MAP = new HashMap<>();  
	static {  
	    API_PATH_MAP.put(key(BT_PRODUCT, EVENT_ANY), "/erp/material/add");  
	    API_PATH_MAP.put(key(BT_PRODUCT, EVENT_DELETE), "/erp/material/delete");  
	  
	    API_PATH_MAP.put(key(BT_BPARTNER, EVENT_ANY), "/erp/partner/add");  
	    API_PATH_MAP.put(key(BT_BPARTNER, EVENT_DELETE), "/erp/partner/delete");  
	  
	    API_PATH_MAP.put(key(BT_BPARTNER_LOCATION, EVENT_ANY), "/erp/partner/add");  
	    API_PATH_MAP.put(key(BT_BPARTNER_LOCATION, EVENT_DELETE), "/erp/partner/add");  
	    
	    API_PATH_MAP.put(key(BT_WAREHOUSE, EVENT_ANY), "/erp/warehouse/add");  
	    API_PATH_MAP.put(key(BT_WAREHOUSE, EVENT_DELETE), "/erp/warehouse/delete");  
	    API_PATH_MAP.put(key(BT_LOCATOR, EVENT_ANY), "/erp/warehouse/add");  
	    API_PATH_MAP.put(key(BT_LOCATOR, EVENT_DELETE), "/erp/warehouse/delete");  
	  
	    API_PATH_MAP.put(key(BT_STOCK_IN, EVENT_COMPLETE), "/erp/stockIn/receive");
	    API_PATH_MAP.put(key(BT_STOCK_IN, EVENT_VOID), "/erp/stockIn/cancel");
	  
	    API_PATH_MAP.put(key(BT_STOCK_OUT, EVENT_COMPLETE), "/erp/stockOut/deliveryAdd");
	    API_PATH_MAP.put(key(BT_STOCK_OUT, EVENT_VOID), "/erp/stockOut/cancel");

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
	public boolean supports(PO po, String businessType, String eventType) {
		if (!supports(businessType)) // 复用原有 API_PATH_MAP 判断
			return false;
		if (!EVENT_UPDATE.equals(eventType)) // INSERT/DELETE不做字段级过滤，直接放行
			return true;
		switch (businessType) {
			case BT_BPARTNER:
				return hasRelevantBPartnerChange((MBPartner) po);
			case BT_BPARTNER_LOCATION:
				return hasRelevantBPartnerLocationChange((MBPartnerLocation) po);
			case BT_PRODUCT:
				return hasRelevantProductChange((MProduct) po);
			case BT_WAREHOUSE:
				return hasRelevantWarehouseChange((MWarehouse) po);
			case BT_LOCATOR:
				return hasRelevantLocatorChange((MLocator) po);
			default:
				return true;
		}
	}

	@Override
	public String buildRequest(PO po, String businessType, String eventType, Map<String, Object> syncContext) {
		switch (businessType) {
			case BT_PRODUCT:
				return buildProductPayload(eventType, (MProduct) po, syncContext);
			case BT_BPARTNER:
				return buildBPartnerPayload(eventType, (MBPartner) po, syncContext);
			case BT_BPARTNER_LOCATION:
				return buildBPartnerLocationPayload(eventType, (MBPartnerLocation) po, syncContext);
			case BT_RESOURCE:
				return buildResourcePayload(eventType, (X_S_Resource) po, syncContext);
			case BT_INOUT:
				return buildInOutPayload(eventType, (MInOut) po, syncContext);
			case BT_PPORDER:
				return buildPPOrderPayload(eventType, (I_PP_Order) po, syncContext);
			case BT_WAREHOUSE:
				return buildWarehousePayload(eventType, (MWarehouse) po, syncContext);
			case BT_LOCATOR:
				return buildLocatorPayload(eventType, (MLocator) po, syncContext);
			case BT_STOCK_IN:
				return WmsInOutRequisitionService.buildInOutRequisitionPayload(eventType, (MInOutRequisition) po, syncContext);
			case BT_STOCK_OUT:
				return WmsInOutRequisitionService.buildInOutRequisitionPayload(eventType, (MInOutRequisition) po, syncContext);
			default:
				throw new IllegalArgumentException("WmsSystemAdapter不支持的业务类型: " + businessType);
		}
	}

	private String buildBPartnerLocationPayload(String eventType, MBPartnerLocation po, Map<String, Object> syncContext) {
		int bpId = (int)syncContext.get(MBPartner.COLUMNNAME_C_BPartner_ID);
	    if (bpId <= 0)  
	        return null; 
	    MBPartner parentBp = MBPartner.get(Env.getCtx(), bpId);  
	    if (parentBp == null || parentBp.get_ID() <= 0)  
	        return null;  
	    return buildBPartnerPayload(EVENT_UPDATE, parentBp, null);  
	}

	private String buildProductPayload(String eventType, MProduct product, Map<String, Object> syncContext) {
	    // ---- DELETE场景：记录已物理删除
	    if (EVENT_DELETE.equals(eventType)) {  
	        return buildDeletePayload(syncContext);  
	    }  
	  
	    
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
	  
	private String buildBPartnerPayload(String eventType, MBPartner bp, Map<String, Object> syncContext) {  
	    if (EVENT_DELETE.equals(eventType)) {  
	        return buildDeletePayload(syncContext);  
	    }
	    
	    JSONObject item = new JSONObject();  
	  
	    // ---- 标准 C_BPartner 字段 ----  
	    item.put("partnerCode", bp.getValue());          // C_BPartner.Value  
	    item.put("partnerName", bp.getName());            // C_BPartner.Name  
	    item.put("partnerType", 1);
	    item.put("partnerShortname", bp.getName2());       // C_BPartner.Name2  
	    item.put("partnersState", bp.isActive() ? 1 : 0);   // C_BPartner.IsActive -> 0停用/1启用  
	    String remark = bp.getDescription();  
	    if (remark != null)  
	        item.put("remark", remark);                    // C_BPartner.Description  
	  
	    // TODO: partnerType / partnerLevel 未提供明确的字段映射关系，需与业务/WMS对接方确认后再补充  
	  
	    // ---- 主联系人：取该 BPartner 下第一个 AD_User；若一条AD_User都没有，退回用 C_BPartner.Name ----  
	    MUser[] users = bp.getContacts(true);  
	    String primaryContact;  
	    String primaryPhone = null;  
	    if (users != null && users.length > 0) {  
	        primaryContact = users[0].getName();   // AD_User.Name  
	        primaryPhone = users[0].getPhone();     // AD_User.Phone  
	    } else {  
	        primaryContact = bp.getName();          // 兜底：C_BPartner.Name  
	    }  
	    item.put("contact", primaryContact);  
	    if (primaryPhone != null)  
	        item.put("phone", primaryPhone);  
	  
	    // ---- 地址列表：遍历 C_BPartner_Location，contact统一取上面算好的 primaryContact ----  
	    JSONArray addrArr = new JSONArray();  
	    MBPartnerLocation[] locations = bp.getLocations(true);  
	    if (locations != null) {  
	        for (MBPartnerLocation bpl : locations) {  
	            JSONObject addr = new JSONObject();  
	            addr.put("contact", primaryContact);      // 统一取主数据上的contact，而不是 C_BPartner_Location.Name  
	            addr.put("phone", bpl.getPhone());          // C_BPartner_Location.Phone  
	  
	            MLocation loc = bpl.getLocation(false);  
	            if (loc != null) {  
	                StringBuilder sb = new StringBuilder();  
	                appendIfNotEmpty(sb, loc.getRegionName(true));  
	                appendIfNotEmpty(sb, loc.getCity());  
	                appendIfNotEmpty(sb, loc.getAddress4());  
	                appendIfNotEmpty(sb, loc.getAddress3());  
	                appendIfNotEmpty(sb, loc.getAddress2());  
	                appendIfNotEmpty(sb, loc.getAddress1());  
	                addr.put("address", sb.toString());  
	            }  
	  

	            addr.put("defaultFlag", 0);  
	  
	            addrArr.put(addr);  
	        }  
	    }  
	    item.put("partnerAddrList", addrArr);  
	  
	    JSONArray arr = new JSONArray();  
	    arr.put(item);  
	    return arr.toString();  
	}  
	  
	
	private String buildWarehousePayload(String eventType, MWarehouse wh, Map<String, Object> syncContext) {  
	    // ---- DELETE场景：记录已物理删除
	    if (EVENT_DELETE.equals(eventType)) {  
	        return buildDeletePayload(syncContext);  
	    }  
		JSONObject item = new JSONObject();  
		item.put("warehouseCode", wh.getValue());          // M_Warehouse.Value  
		item.put("warehouseName", wh.getName());            // M_Warehouse.Name  
		item.put("warehouseModel", 1);                       // 1=仓库  
		String remark = wh.getDescription();  
		if (remark != null)  
			item.put("remark", remark);                     // M_Warehouse.Description  
		item.put("parentWarehouseCode", JSONObject.NULL);    // 仓库无父级，固定null  
		// 按需求：M_Warehouse 层不传 warehouseSort 字段  
	  
		JSONArray arr = new JSONArray();  
		arr.put(item);  
		return arr.toString();  
	}  
	  
	private String buildLocatorPayload(String eventType, MLocator loc, Map<String, Object> syncContext) {  
	    // ---- DELETE场景：记录已物理删除
	    if (EVENT_DELETE.equals(eventType)) {  
	        return buildDeletePayload(syncContext);  
	    }  
		JSONObject item = new JSONObject();  
		item.put("warehouseCode", loc.getValue());          // M_Locator.Value  
		item.put("warehouseName", loc.getValue());           // M_Locator无Name列，同样取Value  
		item.put("warehouseModel", 2);                       // 2=库位  
		item.put("warehouseSort", loc.getPriorityNo());      // M_Locator.PriorityNo  
	  
		int whId = loc.getM_Warehouse_ID();  
		if (whId > 0) {  
			MWarehouse parentWh = MWarehouse.get(loc.getCtx(), whId);  
			item.put("parentWarehouseCode", parentWh != null ? parentWh.getValue() : JSONObject.NULL);  
		} else {  
			item.put("parentWarehouseCode", JSONObject.NULL);  
		}  
		// warehouseType/floorDescr/materialCategoryList 暂不实现，按需求先不传  
	  
		JSONArray arr = new JSONArray();  
		arr.put(item);  
		return arr.toString();  
	}
	
	private void appendIfNotEmpty(StringBuilder sb, String s) {  
	    if (s != null && !s.trim().isEmpty())  
	        sb.append(s);  
	}
	  

	private String buildResourcePayload(String eventType, X_S_Resource resource, Map<String, Object> syncContext) {
		// TODO: 机台/设备固定字段
		return eventType.toString();
	}

	private String buildInOutPayload(String eventType, MInOut inout, Map<String, Object> syncContext) {
		// TODO: 出入库单头+明细固定字段
		return eventType.toString();
	}

	private String buildPPOrderPayload(String eventType, I_PP_Order order, Map<String, Object> syncContext) {
		// TODO: 工单固定字段
		return eventType.toString();
	}


	/**  
	 * @return 构造好的删除请求JSON字符串；若recordCode缺失，返回null，交由调用方按跳过/失败处理  
	 */  
	private String buildDeletePayload(Map<String, Object> syncContext) {  
	    Object recordCode = syncContext != null ? syncContext.get("recordCode") : null;  
	    if (recordCode == null || recordCode.toString().trim().isEmpty()) {  
	        // 没有可用的编码，无法构造删除请求，交给调用方按跳过/失败处理  
	        return null;  
	    }  
	    JSONArray arr = new JSONArray();  
	    arr.put(recordCode.toString()); // WMS要求DELETE直接传编码(Value/DocumentNo)数组，如 ["MAT20260817001"]  
	    return arr.toString();  
	}
	

	/** 只同步 buildProductPayload 中实际用到的字段发生变化的记录（或新增记录） */  
	private boolean hasRelevantProductChange(MProduct product) {  
	    if (product.is_new())  
	        return true;  
	    return product.is_ValueChanged(MProduct.COLUMNNAME_Value)  
	        || product.is_ValueChanged(MProduct.COLUMNNAME_Name)  
	        || product.is_ValueChanged(MProduct.COLUMNNAME_C_UOM_ID)  
	        || product.is_ValueChanged("MaterialType")  
	        || product.is_ValueChanged("Specification")  
	        || product.is_ValueChanged("Length")  
	        || product.is_ValueChanged("Width")  
	        || product.is_ValueChanged("Height")  
	        || product.is_ValueChanged("Thickness")  
	        || product.is_ValueChanged("WeightGross")  
	        || product.is_ValueChanged("WeightNet")  
	        || product.is_ValueChanged("GuaranteeDays")  
	        || product.is_ValueChanged(MProduct.COLUMNNAME_Description)  
	        || product.is_ValueChanged(MProduct.COLUMNNAME_UnitsPerPallet)  
	        || product.is_ValueChanged(MProduct.COLUMNNAME_M_Product_Category_ID);  
	}  
	  
	/** 只同步 buildWarehousePayload 中实际用到的字段发生变化的记录（或新增记录） */  
	private boolean hasRelevantWarehouseChange(MWarehouse wh) {  
	    if (wh.is_new())  
	        return true;  
	    return wh.is_ValueChanged(MWarehouse.COLUMNNAME_Value)  
	        || wh.is_ValueChanged(MWarehouse.COLUMNNAME_Name)  
	        || wh.is_ValueChanged(MWarehouse.COLUMNNAME_Description);  
	}  
	  
	/** 只同步 buildLocatorPayload 中实际用到的字段发生变化的记录（或新增记录） */  
	private boolean hasRelevantLocatorChange(MLocator loc) {  
	    if (loc.is_new())  
	        return true;  
	    return loc.is_ValueChanged(MLocator.COLUMNNAME_Value)  
	        || loc.is_ValueChanged(MLocator.COLUMNNAME_PriorityNo)  
	        || loc.is_ValueChanged(MLocator.COLUMNNAME_M_Warehouse_ID);  
	}
	
	/**  
	 * 只同步 C_BPartner 上被引用字段发生变化的记录（或新增记录），  
	 * 避免无关字段变更也触发一次WMS同步。
	 */  
	private boolean hasRelevantBPartnerChange(MBPartner bp) {  
	    if (bp.is_new()) {  
	        return true;  
	    }  
	    return bp.is_ValueChanged(MBPartner.COLUMNNAME_Value)  
	        || bp.is_ValueChanged(MBPartner.COLUMNNAME_Name)  
	        || bp.is_ValueChanged(MBPartner.COLUMNNAME_Name2)  
	        || bp.is_ValueChanged(MBPartner.COLUMNNAME_IsActive)  
	        || bp.is_ValueChanged(MBPartner.COLUMNNAME_Description);  
	}  
	
	/**  
	 * 只同步 buildBPartnerLocationPayload 间接用到的字段发生变化的记录（或新增记录）。  
	 * 由于最终会重建父BPartner的整条地址列表，这里覆盖会影响地址内容/联系方式/归属关系的关键列。  
	 */  
	private boolean hasRelevantBPartnerLocationChange(MBPartnerLocation bpl) {  
	    if (bpl.is_new())  
	        return true;  
	    return bpl.is_ValueChanged(MBPartnerLocation.COLUMNNAME_C_BPartner_ID)  
	        || bpl.is_ValueChanged(MBPartnerLocation.COLUMNNAME_C_Location_ID)  
	        || bpl.is_ValueChanged(MBPartnerLocation.COLUMNNAME_Phone)  
	        || bpl.is_ValueChanged(MBPartnerLocation.COLUMNNAME_IsActive);  
	}
	
	@Override  
	public Map<String, Object> captureSyncContext(PO po, String businessType, String eventType) {  
	    if (po == null)  
	        return null;  
	  
	    Map<String, Object> context = new HashMap<>();  
	  
	    // buildBPartnerLocationPayload需要这里预先缓存的父C_BPartner_ID重新加载父对象整体重建payload  
	    if (BT_BPARTNER_LOCATION.equals(businessType) && po instanceof MBPartnerLocation) {  
	        int bpId = ((MBPartnerLocation) po).getC_BPartner_ID();  
	        if (bpId > 0)  
	            context.put(MBPartner.COLUMNNAME_C_BPartner_ID, bpId);  
	        return context.isEmpty() ? null : context;  
	    }  
	  
	    // 优先取 DocumentNo（有单据编号语义的表，如 MInOut/PP_Order），  
	    // 该表没有 DocumentNo 列时 get_ValueAsString 会返回 null，再退回取 Value（如 MProduct/MBPartner）  
	    String recordCode = po.get_ValueAsString("DocumentNo");  
	    if (recordCode == null || recordCode.trim().isEmpty()) {  
	        recordCode = po.get_ValueAsString("Value");  
	    }  
	    if (recordCode != null && !recordCode.trim().isEmpty()) {  
	        context.put("recordCode", recordCode);  
	    }  
	  
	    return context.isEmpty() ? null : context;  
	}
	
	@Override  
	public String resolveApiUrl(String businessType, String eventType) {  
	    String prefix = HFSysConfigEnum.WMS_API_URL_PREFIX.getValue(Env.getAD_Client_ID(Env.getCtx()));  
	    if (prefix == null || prefix.trim().isEmpty()) {  
	        throw new IllegalStateException("WMS_API_URL_PREFIX 未在 AD_SysConfig 中配置");  
	    }  
	    String path = API_PATH_MAP.get(key(businessType, eventType));  
	    if (path == null) {  
	        path = API_PATH_MAP.get(key(businessType, EVENT_ANY)); // 没有单独配置该eventType，退回兜底  
	    }  
	    if (path == null) {  
	        throw new IllegalStateException("WmsSystemAdapter不支持的businessType/eventType组合: "  
	                + businessType + "/" + eventType);  
	    }  
	    return prefix + path;  
	}
	
	  
	private static String key(String businessType, String eventType) {  
	    return businessType + "_" + eventType;  
	}


	@Override
	public ExtSyncResult send(String apiUrl, String request) throws Exception {
		Map<String, String> headers = new HashMap<>();
		// TODO: 若WMS要求鉴权(Token/签名等)，在此处补充：
		// headers.put("Authorization", "Bearer " + WmsTokenUtils.getValidToken());

		String rawResponse = HttpClientUtils.postIgnoreSSL(apiUrl, request, headers);
		//String rawResponse =null;
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

	@Override
	public boolean supports(String businessType) {
		return SUPPORTED_BUSINESS_TYPES.contains(businessType);
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