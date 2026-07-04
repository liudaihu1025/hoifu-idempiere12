package org.libero.form;  
  
import java.sql.Timestamp;  
import java.text.SimpleDateFormat;  
import java.util.ArrayList;  
import java.util.List;  
import java.util.logging.Level;  
  
import org.adempiere.webui.component.Button;        // 必须用此包，非 org.zkoss.zul.Button  
import org.adempiere.webui.component.ConfirmPanel;  
import org.adempiere.webui.component.Grid;  
import org.adempiere.webui.component.Rows;  
import org.adempiere.webui.panel.ADForm;  
import org.adempiere.webui.panel.CustomForm;  
import org.adempiere.webui.panel.IFormController;  
import org.adempiere.webui.util.ZKUpdateUtil;  
import org.adempiere.webui.window.Dialog;  
import org.compiere.model.MOrder;  
import org.compiere.model.MOrderLine;  
import org.compiere.model.MProduct;  
import org.compiere.model.PO;  
import org.compiere.model.Query;  
import org.compiere.util.CLogger;  
import org.compiere.util.DB;  
import org.compiere.util.Env;  
import org.eevolution.model.I_PP_Order;  
import org.eevolution.model.I_PP_Order_BOM;  
import org.eevolution.model.I_PP_Order_BOMLine;  
import org.eevolution.model.I_PP_Order_Node;  
import org.eevolution.model.I_PP_Order_Workflow;  
import org.zkoss.util.media.AMedia;  
import org.zkoss.zk.ui.event.Event;  
import org.zkoss.zk.ui.event.EventListener;  
import org.zkoss.zk.ui.event.Events;  
import org.zkoss.zul.Borderlayout;  
import org.zkoss.zul.Caption;  
import org.zkoss.zul.Center;  
import org.zkoss.zul.Column;  
import org.zkoss.zul.Columns;  
import org.zkoss.zul.Filedownload;  
import org.zkoss.zul.Groupbox;  
import org.zkoss.zul.Hlayout;  
import org.zkoss.zul.Label;  
import org.zkoss.zul.North;  
import org.zkoss.zul.Row;  
import org.zkoss.zul.South;  
import org.zkoss.zul.Textbox;  
import org.zkoss.zul.Vlayout;  
  
/**  
 * 生产工单流式展示 Form  
 *  
 * 关于"印前/印中/印后"过滤：  
 *   标准 PP_Order_Node 无此字段，本实现约定用 Value 字段前缀区分：  
 *     印前 → Value 以 "PRE_" 开头  
 *     印中 → Value 以 "MID_" 开头  
 *     印后 → Value 以 "POST_" 开头  
 *   如已通过 AD 扩展自定义列 NodeType，请将 queryNodesByValuePrefix 改为  
 *   按 get_Value("NodeType") 过滤。  
 *  
 * 关于"主材" ComponentType：  
 *   标准值 "CO" = Component（组件/主材），如业务使用自定义值请修改 COMPONENTTYPE_MAIN。  
 */  
@org.idempiere.ui.zk.annotation.Form(name = "org.libero.form.WMfgOrderForm")  
public class WMfgOrderForm implements IFormController, EventListener<Event> {  
  
    private static final CLogger log = CLogger.getCLogger(WMfgOrderForm.class);  
    private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");  
  
    /** 主材 ComponentType 值，按实际业务调整 */  
    private static final String COMPONENTTYPE_MAIN = "CO";  
  
    private CustomForm form = new CustomForm();  
  
    // 查询区域  
    private Textbox txtDocumentNo = new Textbox();  
    private Button  btnSearch;          // 在 zkInit() 中初始化  
  
    // 流式内容区  
    private Vlayout contentLayout = new Vlayout();  
  
    /**  
     * currentOrder 声明为 PO，以便调用 save()/delete()。  
     * 访问业务字段时强转为 I_PP_Order。  
     * 这样可避免 org.libero.model.MPPOrder 与任何 X_PP_Order 之间的 ClassCastException。  
     */  
    private PO currentOrder = null;  
  
    // ─────────────────────────────────────────────────────────────────  
    // 构造器  
    // ─────────────────────────────────────────────────────────────────  
  
