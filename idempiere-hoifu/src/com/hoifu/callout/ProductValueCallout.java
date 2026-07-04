package com.hoifu.callout;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.adempiere.base.IColumnCallout;
import org.adempiere.base.annotation.Callout;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.MBPartner;
import org.compiere.model.MProduct;
import org.compiere.model.MProductCategory;
import org.compiere.model.MRefList;
import org.compiere.model.MUOM;

@Callout(tableName = "M_Product", columnName = { "Name", "M_Product_Category_ID", "Specification", "Length", "Width",
		"Height", "BrandCustomer", "C_BPartner_ID", "Grammage", "Color", "Model", "PaperType", "PaperCharacteristics",
		"Thickness", "UoMSize", "C_UOM_ID", "UoMThickness", "Help" })
public class ProductValueCallout implements IColumnCallout {

	// 常量定义
	private static final String SEPARATOR = "_";
	private static final String MULTIPLIER = "*";
	private static final String GRAMMAGE_SUFFIX = "G";
	
	// 物料分类前缀常量
	private static final String CATEGORY_ZZ = "ZZ"; // 纸张
	private static final String CATEGORY_ML = "ML"; // 膜类
	private static final String CATEGORY_CP = "CP"; // 产品配件类
	private static final String CATEGORY_GX = "GX"; // 工序辅料类
	private static final String CATEGORY_HG = "HG"; // 化工
	private static final String CATEGORY_GH = "GH"; // 个性化物料
	private static final String CATEGORY_CB = "CB"; // 产品半成品
	private static final String CATEGORY_ZB = "ZB"; // 纸张半成品
	private static final String CATEGORY_YB = "YB"; // 油墨半成品
	private static final String CATEGORY_FB = "FB"; // 复合半成品

	// 纸张类型常量
	private static final String PAPER_CODE_FLAT = "PM";
	private static final String PAPER_CODE_ROLL = "PR";
	private static final String PAPER_TYPE_FLAT = "平张";

	@Override
	public String start(Properties ctx, int WindowNo, GridTab mTab, GridField mField, Object value, Object oldValue) {
		// 只在新增记录时生成编码
//        if (mTab.getValue("M_Product_ID") != null) {
//            return "";
//        }

		Integer categoryId = (Integer) mTab.getValue("M_Product_Category_ID");
		if (categoryId != null) {
			MProductCategory category = MProductCategory.get(ctx, categoryId);
			if (category != null) {
//				String categoryValue = category.getValue();
//				if (categoryValue.startsWith(CATEGORY_ZZ)) {
//					mTab.setValue(MProduct.COLUMNNAME_Value, buildPaperProductCode(mTab, ctx));
//				} else {
//					if (mField.getColumnName().equals("M_Product_Category_ID")) {
//						// 其他分类使用默认编码规则：分类值 + 时间戳 + 随机数
//						mTab.setValue(MProduct.COLUMNNAME_Value, categoryValue + System.currentTimeMillis() % 10000 + ThreadLocalRandom.current().nextInt(1000, 10000));
//					}
//				}
				
				mTab.setValue(MProduct.COLUMNNAME_Description, generateProductDescription(mTab, category, ctx));
			}
		}

		return "";
	}

	/**
	 * 生成物料描述
	 */
	private String generateProductDescription(GridTab mTab, MProductCategory category, Properties ctx) {
		String categoryValue = category.getValue();
		ProductDescriptionBuilder builder = new ProductDescriptionBuilder(mTab, ctx);

		return switch (getCategoryPrefix(categoryValue)) {
		case CATEGORY_ZZ -> builder.buildPaperDescription();
		case CATEGORY_ML -> builder.buildFilmDescription();
		case CATEGORY_CP -> builder.buildAccessoryDescription();
		case CATEGORY_GX -> builder.buildProcessMaterialDescription();
		case CATEGORY_HG -> builder.buildChemicalDescription();
		case CATEGORY_GH -> builder.buildCustomDescription();
		case CATEGORY_CB -> builder.buildProductSemiFinishedDescription();
		case CATEGORY_ZB -> builder.buildPaperSemiFinishedDescription();
		case CATEGORY_YB -> builder.buildInkSemiFinishedDescription();
		case CATEGORY_FB -> builder.buildCompositeSemiFinishedDescription();
		default -> ""; // SG, KZ, FL, BM, DZ 等分类暂无描述规则
		};
	}

	/**
	 * 生成物料编码
	 */
	private String generateProductValue(GridTab mTab, MProductCategory category, Properties ctx) {
		String categoryValue = category.getValue();

		if (categoryValue.startsWith(CATEGORY_ZZ)) {
			return buildPaperProductCode(mTab, ctx);
		} else {
			// 其他分类使用默认编码规则：分类值 + 时间戳
			return categoryValue + (System.currentTimeMillis() % 10000);
		}
	}

