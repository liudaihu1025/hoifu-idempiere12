package com.hoifu.utils;

import org.compiere.util.DB;

/**
 * 打样模块公共工具方法。
 */
public class DYSamplingUtil {

	private DYSamplingUtil() {
	} // 工具类，禁止实例化

	/**
	 * 判断指定用户是否具有指定的岗位领域和岗位级别。
	 * 
	 * @param userID   要判断的用户 AD_User_ID
	 * @param jobField 岗位领域 Value（如 "J"=项目, "S"=营业, "E"=工程 等）
	 * @param jobLevel 岗位级别 Value（如 "6"=总监, "5"=高工, "4"=中级 等）
	 * @return true 表示该用户的岗位满足条件
	 */
	public static boolean hasJobPosition(int userID, String jobField, String jobLevel) {
		int count = DB.getSQLValue(null,
				"SELECT COUNT(1) FROM AD_User u " + "JOIN C_Job j ON u.C_Job_ID = j.C_Job_ID "
						+ "WHERE u.AD_User_ID = ? " + "  AND j.JobField = ? " + "  AND j.JobLevel = ? "
						+ "  AND u.IsActive = 'Y' " + "  AND j.IsActive = 'Y'",
				userID, jobField, jobLevel);
		return count > 0;
	}
}