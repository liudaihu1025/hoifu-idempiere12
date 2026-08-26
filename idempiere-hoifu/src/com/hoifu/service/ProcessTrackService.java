package com.hoifu.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;

import com.hoifu.config.TrackingFieldConfig;
import com.hoifu.config.TrackingFieldDef;
import com.hoifu.model.MHFPPOrderNodeTracking;

/**
 * 打样工序追踪服务：根据 PP_Order_Node_ID 查询对应的 operationclass 信息（决定弹窗渲染哪套字段）
 * 查询/校验/保存/物理删除 HF_PP_OrderNode_Tracking 追踪记录
 */
public class ProcessTrackService {

	private static final CLogger log = CLogger.getCLogger(ProcessTrackService.class);

	/**
	 * 根据 PP_Order_Node_ID 同时查出 operationclass_ID + Value + Name。
	 */
	public OperationClassInfo getOperationClassInfo(int ppOrderNodeId) {
		if (ppOrderNodeId <= 0)
			return null;

		String sql = "SELECT oc.operationclass_ID, oc.Value, oc.Name " + "FROM PP_Order_Node pon "
				+ "JOIN AD_Routing_Node arn ON arn.AD_Routing_Node_ID = pon.AD_Routing_Node_ID "
				+ "JOIN operationclass oc ON oc.operationclass_ID = arn.operationclass_ID "
				+ "WHERE pon.PP_Order_Node_ID = ? AND pon.AD_Client_ID = ? "
				+ "AND arn.IsActive='Y' AND oc.IsActive='Y'";

		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null); // 只读查询，不在具名事务中
			pstmt.setInt(1, ppOrderNodeId);
			pstmt.setInt(2, Env.getAD_Client_ID(Env.getCtx()));
			rs = pstmt.executeQuery();
			if (rs.next()) {
				return new OperationClassInfo(rs.getInt("operationclass_ID"), rs.getString("Value"),
						rs.getString("Name"));
			}
			log.warning("未能查询到 PP_Order_Node_ID=" + ppOrderNodeId + " 对应的 operationclass 信息");
			return null;
		} catch (Exception e) {
			log.severe("查询工序组信息失败: " + e.getMessage());
			return null;
		} finally {
			DB.close(rs, pstmt); // 统一关闭资源，避免连接泄漏
		}
	}

	/**
	 * 一次性查出 operationclass 全部记录的 ID -> (Value, Name) 映射，
	 * 供详情页按分组渲染标题时使用，避免每个分组循环里各查一次库（N+1 查询）。
	 */
	public Map<Integer, OperationClassInfo> loadAllOperationClass() {
		Map<Integer, OperationClassInfo> map = new HashMap<>();
		String sql = "SELECT operationclass_ID, Value, Name FROM operationclass "
				+ "WHERE IsActive='Y' AND AD_Client_ID=?";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, null);
			pstmt.setInt(1, Env.getAD_Client_ID(Env.getCtx()));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				int id = rs.getInt("operationclass_ID");
				map.put(id, new OperationClassInfo(id, rs.getString("Value"), rs.getString("Name")));
			}
		} catch (Exception e) {
			log.severe("查询工序组字典失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return map;
	}

	/**
	 * 查询某个工单工序（PP_Order_Node_ID）下已录入的所有追踪记录，供编辑弹窗回显。
	 */
	public List<MHFPPOrderNodeTracking> findByOrderNode(Properties ctx, int ppOrderNodeId, String trxName) {
		List<MHFPPOrderNodeTracking> list = new ArrayList<>();
		String sql = "SELECT HF_PP_OrderNode_Tracking_ID FROM HF_PP_OrderNode_Tracking "
				+ "WHERE PP_Order_Node_ID=? AND AD_Client_ID=? " + "ORDER BY HF_PP_OrderNode_Tracking_ID";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, ppOrderNodeId);
			pstmt.setInt(2, Env.getAD_Client_ID(ctx));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(new MHFPPOrderNodeTracking(ctx, rs.getInt(1), trxName));
			}
		} catch (Exception e) {
			log.severe("查询工序追踪记录失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return list;
	}

	/**
	 * 查询某个工单（PP_Order_ID）下所有工序组已录入的追踪记录，供"打样追踪详情"页面分组展示。 带 AD_Client_ID 过滤。
	 */
	public List<MHFPPOrderNodeTracking> findByOrder(Properties ctx, int ppOrderId, String trxName) {
		List<MHFPPOrderNodeTracking> list = new ArrayList<>();
		String sql = "SELECT HF_PP_OrderNode_Tracking_ID FROM HF_PP_OrderNode_Tracking "
				+ "WHERE PP_Order_ID=? AND AD_Client_ID=? " + "ORDER BY HF_PP_OrderNode_Tracking_ID";
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, trxName);
			pstmt.setInt(1, ppOrderId);
			pstmt.setInt(2, Env.getAD_Client_ID(ctx));
			rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(new MHFPPOrderNodeTracking(ctx, rs.getInt(1), trxName));
			}
		} catch (Exception e) {
			log.severe("查询工单追踪记录失败: " + e.getMessage());
		} finally {
			DB.close(rs, pstmt);
		}
		return list;
	}

	/**
	 * 校验一条记录是否满足当前工序组模板的必填项要求，不落库，仅做校验。 供 UI 层"批量保存前先整体校验"时调用，避免部分行保存成功、部分行失败
	 * 导致的不完整数据。
	 * 这里仍使用通用的 get_ValueAsString(columnName)，因为业务字段列表是由 TrackingFieldConfig 按
	 * operationclass.Value 动态决定的，编译期不知道 具体是哪几列，无法使用生成基类的类型安全 getter。
	 */
	public String validate(MHFPPOrderNodeTracking po, String operationClassValue) {
		for (TrackingFieldDef def : TrackingFieldConfig.getFields(operationClassValue)) {
			if (def.mandatory) {
				String v = po.get_ValueAsString(def.columnName);
				if (v == null || v.trim().isEmpty()) {
					return def.label + " 为必填项";
				}
			}
		}
		return null; // null 表示校验通过
	}

	/**
	 * 构造一条新的（未持久化）追踪记录，并用类型安全 setter 填好三个固定外键。 动态业务字段仍由调用方（弹窗代码）通过
	 * set_ValueOfColumn(columnName, value) 填充。
	 */
	public MHFPPOrderNodeTracking newRecord(Properties ctx, int ppOrderId, int ppOrderNodeId, int operationClassId,
			String trxName) {
		MHFPPOrderNodeTracking po = new MHFPPOrderNodeTracking(ctx, 0, trxName);
		po.setPP_Order_ID(ppOrderId);
		po.setPP_Order_Node_ID(ppOrderNodeId);
		po.setoperationclass_ID(operationClassId);
		return po;
	}

	/** 保存一条已经校验通过的追踪记录。调用方须先调用 validate() 确认通过。 */
	public boolean save(MHFPPOrderNodeTracking po) {
		return po.save();
	}

	/** 物理删除一条追踪记录（按需求确认：不做逻辑删除、不做工单状态校验）。 */
	public boolean delete(MHFPPOrderNodeTracking po) {
		try {
			po.deleteEx(true);
			return true;
		} catch (Exception e) {
			log.severe("删除打样追踪记录失败(ID=" + po.get_ID() + "): " + e.getMessage());
			return false;
		}
	}

	/** 简单值对象，承载 operationclass 的三个关键字段 */
	public static class OperationClassInfo {
		public final int operationClassId; // operationclass 主键
		public final String value; // 稳定业务编码，如 "1"~"9"
		public final String name; // 中文名称，如"胶印"

		public OperationClassInfo(int operationClassId, String value, String name) {
			this.operationClassId = operationClassId;
			this.value = value;
			this.name = name;
		}
	}
}