	/**
	 * 构建纸张类物料编码
	 */
	private String buildPaperProductCode(GridTab mTab, Properties ctx) {
		StringBuilder code = new StringBuilder();
		String paperType = (String) mTab.getValue("PaperType");
		BigDecimal grammage = (BigDecimal) mTab.getValue("Grammage");
		String paperCharacteristics = (String) mTab.getValue("PaperCharacteristics");
//		String specification = (String) mTab.getValue("Specification");
		BigDecimal length = (BigDecimal) mTab.getValue("Length");
		BigDecimal width = (BigDecimal) mTab.getValue("Width");
		BigDecimal height = (BigDecimal) mTab.getValue("Height");
		Integer cBPartnerId = (Integer) mTab.getValue("C_BPartner_ID");
		// 纸张类型
		if (paperType != null && !paperType.isEmpty()) {
			code.append(paperType.contains(PAPER_TYPE_FLAT) ? PAPER_CODE_FLAT : PAPER_CODE_ROLL);
		}

		// 克重
		if (grammage != null && grammage.compareTo(BigDecimal.ZERO) > 0) {
			code.append(grammage);
		}

		// 纸张特性
		appendIfNotEmpty(code, paperCharacteristics);

		// 长宽高
		if (Objects.nonNull(length) && length.compareTo(BigDecimal.ZERO) > 0) {
			code.append(length);
		}
		if (Objects.nonNull(width) && width.compareTo(BigDecimal.ZERO) > 0) {
			code.append(width);
		}
		if (Objects.nonNull(height) && height.compareTo(BigDecimal.ZERO) > 0) {
			code.append(height);
		}

		// 供应商简称
		String partnerName = getPartnerName(ctx, cBPartnerId);
		appendIfNotEmpty(code, partnerName);

		return code.toString();
	}

	/**
	 * 产品字段记录类 - 使用 JDK 17 record
	 */
	private record ProductFields(String name, String specification, String brand, String color, String model,
			String help, BigDecimal length, BigDecimal width, BigDecimal height, BigDecimal grammage,
			BigDecimal thickness, Integer cUomID, String uomSize, String uomThickness, MRefList brandRefList,
			String partnerName, String uom) {
		public ProductFields(GridTab mTab, Properties ctx) {
			this((String) mTab.getValue("Name"), (String) mTab.getValue("Specification"),
					(String) mTab.getValue("BrandCustomer"), getColorName(ctx, (String) mTab.getValue("Color")),
					(String) mTab.getValue("Model"), (String) mTab.getValue("Help"),
					(BigDecimal) mTab.getValue("Length"), (BigDecimal) mTab.getValue("Width"),
					(BigDecimal) mTab.getValue("Height"), (BigDecimal) mTab.getValue("Grammage"),
					(BigDecimal) mTab.getValue("Thickness"), (Integer) mTab.getValue("C_UOM_ID"),
					(String) mTab.getValue("UoMSize"), (String) mTab.getValue("UoMThickness"),
					getBrandRefList(ctx, (String) mTab.getValue("BrandCustomer")),
					getPartnerName(ctx, (Integer) mTab.getValue("C_BPartner_ID")),
					getUomName(ctx, (Integer) mTab.getValue("C_UOM_ID")));
		}

		private static String getColorName(Properties ctx, String color) {
			if (color != null && !color.isEmpty()) {
				MRefList colorRefList = MRefList.get(ctx, 1000003, color, null);
				if (colorRefList != null) {
					return colorRefList.getName();
				}
			}
			return color;
		}

		private static MRefList getBrandRefList(Properties ctx, String brand) {
			if (brand != null && !brand.isEmpty()) {
				return MRefList.get(ctx, 1000005, brand, null);
			}
			return null;
		}

		private static String getPartnerName(Properties ctx, Integer cBPartnerId) {
			if (cBPartnerId != null) {
				MBPartner mbPartner = MBPartner.get(ctx, cBPartnerId, null);
				if (mbPartner != null) {
					return mbPartner.get_ValueAsString("Name2");
				}
			}
			return null;
		}

		private static String getUomName(Properties ctx, Integer cUomID) {
			if (cUomID != null) {
				MUOM muom = MUOM.get(ctx, cUomID);
				if (muom != null) {
					return muom.get_ValueAsString("Name");
				}
			}
			return null;
		}
	}

	/**
	 * 产品描述构建器
	 */
	private static class ProductDescriptionBuilder {
		private final ProductFields fields;
		private final StringBuilder desc;

		public ProductDescriptionBuilder(GridTab mTab, Properties ctx) {
			this.fields = new ProductFields(mTab, ctx);
			this.desc = new StringBuilder();
		}

		public String buildPaperDescription() {
			appendGrammage().appendLength().appendWidth().appendHeight().appendUomSize().appendUom().appendThickness()
					.appendBrand().appendPartner();
			return desc.toString();
		}

		public String buildFilmDescription() {
			appendSpecification().appendThickness().appendBrand().appendPartner();
			return desc.toString();
		}

		public String buildAccessoryDescription() {
			appendSpecification().appendName().appendColor().appendBrand().appendPartner();
			return desc.toString();
		}

		public String buildProcessMaterialDescription() {
			appendLength().appendWidth().appendHeight().appendUomSize().appendName().appendBrand().appendPartner()
					.appendHelp();
			return desc.toString();
		}

