package com.hoifu.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.compiere.util.CLogger;

/**
 * 打样追踪 — 按工序组（operationclass.Value）定义每组要展示/录入的字段模板。 LinkedHashMap 保证遍历顺序 = 声明顺序
 * = 详情页分组渲染顺序，不依赖数据库自增 ID 排序。
 */
public final class TrackingFieldConfig {

	private static final CLogger log = CLogger.getCLogger(TrackingFieldConfig.class);

	// ── operationclass.Value 常量，避免魔法字符串散落在各处调用代码里 ──────────────
	/** 胶印 */
	public static final String OC_OFFSET_PRINT = "1";
	/** 凹印 */
	public static final String OC_GRAVURE = "2";
	/** 喷码 */
	public static final String OC_INKJET_CODE = "3";
	/** 镭射转印（与丝印共用字段模板） */
	public static final String OC_LASER_TRANSFER = "4";
	/** 单凹 */
	public static final String OC_SINGLE_GRAVURE = "5";
	/** 丝印（与镭射转印共用字段模板） */
	public static final String OC_SILKSCREEN = "6";
	/** 烫金 */
	public static final String OC_HOT_STAMP = "7";
	/** 凹凸压纹 */
	public static final String OC_EMBOSS = "8";
	/** 模切 */
	public static final String OC_DIE_CUT = "9";
	/**
	 * ── 以下编码不在本功能范围内，仅记录以避免误用 ── 10 = 切纸 910 = 清废 911 = 品检 912 = 包装 999 = 委外
	 */

	/**
	 * 工序组字段模板主表。 key：operationclass.Value； value：该工序组追踪弹窗/详情页要渲染的字段列表，List 内顺序即 UI
	 * 列的显示顺序。
	 */
	private static final Map<String, List<TrackingFieldDef>> TEMPLATES = new LinkedHashMap<>();

