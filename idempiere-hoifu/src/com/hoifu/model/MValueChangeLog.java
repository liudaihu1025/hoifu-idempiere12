package com.hoifu.model;

import java.sql.ResultSet;
import java.util.Properties;

public class MValueChangeLog extends X_AD_ValueChangeLog {

	public MValueChangeLog(Properties ctx, int X_AD_ValueChangeLog_ID, String trxName) {
		super(ctx, X_AD_ValueChangeLog_ID, trxName);
	}

	public MValueChangeLog(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}

}