		public String buildChemicalDescription() {
			appendModel().appendColor().appendSpecification().appendBrand().appendPartner();
			return desc.toString();
		}

		public String buildCustomDescription() {
			appendModel().appendSpecification().appendName().appendColor().appendBrand().appendPartner();
			return desc.toString();
		}

		public String buildProductSemiFinishedDescription() {
			appendName().appendHelp();
			return desc.toString();
		}

		public String buildPaperSemiFinishedDescription() {
			appendGrammage().appendLength().appendWidth().appendHeight().appendUomSize().appendUom().appendBrand()
					.appendPartner();
			return desc.toString();
		}

		public String buildInkSemiFinishedDescription() {
			appendModel().appendName().appendColor().appendBrand().appendPartner().appendHelp();
			return desc.toString();
		}

		public String buildCompositeSemiFinishedDescription() {
			appendGrammage().appendLength().appendWidth().appendHeight().appendUomSize().appendUom().appendThickness()
					.appendBrand().appendPartner();
			return desc.toString();
		}

		// 构建方法 - 保持与原始逻辑一致
		private ProductDescriptionBuilder appendGrammage() {
			if (fields.grammage != null && fields.grammage.compareTo(BigDecimal.ZERO) > 0) {
				appendField(fields.grammage + GRAMMAGE_SUFFIX);
			}
			return this;
		}

		private ProductDescriptionBuilder appendLength() {
			if (fields.length != null && fields.length.compareTo(BigDecimal.ZERO) > 0) {
				appendField(fields.length.toString());
			}
			return this;
		}

		private ProductDescriptionBuilder appendWidth() {
			if (fields.width != null && fields.width.compareTo(BigDecimal.ZERO) > 0) {
				if (desc.length() > 0)
					desc.append(MULTIPLIER);
				desc.append(fields.width);
			}
			return this;
		}

		private ProductDescriptionBuilder appendHeight() {
			if (fields.height != null && fields.height.compareTo(BigDecimal.ZERO) > 0) {
				if (desc.length() > 0)
					desc.append(MULTIPLIER);
				desc.append(fields.height);
			}
			return this;
		}

		private ProductDescriptionBuilder appendUomSize() {
			if ((fields.height != null && fields.height.compareTo(BigDecimal.ZERO) > 0)
					|| (fields.width != null && fields.width.compareTo(BigDecimal.ZERO) > 0)
					|| (fields.length != null && fields.length.compareTo(BigDecimal.ZERO) > 0)) {
				if (fields.uomSize != null && !fields.uomSize.isEmpty()) {
					desc.append(fields.uomSize);
				}
			}
			return this;
		}

		private ProductDescriptionBuilder appendUom() {
			if (fields.uom != null) {
				appendField(fields.uom);
			}
			return this;
		}

		private ProductDescriptionBuilder appendThickness() {
			if (fields.thickness != null && fields.thickness.compareTo(BigDecimal.ZERO) > 0) {
				appendField(fields.thickness.toString());
				if (fields.uomThickness != null && !fields.uomThickness.isEmpty()) {
					desc.append(fields.uomThickness);
				}
			}
			return this;
		}

		private ProductDescriptionBuilder appendBrand() {
			if (fields.brandRefList != null) {
				appendField(fields.brandRefList.getName());
			}
			return this;
		}

		private ProductDescriptionBuilder appendPartner() {
			if (fields.partnerName != null && !fields.partnerName.isEmpty()) {
				appendField(fields.partnerName);
			}
			return this;
		}

		private ProductDescriptionBuilder appendSpecification() {
			if (fields.specification != null && !fields.specification.isEmpty()) {
				appendField(fields.specification);
			}
			return this;
		}

		private ProductDescriptionBuilder appendName() {
			if (fields.name != null && !fields.name.isEmpty()) {
				appendField(fields.name);
			}
			return this;
		}

		private ProductDescriptionBuilder appendColor() {
			if (fields.color != null && !fields.color.isEmpty()) {
				appendField(fields.color);
			}
			return this;
		}

		private ProductDescriptionBuilder appendModel() {
			if (fields.model != null && !fields.model.isEmpty()) {
				if (desc.length() == 0) {
					desc.append(fields.model);
				} else {
					appendField(fields.model);
				}
			}
			return this;
		}

		private ProductDescriptionBuilder appendHelp() {
			if (fields.help != null && !fields.help.isEmpty()) {
				appendField(fields.help);
			}
			return this;
		}

		private void appendField(String value) {
			if (value != null && !value.isEmpty()) {
				if (desc.length() > 0) {
					desc.append(SEPARATOR);
				}
				desc.append(value);
			}
		}
	}

	// 工具方法
	private String getPartnerName(Properties ctx, Integer cBPartnerId) {
		return ProductFields.getPartnerName(ctx, cBPartnerId);
	}

	private void appendIfNotEmpty(StringBuilder sb, String value) {
		if (value != null && !value.isEmpty()) {
			sb.append(value);
		}
	}

	private String getCategoryPrefix(String categoryValue) {
		return categoryValue != null && categoryValue.length() >= 2 ? categoryValue.substring(0, 2) : "";
	}
}