    public WMfgOrderForm() {  
        try {  
            zkInit();  
            if (form.getProcessInfo() != null && form.getProcessInfo().getRecord_ID() > 0) {  
                loadOrder(form.getProcessInfo().getRecord_ID());  
            }  
        } catch (Exception e) {  
            log.log(Level.SEVERE, "WMfgOrderForm init error", e);  
        }  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // 布局初始化  
    // ─────────────────────────────────────────────────────────────────  
  
    private void zkInit() {  
        Borderlayout mainLayout = new Borderlayout();  
        ZKUpdateUtil.setWidth(mainLayout, "100%");  
        ZKUpdateUtil.setHeight(mainLayout, "100%");  
        form.appendChild(mainLayout);  
  
        // ── North：查询栏 ──────────────────────────────────────────  
        North north = new North();  
        north.setStyle("border-bottom:1px solid #ddd; padding:8px; background:#f5f5f5;");  
        mainLayout.appendChild(north);  
  
        Hlayout searchBar = new Hlayout();  
        searchBar.setSpacing("8px");  
        searchBar.setStyle("align-items:center; padding:4px 0;");  
  
        Label lbl = new Label("生产工单号：");  
        lbl.setStyle("font-weight:bold;");  
        txtDocumentNo.setWidth("220px");  
        txtDocumentNo.setPlaceholder("输入单据号后按 Enter 或点击查询");  
        txtDocumentNo.addEventListener(Events.ON_OK, this);  
  
        // 使用 org.adempiere.webui.component.Button（无参构造 + setLabel）  
        btnSearch = new Button();  
        btnSearch.setLabel("查询");  
        btnSearch.setSclass("z-button");  
        btnSearch.addEventListener(Events.ON_CLICK, this);  
  
        searchBar.appendChild(lbl);  
        searchBar.appendChild(txtDocumentNo);  
        searchBar.appendChild(btnSearch);  
        north.appendChild(searchBar);  
  
        // ── Center：流式内容区 ─────────────────────────────────────  
        Center center = new Center();  
        center.setAutoscroll(true);  
        center.setStyle("padding:12px;");  
        mainLayout.appendChild(center);  
  
        ZKUpdateUtil.setWidth(contentLayout, "100%");  
        contentLayout.setSpacing("0px");  
        center.appendChild(contentLayout);  
  
        // ── South：操作按钮栏 ──────────────────────────────────────  
        South south = new South();  
        south.setStyle("border-top:1px solid #ddd; padding:4px;");  
        mainLayout.appendChild(south);  
  
        // 6 参数构造：(withCancel, withRefresh, withReset, withCustomize, withHistory, withZoom)  
        ConfirmPanel cp = new ConfirmPanel(false, false, false, false, false, false);  
  
        // 隐藏默认 OK 按钮  
        Button bOk = cp.getButton(ConfirmPanel.A_OK);  
        if (bOk != null) bOk.setVisible(false);  
  
        // 创建按钮 → 加入面板 → 最后统一注册监听（顺序不能颠倒）  
        Button bNew     = cp.createButton(ConfirmPanel.A_NEW);  
        Button bSave    = cp.createButton("Save");  
        Button bCancel  = cp.createButton(ConfirmPanel.A_CANCEL);  
        Button bRefresh = cp.createButton(ConfirmPanel.A_REFRESH);  
        Button bPrint   = cp.createButton(ConfirmPanel.A_PRINT);  
        Button bExport  = cp.createButton(ConfirmPanel.A_EXPORT);  
        Button bDelete  = cp.createButton(ConfirmPanel.A_DELETE);  
  
        cp.addComponentsLeft(bNew);  
        cp.addComponentsLeft(bSave);  
        cp.addComponentsLeft(bCancel);  
        cp.addComponentsLeft(bRefresh);  
        cp.addComponentsLeft(bPrint);  
        cp.addComponentsLeft(bExport);  
        cp.addComponentsLeft(bDelete);  
  
        // 必须在所有 addComponentsLeft 之后调用  
        cp.addActionListener(this);  
  
        south.appendChild(cp);  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // 事件处理  
    // ─────────────────────────────────────────────────────────────────  
  
    @Override  
    public void onEvent(Event event) throws Exception {  
        Object target = event.getTarget();  
  
        // 查询栏  
        if (target == btnSearch || target == txtDocumentNo) {  
            cmdSearch();  
            return;  
        }  
  
        // ConfirmPanel 按钮（通过 ID 区分）  
        if (target instanceof Button) {  
            String id = ((Button) target).getId();  
            if      (ConfirmPanel.A_NEW.equals(id))     cmdNew();  
            else if ("Save".equals(id))                 cmdSave();  
            else if (ConfirmPanel.A_CANCEL.equals(id))  cmdCancel();  
            else if (ConfirmPanel.A_REFRESH.equals(id)) cmdRefresh();  
            else if (ConfirmPanel.A_PRINT.equals(id))   cmdPrint();  
            else if (ConfirmPanel.A_EXPORT.equals(id))  cmdExport();  
            else if (ConfirmPanel.A_DELETE.equals(id))  cmdDelete();  
        }  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // 命令方法  
    // ─────────────────────────────────────────────────────────────────  
  
    private void cmdSearch() {  
        String docNo = txtDocumentNo.getValue().trim();  
        if (docNo.isEmpty()) return;  
  
        // Query 返回的是 MPPOrder（实现 I_PP_Order），不强转为任何 X_PP_Order  
        PO po = new Query(Env.getCtx(), I_PP_Order.Table_Name,  
                "DocumentNo=? AND AD_Client_ID=?", null)  
            .setParameters(docNo, Env.getAD_Client_ID(Env.getCtx()))  
            .first();  
  
        contentLayout.getChildren().clear();  
        if (po instanceof I_PP_Order) {  
            currentOrder = po;  
            loadOrder(((I_PP_Order) po).getPP_Order_ID());  
        } else {  
            Label msg = new Label("未找到单据号 [" + docNo + "] 对应的生产工单");  
            msg.setStyle("color:#c00; padding:20px; font-size:14px;");  
            contentLayout.appendChild(msg);  
        }  
    }  
  
    private void cmdNew() {  
        // 直接实例化标准类（不经过模型工厂），避免 ClassCastException  
        org.eevolution.model.X_PP_Order newOrder =  
            new org.eevolution.model.X_PP_Order(Env.getCtx(), 0, null);  
        // setAD_Client_ID 是 protected，用 set_ValueNoCheck 代替  
        newOrder.set_ValueNoCheck("AD_Client_ID", Env.getAD_Client_ID(Env.getCtx()));  
        // setAD_Org_ID 是 public，可直接调用  
        newOrder.setAD_Org_ID(Env.getAD_Org_ID(Env.getCtx()));  
        currentOrder = newOrder;  
        contentLayout.getChildren().clear();  
        Label msg = new Label("新建生产工单 — 请通过标准工单窗口填写必填字段后点击保存");  
        msg.setStyle("color:#555; padding:20px; font-size:14px;");  
        contentLayout.appendChild(msg);  
    }  
  
    private void cmdSave() {  
        if (currentOrder == null) return;  
        try {  
            currentOrder.saveEx();  
            if (currentOrder instanceof I_PP_Order) {  
                loadOrder(((I_PP_Order) currentOrder).getPP_Order_ID());  
            }  
        } catch (Exception e) {  
            log.log(Level.SEVERE, "Save error", e);  
            Dialog.error(0, "SaveError", e.getMessage());  
        }  
    }  
  
    private void cmdCancel() {  
        if (currentOrder == null) return;  
        if (currentOrder.get_ID() == 0) {  
            // 新建未保存，直接清空  
            currentOrder = null;  
            contentLayout.getChildren().clear();  
        } else {  
            // 已有记录，从数据库重新加载（丢弃未保存修改）  
            currentOrder.load(null);  
            if (currentOrder instanceof I_PP_Order) {  
                loadOrder(((I_PP_Order) currentOrder).getPP_Order_ID());  
            }  
        }  
    }  
  
    private void cmdRefresh() {  
        if (currentOrder == null || currentOrder.get_ID() == 0) return;  
        if (currentOrder instanceof I_PP_Order) {  
            loadOrder(((I_PP_Order) currentOrder).getPP_Order_ID());  
        }  
    }  
  
    private void cmdDelete() {  
        if (currentOrder == null || currentOrder.get_ID() == 0) return;  
        try {  
            currentOrder.deleteEx(true);  
            currentOrder = null;  
            contentLayout.getChildren().clear();  
            Label msg = new Label("记录已删除");  
            msg.setStyle("color:#333; padding:20px;");  
            contentLayout.appendChild(msg);  
        } catch (Exception e) {  
            log.log(Level.SEVERE, "Delete error", e);  
            Dialog.error(0, "DeleteError", e.getMessage());  
        }  
    }  
  
    private void cmdPrint() {  
        // 打印需要在应用字典中为 PP_Order 配置打印格式  
        // 配置完成后替换为：  
        //   ReportEngine re = ReportEngine.get(Env.getCtx(), <type>, currentOrder.get_ID(), form.getWindowNo());  
        //   if (re != null) ReportCtl.preview(re);  
        Dialog.error(0, "Error", "打印功能需要先在应用字典中为生产工单配置打印格式");  
    }  
  
    private void cmdExport() {  
        if (currentOrder == null || currentOrder.get_ID() == 0) return;  
        I_PP_Order o = (I_PP_Order) currentOrder;  
  
        StringBuilder sb = new StringBuilder("\uFEFF"); // UTF-8 BOM，Excel 正确识别中文  
        sb.append("单据号,状态,产品,仓库,订单数量,已交付,计划开始,计划完成\n");  
        sb.append(esc(o.getDocumentNo())).append(",")  
          .append(esc(docStatusLabel(o.getDocStatus()))).append(",")  
          .append(esc(lookupName("M_Product",   "M_Product_ID",   o.getM_Product_ID()))).append(",")  
          .append(esc(lookupName("M_Warehouse", "M_Warehouse_ID", o.getM_Warehouse_ID()))).append(",")  
          .append(o.getQtyOrdered().toPlainString()).append(",")  
          .append(o.getQtyDelivered().toPlainString()).append(",")  
          .append(esc(fmt(o.getDateStartSchedule()))).append(",")  
          .append(esc(fmt(o.getDateFinishSchedule()))).append("\n");  
  
        // BOM 物料明细  
        I_PP_Order_BOM bom = queryBOM(o.getPP_Order_ID());  
        if (bom != null) {  
            sb.append("\nBOM物料明细\n行号,物料,需求数量,单位,组件类型,发料方式,关键\n");  
            for (I_PP_Order_BOMLine l : queryBOMLines(bom.getPP_Order_BOM_ID(), o.getPP_Order_ID())) {  
                sb.append(l.getLine()).append(",")  
                  .append(esc(lookupName("M_Product", "M_Product_ID", l.getM_Product_ID()))).append(",")  
                  .append(l.getQtyRequiered().toPlainString()).append(",")  
                  .append(esc(lookupName("C_UOM", "C_UOM_ID", l.getC_UOM_ID()))).append(",")  
                  .append(esc(l.getComponentType())).append(",")  
                  .append(esc(l.getIssueMethod())).append(",")  
                  .append(l.isCritical() ? "是" : "否").append("\n");  
            }  
        }  
  
        // 工序  
        List<I_PP_Order_Node> nodes = queryAllNodes(o.getPP_Order_ID());  
        if (!nodes.isEmpty()) {  
            sb.append("\n工序活动\n工序名称,资源,计划开始,计划完成,工时(分),准备时间,需求数量,已完成,状态\n");  
            for (I_PP_Order_Node n : nodes) {  
                sb.append(esc(n.getName())).append(",")  
                  .append(esc(lookupName("S_Resource", "S_Resource_ID", n.getS_Resource_ID()))).append(",")  
                  .append(esc(fmt(n.getDateStartSchedule()))).append(",")  
                  .append(esc(fmt(n.getDateFinishSchedule()))).append(",")  
                  .append(n.getDuration()).append(",")  
                  .append(n.getSetupTime()).append(",")  
                  .append(n.getQtyRequiered().toPlainString()).append(",")  
                  .append(n.getQtyDelivered().toPlainString()).append(",")  
                  .append(esc(docStatusLabel(n.getDocStatus()))).append("\n");  
            }  
        }  
  
        byte[] data = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);  
        AMedia media = new AMedia(o.getDocumentNo() + ".csv", "csv", "text/csv", data);  
        Filedownload.save(media);  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // 数据加载  
    // ─────────────────────────────────────────────────────────────────  
  
    private void loadOrder(int ppOrderId) {  
        contentLayout.getChildren().clear();  
  
        PO po = new Query(Env.getCtx(), I_PP_Order.Table_Name,  
                "PP_Order_ID=?", null)  
            .setParameters(ppOrderId)  
            .first();  
        if (!(po instanceof I_PP_Order)) return;  
  
        currentOrder = po;  
        I_PP_Order order = (I_PP_Order) po;  
  
        // Section 1：生产工单主信息  
        contentLayout.appendChild(buildOrderSection(order));  
  
        // Section 2：关联订单信息  
        if (order.getC_OrderLine_ID() > 0) {  
            Groupbox gb = buildOrderLineSection(order);  
            if (gb != null) contentLayout.appendChild(gb);  
        }  
  
        // Section 3：产品信息  
        Groupbox gbProduct = buildProductSection(order);  
        if (gbProduct != null) contentLayout.appendChild(gbProduct);  
  
        // Section 4：BOM 头信息  
        I_PP_Order_BOM bom = queryBOM(ppOrderId);  
        if (bom != null) {  
            contentLayout.appendChild(buildBOMSection(bom));  
  
            // Section 5：主材信息（ComponentType = COMPONENTTYPE_MAIN）  
            List<I_PP_Order_BOMLine> mainMats =  
                queryBOMLinesByType(bom.getPP_Order_BOM_ID(), ppOrderId, COMPONENTTYPE_MAIN);  
            if (!mainMats.isEmpty())  
                contentLayout.appendChild(buildBOMLineSection(  
                    "主材信息（ComponentType=" + COMPONENTTYPE_MAIN + "，共 " + mainMats.size() + " 行）",  
                    mainMats));  
  
            // Section 6：全部 BOM 物料明细  
            List<I_PP_Order_BOMLine> allLines =  
                queryBOMLines(bom.getPP_Order_BOM_ID(), ppOrderId);  
            if (!allLines.isEmpty())  
                contentLayout.appendChild(buildBOMLineSection(  
                    "BOM 物料明细（共 " + allLines.size() + " 行）", allLines));  
        }  
  
        // Section 7：印前准备（Value 前缀 = PRE）  
        List<I_PP_Order_Node> preNodes = queryNodesByValuePrefix(ppOrderId, "PRE");  
        if (!preNodes.isEmpty())  
            contentLayout.appendChild(buildNodeSection(  
                "印前准备（共 " + preNodes.size() + " 道工序）", preNodes));  
  
        // Section 8：生产工艺（Value 前缀 = MID 或 POST）  
        List<I_PP_Order_Node> prodNodes = queryNodesByValuePrefixes(ppOrderId, "MID", "POST");  
        if (!prodNodes.isEmpty())  
            contentLayout.appendChild(buildNodeSection(  
                "生产工艺（共 " + prodNodes.size() + " 道工序）", prodNodes));  
  
        // Section 9：工艺路线  
        List<I_PP_Order_Workflow> workflows = queryWorkflows(ppOrderId);  
        if (!workflows.isEmpty())  
            contentLayout.appendChild(buildWorkflowSection(workflows));  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // 区块构建  
    // ─────────────────────────────────────────────────────────────────  
  
    private Groupbox buildOrderSection(I_PP_Order o) {  
        Groupbox gb = newGroupbox("生产工单信息");  
        Grid g = newFormGrid();  
        Rows rows = g.newRows();  
        addRow(rows, "单据号",   o.getDocumentNo(),  
                     "单据状态", docStatusLabel(o.getDocStatus()));  
        addRow(rows, "产品",  
                     lookupName("M_Product",   "M_Product_ID",   o.getM_Product_ID()),  
                     "仓库",  
                     lookupName("M_Warehouse", "M_Warehouse_ID", o.getM_Warehouse_ID()));  
        addRow(rows, "订单数量",  
                     o.getQtyOrdered() + " " + lookupName("C_UOM", "C_UOM_ID", o.getC_UOM_ID()),  
                     "已交付", o.getQtyDelivered().toPlainString());  
        addRow(rows, "计划开始", fmt(o.getDateStartSchedule()),  
                     "计划完成", fmt(o.getDateFinishSchedule()));  
        gb.appendChild(g);  
        return gb;  
    }  
  
    private Groupbox buildOrderLineSection(I_PP_Order order) {  
        int olId = order.getC_OrderLine_ID();  
        if (olId <= 0) return null;  
        MOrderLine ol;  
        MOrder     mo;  
        try {  
            ol = new MOrderLine(Env.getCtx(), olId, null);  
            mo = new MOrder(Env.getCtx(), ol.getC_Order_ID(), null);  
        } catch (Exception e) {   
            log.log(Level.WARNING, "buildOrderLineSection", e);  
            return null;  
        }  
        Groupbox gb = newGroupbox("关联订单信息");  
        Grid g = newFormGrid();  
        Rows rows = g.newRows();  
        addRow(rows, "订单号",   mo.getDocumentNo(),  
                     "订单状态", docStatusLabel(mo.getDocStatus()));  
        addRow(rows, "客户",  
                     lookupName("C_BPartner", "C_BPartner_ID", mo.getC_BPartner_ID()),  
                     "订单日期", fmt(mo.getDateOrdered()));  
        addRow(rows, "行号",     String.valueOf(ol.getLine()),  
                     "行描述",   ol.getDescription() != null ? ol.getDescription() : "");  
        addRow(rows, "订单数量", ol.getQtyOrdered().toPlainString(),  
                     "已交付",   ol.getQtyDelivered().toPlainString());  
        gb.appendChild(g);  
        return gb;  
    }  
  
    private Groupbox buildProductSection(I_PP_Order order) {  
        MProduct p = MProduct.get(Env.getCtx(), order.getM_Product_ID());  
        if (p == null) return null;  
        Groupbox gb = newGroupbox("产品信息");  
        Grid g = newFormGrid();  
        Rows rows = g.newRows();  
        addRow(rows, "产品编码", p.getValue(),       "产品名称", p.getName());  
        addRow(rows, "描述",     p.getDescription() != null ? p.getDescription() : "",  
                     "产品类型", p.getProductType());  
        addRow(rows, "单位",  
                     lookupName("C_UOM", "C_UOM_ID", p.getC_UOM_ID()),  
                     "产品分类",  
                     lookupName("M_Product_Category", "M_Product_Category_ID",  
                                p.getM_Product_Category_ID()));  
        gb.appendChild(g);  
        return gb;  
    }  
  
    private Groupbox buildBOMSection(I_PP_Order_BOM b) {  
        Groupbox gb = newGroupbox("BOM 头信息");  
        Grid g = newFormGrid();  
        Rows rows = g.newRows();  
        addRow(rows, "BOM 名称", b.getName(),  
                     "版本",     b.getRevision() != null ? b.getRevision() : "");  
        addRow(rows, "BOM 类型", b.getBOMType(),  
                     "BOM 用途", b.getBOMUse());  
        addRow(rows, "有效期从", fmt(b.getValidFrom()),  
                     "有效期至", fmt(b.getValidTo()));  
        gb.appendChild(g);  
        return gb;  
    }  
  
    private Groupbox buildBOMLineSection(String title, List<I_PP_Order_BOMLine> lines) {  
        Groupbox gb = newGroupbox(title);  
        Grid g = newListGrid(  
            new String[]{"行号", "物料", "组件类型", "BOM 数量", "需求数量", "已发料", "单位", "关键"},  
            new String[]{"6%",  "22%", "10%",     "10%",     "10%",     "10%",   "8%",  "6%"}  
        );  
        Rows rows = g.newRows();  
        for (I_PP_Order_BOMLine l : lines) {  
            Row row = new Row();  
            row.appendChild(cell(String.valueOf(l.getLine())));  
            row.appendChild(cell(lookupName("M_Product", "M_Product_ID", l.getM_Product_ID())));  
            row.appendChild(cell(l.getComponentType()));  
            row.appendChild(cell(l.getQtyBOM().toPlainString()));  
            row.appendChild(cell(l.getQtyRequiered().toPlainString()));  
            row.appendChild(cell(l.getQtyDelivered().toPlainString()));  
            row.appendChild(cell(lookupName("C_UOM", "C_UOM_ID", l.getC_UOM_ID())));  
            row.appendChild(cell(l.isCritical() ? "是" : "否"));  
            rows.appendChild(row);  
        }  
        gb.appendChild(g);  
        return gb;  
    }  
  
    private Groupbox buildNodeSection(String title, List<I_PP_Order_Node> nodes) {  
        Groupbox gb = newGroupbox(title);  
        Grid g = newListGrid(  
            new String[]{"工序名称", "资源", "计划开始", "计划完成", "工时(分)", "准备时间", "需求量", "已完成", "状态"},  
            new String[]{"14%",    "12%", "10%",     "10%",     "9%",      "9%",      "8%",   "8%",   "8%"}  
        );  
        Rows rows = g.newRows();  
        for (I_PP_Order_Node n : nodes) {  
            Row row = new Row();  
            row.appendChild(cell(n.getName()));  
            row.appendChild(cell(lookupName("S_Resource", "S_Resource_ID", n.getS_Resource_ID())));  
            row.appendChild(cell(fmt(n.getDateStartSchedule())));  
            row.appendChild(cell(fmt(n.getDateFinishSchedule())));  
            row.appendChild(cell(String.valueOf(n.getDuration())));  
            row.appendChild(cell(String.valueOf(n.getSetupTime())));  
            row.appendChild(cell(n.getQtyRequiered().toPlainString()));  
            row.appendChild(cell(n.getQtyDelivered().toPlainString()));  
            row.appendChild(cell(docStatusLabel(n.getDocStatus())));  
            rows.appendChild(row);  
        }  
        gb.appendChild(g);  
        return gb;  
    }  
  
    private Groupbox buildWorkflowSection(List<I_PP_Order_Workflow> workflows) {  
        Groupbox gb = newGroupbox("工艺路线（共 " + workflows.size() + " 条）");  
        Grid g = newListGrid(  
            new String[]{"名称", "版本", "工作时间(分)", "等待时间(分)", "有效期从", "有效期至"},  
            new String[]{"20%", "10%", "14%",         "14%",         "14%",     "14%"}  
        );  
        Rows rows = g.newRows();  
        for (I_PP_Order_Workflow w : workflows) {  
            Row row = new Row();  
            row.appendChild(cell(w.getName()));  
            row.appendChild(cell(String.valueOf(w.getVersion())));  
            row.appendChild(cell(String.valueOf(w.getWorkingTime())));  
            row.appendChild(cell(String.valueOf(w.getWaitingTime())));  
            row.appendChild(cell(fmt(w.getValidFrom())));  
            row.appendChild(cell(fmt(w.getValidTo())));  
            rows.appendChild(row);  
        }  
        gb.appendChild(g);  
        return gb;  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // 查询辅助（全部使用 Query，不用裸 SQL；全部返回接口类型）  
    // ─────────────────────────────────────────────────────────────────  
  
    private I_PP_Order_BOM queryBOM(int ppOrderId) {  
        PO po = new Query(Env.getCtx(), I_PP_Order_BOM.Table_Name,  
                "PP_Order_ID=?", null)  
            .setParameters(ppOrderId)  
            .setOnlyActiveRecords(true)  
            .first();  
        return (po instanceof I_PP_Order_BOM) ? (I_PP_Order_BOM) po : null;  
    }  
  
    private List<I_PP_Order_BOMLine> queryBOMLines(int bomId, int ppOrderId) {  
        return toInterfaceList(  
            new Query(Env.getCtx(), I_PP_Order_BOMLine.Table_Name,  
                    "PP_Order_BOM_ID=? AND PP_Order_ID=?", null)  
                .setParameters(bomId, ppOrderId)  
                .setOnlyActiveRecords(true)  
                .setOrderBy("Line")  
                .list(),  
            I_PP_Order_BOMLine.class);  
    }  
  
    private List<I_PP_Order_BOMLine> queryBOMLinesByType(int bomId, int ppOrderId, String componentType) {  
        return toInterfaceList(  
            new Query(Env.getCtx(), I_PP_Order_BOMLine.Table_Name,  
                    "PP_Order_BOM_ID=? AND PP_Order_ID=? AND ComponentType=?", null)  
                .setParameters(bomId, ppOrderId, componentType)  
                .setOnlyActiveRecords(true)  
                .setOrderBy("Line")  
                .list(),  
            I_PP_Order_BOMLine.class);  
    }  
  
    private List<I_PP_Order_Node> queryNodesByValuePrefix(int ppOrderId, String prefix) {  
        return toInterfaceList(  
            new Query(Env.getCtx(), I_PP_Order_Node.Table_Name,  
                    "PP_Order_ID=? AND Value LIKE ?", null)  
                .setParameters(ppOrderId, prefix + "_%")  
                .setOnlyActiveRecords(true)  
                .setOrderBy("PP_Order_Node_ID")  
                .list(),  
            I_PP_Order_Node.class);  
    }  
  
    private List<I_PP_Order_Node> queryNodesByValuePrefixes(int ppOrderId, String... prefixes) {  
        if (prefixes == null || prefixes.length == 0) return new ArrayList<>();  
        StringBuilder where = new StringBuilder("PP_Order_ID=? AND (");  
        List<Object> params = new ArrayList<>();  
        params.add(ppOrderId);  
        for (int i = 0; i < prefixes.length; i++) {  
            if (i > 0) where.append(" OR ");  
            where.append("Value LIKE ?");  
            params.add(prefixes[i] + "_%");  
        }  
        where.append(")");  
        return toInterfaceList(  
            new Query(Env.getCtx(), I_PP_Order_Node.Table_Name, where.toString(), null)  
                .setParameters(params)  
                .setOnlyActiveRecords(true)  
                .setOrderBy("PP_Order_Node_ID")  
                .list(),  
            I_PP_Order_Node.class);  
    }  
  
    private List<I_PP_Order_Node> queryAllNodes(int ppOrderId) {  
        return toInterfaceList(  
            new Query(Env.getCtx(), I_PP_Order_Node.Table_Name,  
                    "PP_Order_ID=?", null)  
                .setParameters(ppOrderId)  
                .setOnlyActiveRecords(true)  
                .setOrderBy("PP_Order_Node_ID")  
                .list(),  
            I_PP_Order_Node.class);  
    }  
  
    private List<I_PP_Order_Workflow> queryWorkflows(int ppOrderId) {  
        return toInterfaceList(  
            new Query(Env.getCtx(), I_PP_Order_Workflow.Table_Name,  
                    "PP_Order_ID=?", null)  
                .setParameters(ppOrderId)  
                .setOnlyActiveRecords(true)  
                .list(),  
            I_PP_Order_Workflow.class);  
    }  
  
    /** 安全地将 Query 返回的 List<PO> 转换为 List<T>，过滤掉不匹配的对象 */  
    @SuppressWarnings("unchecked")  
    private <T> List<T> toInterfaceList(List<? extends PO> poList, Class<T> iface) {  
        List<T> result = new ArrayList<>();  
        for (PO po : poList) {  
            if (iface.isInstance(po)) result.add((T) po);  
        }  
        return result;  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // ZK 组件工厂  
    // ─────────────────────────────────────────────────────────────────  
  
    private Groupbox newGroupbox(String title) {  
        Groupbox gb = new Groupbox();  
        gb.setMold("3d");  
        gb.setStyle("margin-bottom:12px; width:100%;");  
        Caption cap = new Caption(title);  
        cap.setStyle("font-weight:bold; font-size:13px; color:#333;");  
        gb.appendChild(cap);  
        return gb;  
    }  
  
    private Grid newFormGrid() {  
        Grid g = new Grid();  
        g.setSclass("grid-layout");  
        ZKUpdateUtil.setHflex(g, "1");  
        Columns cols = new Columns();  
        for (String w : new String[]{"15%", "35%", "15%", "35%"}) {  
            Column c = new Column();  
            c.setWidth(w);  
            cols.appendChild(c);  
        }  
        g.appendChild(cols);  
        return g;  
    }  
  
    private Grid newListGrid(String[] headers, String[] widths) {  
        Grid g = new Grid();  
        g.setSclass("z-grid");  
        ZKUpdateUtil.setHflex(g, "1");  
        g.setSizedByContent(false);  
        Columns cols = new Columns();  
        cols.setSizable(true);  
        for (int i = 0; i < headers.length; i++) {  
            Column c = new Column(headers[i]);  
            c.setWidth(widths[i]);  
            cols.appendChild(c);  
        }  
        g.appendChild(cols);  
        return g;  
    }  
  
    private void addRow(Rows rows, String l1, String v1, String l2, String v2) {  
        Row row = new Row();  
        row.appendChild(labelCell(l1));  
        row.appendChild(valueCell(v1));  
        row.appendChild(labelCell(l2));  
        row.appendChild(valueCell(v2));  
        rows.appendChild(row);  
    }  
  
    private Label labelCell(String text) {  
        Label l = new Label(text != null ? text : "");  
        l.setStyle("font-weight:bold; color:#555; text-align:right; padding-right:8px; display:block;");  
        return l;  
    }  
  
    private Label valueCell(String text) {  
        Label l = new Label(text != null ? text : "");  
        l.setStyle("padding-left:4px; color:#222;");  
        return l;  
    }  
  
    private Label cell(String text) {  
        return new Label(text != null ? text : "");  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // 通用辅助  
    // ─────────────────────────────────────────────────────────────────  
  
    private String lookupName(String table, String pkCol, int id) {  
        return lookupName(table, pkCol, id, "Name");  
    }  
  
    private String lookupName(String table, String pkCol, int id, String nameCol) {  
        if (id <= 0) return "";  
        String v = DB.getSQLValueString(null,  
                "SELECT " + nameCol + " FROM " + table + " WHERE " + pkCol + "=?", id);  
        return v != null ? v : "";  
    }  
  
    private String fmt(Timestamp ts) {  
        return ts != null ? SDF.format(ts) : "";  
    }  
  
    /** CSV 字段转义 */  
    private String esc(String s) {  
        if (s == null) return "";  
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))  
            return "\"" + s.replace("\"", "\"\"") + "\"";  
        return s;  
    }  
  
    private String docStatusLabel(String s) {  
        if (s == null) return "";  
        switch (s) {  
            case "DR": return "草稿";  
            case "IP": return "进行中";  
            case "CO": return "已完成";  
            case "CL": return "已关闭";  
            case "VO": return "已作废";  
            case "RE": return "已冲销";  
            default:   return s;  
        }  
    }  
  
    // ─────────────────────────────────────────────────────────────────  
    // IFormController 接口  
    // ─────────────────────────────────────────────────────────────────  
  
    @Override  
    public ADForm getForm() {  
        return form;  
    }  
}