package com.trekglobal.idempiere.rest.api.v1.resource.hoifu;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.compiere.util.CLogger;
import org.compiere.util.DB;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.trekglobal.idempiere.rest.api.json.ResponseUtils;
import com.trekglobal.idempiere.rest.api.v1.resource.impl.ModelResourceImpl;

public class ProductResourceImpl implements ProductResource {
	private static final CLogger log = CLogger.getCLogger(ProductResourceImpl.class);

	@Override
	public Response getProductsWithStock(String details, String filter, String order, String select, int top, int skip,
			String validationRuleID, String context, String showsql, String label, String showlabel) {
		try {
			// 复用现有的批量查询逻辑
			ModelResourceImpl productResource = new ModelResourceImpl();
			Response productsResponse = productResource.getPOs("M_Product", details, filter, order, select, top, skip,
					validationRuleID, context, showsql, label, showlabel);

			if (productsResponse.getStatus() != 200) {
				return productsResponse;
			}

			// 解析产品列表
			JsonObject responseJson = new Gson().fromJson(productsResponse.getEntity().toString(), JsonObject.class);
			JsonArray records = responseJson.getAsJsonArray("records");

			// 批量查询库存汇总
			Set<Integer> productIds = new HashSet<>();
			for (JsonElement record : records) {
				productIds.add(record.getAsJsonObject().get("id").getAsInt());
			}

			Map<Integer, BigDecimal> stockMap = getQtyOnHandSumMap(productIds);

			// 添加库存信息到每个产品
			for (JsonElement record : records) {
				JsonObject product = record.getAsJsonObject();
				int productId = product.get("id").getAsInt();
				product.addProperty("QtyOnHandSum", stockMap.getOrDefault(productId, BigDecimal.ZERO));
			}

			return Response.ok(responseJson.toString()).build();
		} catch (Exception ex) {
			return ResponseUtils.getResponseErrorFromException(ex, "GET Error");
		}
	}
	
	@Override
	public Response material(String code) {
		JsonObject result = new JsonObject();
		result.addProperty("type", "0201");
		result.addProperty("length", 2303);
		result.addProperty("width", 1118);
		result.addProperty("height", 1110);
		result.addProperty("unit", "mm");
		result.addProperty("glueWidth", 35);
		result.addProperty("flapRatio", 0.5);
		return Response.ok(result.toString()).build();
	}

	private Map<Integer, BigDecimal> getQtyOnHandSumMap(Set<Integer> productIds) {
		Map<Integer, BigDecimal> result = new HashMap<>();
		if (productIds.isEmpty())
			return result;

		String sql = "SELECT M_Product_ID, COALESCE(SUM(QtyOnHand), 0) as QtySum " + "FROM M_StorageOnHand "
				+ "WHERE M_Product_ID IN (" + productIds.stream().map(String::valueOf).collect(Collectors.joining(","))
				+ ") AND IsActive = 'Y' " + "GROUP BY M_Product_ID";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				result.put(rs.getInt(1), rs.getBigDecimal(2));
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, sql, e);
		} finally {
			DB.close(rs, pstmt);
		}

		return result;
	}

}