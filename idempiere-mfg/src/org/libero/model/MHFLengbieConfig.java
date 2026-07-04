package org.libero.model;

import java.sql.ResultSet;
import java.util.Properties;
import org.libero.tables.X_HF_LengbieConfig;


public class MHFLengbieConfig extends X_HF_LengbieConfig {

	public MHFLengbieConfig(Properties ctx, int X_HF_LengbieConfig_ID, String trxName) {
		super(ctx, X_HF_LengbieConfig_ID, trxName);
	}

	public MHFLengbieConfig(Properties ctx, ResultSet rs, String trxName) {
		super(ctx, rs, trxName);
	}
}
