package com.trekglobal.idempiere.rest.api.v1.resource.hoifu;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.trekglobal.idempiere.rest.api.json.QueryOperators;

@Path("v2/product")
public interface ProductResource {

	@Path("/getProductWithStock")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getProductsWithStock(@QueryParam(QueryOperators.EXPAND) String details,
			@QueryParam(QueryOperators.FILTER) String filter, @QueryParam(QueryOperators.ORDERBY) String order,
			@QueryParam(QueryOperators.SELECT) String select, @QueryParam(QueryOperators.TOP) int top,
			@DefaultValue("0") @QueryParam(QueryOperators.SKIP) int skip,
			@QueryParam(QueryOperators.VALRULE) String validationRuleID,
			@QueryParam(QueryOperators.CONTEXT) String context, @QueryParam(QueryOperators.SHOW_SQL) String showsql,
			@QueryParam(QueryOperators.LABEL) String label, @QueryParam(QueryOperators.SHOW_LABEL) String showlabel);

	/**
	 * 根据产品编码，查询返回纸箱相关属性（箱型计算参数）
	 * @Title: material
	 * @param code
	 * @return
	 * @return Response
	 */
	@Path("/material/{code}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response material(@PathParam("code") String code);
}