	static {
		// ── 1. 胶印（效果名称/网纹辊线数/过油版材/灯组/色序/油墨配方，共6个必填字段）──
		TEMPLATES.put(OC_OFFSET_PRINT, Arrays.asList(TrackingFieldDef.text("EffectName", "效果名称", true),
				TrackingFieldDef.text("ScreenLineCount", "网纹辊线数", true),
				TrackingFieldDef.text("OilPlateMaterial", "过油版材", true),
				TrackingFieldDef.text("LightGroup", "灯组", true), TrackingFieldDef.textarea("ColorSequence", "色序", true), // 内容较多，换行输入
				TrackingFieldDef.textarea("InkFormula", "油墨配方", true) // 内容较多，换行输入
		));

		// ── 2. 凹印（胶印6字段 + 油墨粘度 + 烘箱温度，共8个必填字段）──
		TEMPLATES.put(OC_GRAVURE, Arrays.asList(TrackingFieldDef.text("EffectName", "效果名称", true),
				TrackingFieldDef.text("ScreenLineCount", "网纹辊线数", true),
				TrackingFieldDef.text("OilPlateMaterial", "过油版材", true),
				TrackingFieldDef.text("LightGroup", "灯组", true), TrackingFieldDef.textarea("ColorSequence", "色序", true),
				TrackingFieldDef.textarea("InkFormula", "油墨配方", true),
				TrackingFieldDef.textarea("InkViscosity", "油墨粘度", true),
				TrackingFieldDef.textarea("OvenTemperature", "烘箱温度", true)));

		// ── 3. 喷码（效果名称/二维码尺寸/验证码字体及字号，共3个必填字段）──
		TEMPLATES.put(OC_INKJET_CODE,
				Arrays.asList(TrackingFieldDef.text("EffectName", "效果名称", true),
						TrackingFieldDef.text("QRCodeSize", "二维码尺寸", true),
						TrackingFieldDef.text("VerifyCodeFont", "验证码字体及字号", true)));

		// ── 4/6. 丝印、镭射转印共用同一套字段（效果名称/油墨名称及型号/丝网目/灯组，共4个必填字段）
		// 两个 Value 分别指向同一个 List 实例，避免维护两份重复配置导致改一处漏一处 ──
		List<TrackingFieldDef> silkscreenFields = Arrays.asList(TrackingFieldDef.text("EffectName", "效果名称", true),
				TrackingFieldDef.text("InkNameModel", "油墨名称及型号", true),
				TrackingFieldDef.text("ScreenMesh", "丝网目", true), TrackingFieldDef.text("LightGroup", "灯组", true));
		TEMPLATES.put(OC_LASER_TRANSFER, silkscreenFields); // 4 = 镭射转印
		TEMPLATES.put(OC_SILKSCREEN, silkscreenFields); // 6 = 丝印

		// ── 5. 单凹（效果名称/油墨名称及型号/凹版线数/灯组，共4个必填字段）──
		TEMPLATES.put(OC_SINGLE_GRAVURE,
				Arrays.asList(TrackingFieldDef.text("EffectName", "效果名称", true),
						TrackingFieldDef.text("InkNameModel", "油墨名称及型号", true),
						TrackingFieldDef.text("GravureLineCount", "凹版线数", true),
						TrackingFieldDef.text("LightGroup", "灯组", true)));

		// ── 7. 烫金（效果名称/烫金温度/烫金版版号/电化铝型号，共4个必填字段）──
		TEMPLATES.put(OC_HOT_STAMP,
				Arrays.asList(TrackingFieldDef.text("EffectName", "效果名称", true),
						TrackingFieldDef.text("FoilTemperature", "烫金温度", true),
						TrackingFieldDef.text("FoilPlateNo", "烫金版版号", true),
						TrackingFieldDef.text("FoilAluModel", "电化铝型号", true)));

		// ── 8. 凹凸压纹（效果名称/凹凸位置/凹凸版版号，共3个必填字段）──
		TEMPLATES.put(OC_EMBOSS,
				Arrays.asList(TrackingFieldDef.text("EffectName", "效果名称", true),
						TrackingFieldDef.text("EmbossPosition", "凹凸位置", true),
						TrackingFieldDef.text("EmbossPlateNo", "凹凸版版号", true)));

		// ── 9. 模切（效果名称/模切版号/其他参数，共3个必填字段）──
		TEMPLATES.put(OC_DIE_CUT,
				Arrays.asList(TrackingFieldDef.text("EffectName", "效果名称", true),
						TrackingFieldDef.text("DiePlateNo", "模切版号", true),
						TrackingFieldDef.textarea("OtherParam", "其他参数", true)));
	}

	/** 工具类，禁止实例化 */
	private TrackingFieldConfig() {
	}

	/**
	 * 根据 operationclass.Value 取该工序组的字段模板。 未配置时返回空列表并记录警告日志。
	 * 
	 * @param operationClassValue operationclass.Value，如 "1"~"9"
	 * @return 不可变的字段定义列表；未匹配到模板时返回空列表（不会返回 null）
	 */
	public static List<TrackingFieldDef> getFields(String operationClassValue) {
		if (operationClassValue == null) {
			log.warning("operationClassValue 为空，无法获取追踪字段模板");
			return Collections.emptyList();
		}
		List<TrackingFieldDef> fields = TEMPLATES.get(operationClassValue.trim());
		if (fields == null) {
			log.warning("未配置该工序组的追踪字段模板，operationclass.Value=" + operationClassValue);
			return Collections.emptyList();
		}
		// 不可变视图，防止调用方误改共享的静态模板（丝印与镭射转印共用同一 List 实例）
		return Collections.unmodifiableList(fields);
	}

	/** 判断某个 operationclass.Value 是否属于本功能当前支持的编码 */
	public static boolean isSupported(String operationClassValue) {
		return operationClassValue != null && TEMPLATES.containsKey(operationClassValue.trim());
	}


}