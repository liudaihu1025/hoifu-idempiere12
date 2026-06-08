package org.libero.model;

import java.sql.ResultSet;
import java.util.Properties;

import org.libero.tables.X_AD_Routing_Node;

public class MADRoutingNode extends X_AD_Routing_Node {
    public MADRoutingNode(Properties ctx, int AD_Routing_Node_ID, String trxName) {
        super(ctx, AD_Routing_Node_ID, trxName);
    }

    public MADRoutingNode(Properties ctx, int AD_Routing_Node_ID, String trxName, String... virtualColumns) {
        super(ctx, AD_Routing_Node_ID, trxName, virtualColumns);
    }

    public MADRoutingNode(Properties ctx, String AD_Routing_Node_UU, String trxName) {
        super(ctx, AD_Routing_Node_UU, trxName);
    }

    public MADRoutingNode(Properties ctx, String AD_Routing_Node_UU, String trxName, String... virtualColumns) {
        super(ctx, AD_Routing_Node_UU, trxName, virtualColumns);
    }

    public MADRoutingNode(Properties ctx, ResultSet rs, String trxName) {
        super(ctx, rs, trxName);
    }
}
