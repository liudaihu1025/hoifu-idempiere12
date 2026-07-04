package com.hoifu.form;  
  
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.adempiere.webui.Extensions;
import org.adempiere.webui.component.Window;
import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.util.ZKUpdateUtil;
import org.compiere.model.MAttachment;
import org.compiere.model.MAttachmentEntry;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.idempiere.ui.zk.media.IMediaView;
import org.zkoss.util.media.AMedia;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zul.Borderlayout;
import org.zkoss.zul.Button;
import org.zkoss.zul.Center;
import org.zkoss.zul.Div;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Hlayout;
import org.zkoss.zul.Iframe;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.North;
import org.zkoss.zul.South;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.West;  
  
@org.idempiere.ui.zk.annotation.Form  
public class WAttachmentViewerForm extends ADForm implements EventListener<Event> {  
  
	private static final long serialVersionUID = 1L;
	private static final CLogger log = CLogger.getCLogger(WAttachmentViewerForm.class);

	/** 可直接用 Iframe 预览的 MIME 类型 */
	private static final List<String> PREVIEWABLE = Arrays.asList("image/jpeg", "image/png", "image/gif", "text/plain",
			"application/pdf", "text/xml", "application/json");

	// ---- UI 组件 ----
	private final Listbox fileList = new Listbox();
	private final Iframe preview = new Iframe();
	private final Label statusLabel = new Label();
	private final Button btnDownload = new Button("下载");

	/** 左侧顶部抬头，显示"附件（x个）" */
	private final Label attachmentHeader = new Label("附件");

	/** 备注文本框（只读），用于显示 MAttachment.getTextMsg() 的内容 */
	private final Textbox noteBox = new Textbox();

	/** 当前通过 IMediaView 渲染的自定义预览组件（如 Keikai Excel 预览） */
	private Component customPreviewComponent = null;

	/** 预览区容器，IMediaView 渲染时需要传入 */
	private Div previewContainer;

	// ---- 数据 ----
	private MAttachment m_attachment = null;
	private MAttachmentEntry currentEntry = null;

	// -----------------------------------------------------------------------
	@Override
	protected void initForm() {
		// 修复 InfoPanel.runProcess() 漏掉 form.setPage() 的问题
		if (getPage() == null) {
			org.zkoss.zk.ui.Page page = Executions.getCurrent().getDesktop().getFirstPage();
			if (page != null)
				setPage(page);
		}
  
		buildUI();
  
		if (getProcessInfo() == null) {
			showStatus("ProcessInfo 为空");
			return;
		}
  
		int pInstanceID = getProcessInfo().getAD_PInstance_ID();
		if (pInstanceID <= 0) {
			showStatus("无效的 PInstance ID");
			return;
		}
  
		// 1. 从 T_Selection 取选中行的 Record_ID（评审明细 ID）
		int reviewLineId = DB.getSQLValue(null, "SELECT T_Selection_ID FROM T_Selection WHERE AD_PInstance_ID=?",
				pInstanceID);
  
		if (reviewLineId <= 0) {
			showStatus("无法获取记录信息");
			return;
		}
  
		// 2. 通过评审明细找到任务 ID
		int taskId = DB.getSQLValue(null,
				"SELECT dy_samplingtask_ID FROM dy_samplingreviewline WHERE dy_samplingreviewline_ID=?", reviewLineId);
  
		if (taskId <= 0) {
			showStatus("该明细未关联任务");
			return;
		}
  
		// 3. 查询 dy_samplingtask 表的 AD_Table_ID
		int taskTableId = DB.getSQLValue(null, "SELECT AD_Table_ID FROM AD_Table WHERE TableName='dy_samplingtask'");
  
		if (taskTableId <= 0) {
			showStatus("无法获取任务表信息");
			return;
		}
  
		// 4. 加载任务的附件
		m_attachment = MAttachment.get(Env.getCtx(), taskTableId, taskId, null, null);
  
		if (m_attachment == null || m_attachment.getEntryCount() == 0) {
			showStatus("该任务暂无附件");
			return;
		}
  
		// 加载附件后，将附件的提交备注显示到 noteBox
		String textMsg = m_attachment.getTextMsg();
		noteBox.setText(textMsg != null ? textMsg : "");
  
		// 5. 填充附件列表，并更新抬头
		MAttachmentEntry[] entries = m_attachment.getEntries();
		attachmentHeader.setValue("附件（" + entries.length + "个）");
  
		for (int i = 0; i < entries.length; i++) {
			Listitem item = new Listitem();
			item.setValue(i);
			item.appendChild(new Listcell(entries[i].getName()));
			fileList.appendChild(item);
		}
  
		// 默认预览第一个
		fileList.setSelectedIndex(0);
		previewEntry(0);
	}
  
	// -----------------------------------------------------------------------
	private void buildUI() {
		this.setSizable(true);
		this.setMaximizable(true);
		this.setBorder("normal");
		// 显示右上角关闭
		this.setClosable(true);
		// 点击时执行与"关闭"按钮相同的清理逻辑
		this.addEventListener(Events.ON_CLOSE, e -> {
			clearPreview();
			if (m_attachment != null) {
				m_attachment.close();
				m_attachment = null;
			}
			this.detach();
		});
		ZKUpdateUtil.setWindowWidthX(this, 1500);
		ZKUpdateUtil.setHeight(this, "90%");
  
		Borderlayout layout = new Borderlayout();
		ZKUpdateUtil.setWidth(layout, "100%");
		ZKUpdateUtil.setHeight(layout, "100%");
		this.appendChild(layout);
  
		// ---- 左侧：抬头 + 附件列表 + 备注 ----
		West west = new West();
		west.setSize("280px");
		west.setSplittable(true);
		layout.appendChild(west);
  
		// 左侧内嵌 Borderlayout，分三区
		Borderlayout westLayout = new Borderlayout();
		ZKUpdateUtil.setWidth(westLayout, "100%");
		ZKUpdateUtil.setHeight(westLayout, "100%");
		west.appendChild(westLayout);
  
		// 左侧顶部：附件抬头
		North north = new North();
		north.setHeight("30px");
		north.setBorder("none");
		westLayout.appendChild(north);
  
		attachmentHeader.setStyle("font-weight:bold; padding:4px 6px; display:block;");
		north.appendChild(attachmentHeader);
  
		// 左侧中部：附件列表
		Center westCenter = new Center();
		westLayout.appendChild(westCenter);

		ZKUpdateUtil.setHflex(fileList, "1");
		ZKUpdateUtil.setVflex(fileList, "1");
		fileList.addEventListener(Events.ON_SELECT, this);
		westCenter.appendChild(fileList);
  
		// 左侧底部：备注区域（加大，约 300px）
		South westSouth = new South();
		westSouth.setHeight("300px");
		westSouth.setBorder("none");
		westLayout.appendChild(westSouth);
  
		org.zkoss.zul.Vlayout notePanel = new org.zkoss.zul.Vlayout();
		notePanel.setStyle("padding:4px;");
		ZKUpdateUtil.setWidth(notePanel, "100%");
		ZKUpdateUtil.setHeight(notePanel, "100%");
  
		Label noteLabel = new Label("提交备注：");
		noteLabel.setStyle("font-weight:bold; margin-bottom:2px; display:block;");
		noteBox.setReadonly(true);
		noteBox.setMultiline(true);
		ZKUpdateUtil.setWidth(noteBox, "100%");
		ZKUpdateUtil.setVflex(noteBox, "1"); // 撑满父容器剩余高度
  
		notePanel.appendChild(noteLabel);
		notePanel.appendChild(noteBox);
		westSouth.appendChild(notePanel);
  
		// ---- 中部：预览区域 ----
		Center center = new Center();
		layout.appendChild(center);
  
		previewContainer = new Div();
		ZKUpdateUtil.setHflex(previewContainer, "1");
		ZKUpdateUtil.setVflex(previewContainer, "1");
		center.appendChild(previewContainer);
  
		ZKUpdateUtil.setHflex(preview, "1");
		ZKUpdateUtil.setVflex(preview, "1");
		preview.setVisible(false);
		previewContainer.appendChild(preview);
  
		statusLabel.setVisible(false);
		previewContainer.appendChild(statusLabel);
  
		// ---- 南部：仅按钮行（高度缩回 40px）----
		South south = new South();
		south.setHeight("40px");
		layout.appendChild(south);
  
		// 按钮行：text-align:right 使按钮靠右对齐
		Hlayout btnPanel = new Hlayout();
		btnPanel.setStyle("padding:4px; text-align:right;");
		ZKUpdateUtil.setHflex(btnPanel, "1");
  
		btnDownload.setVisible(false);
		btnDownload.addEventListener(Events.ON_CLICK, e -> downloadCurrent());
  
		// 关闭时先清除预览资源，再关闭附件，最后 detach
		Button btnClose = new Button("关闭");
		btnClose.addEventListener(Events.ON_CLICK, e -> {
			clearPreview();
			if (m_attachment != null) {
				m_attachment.close();
				m_attachment = null;
			}
			this.detach();
		});
  
		btnPanel.appendChild(btnDownload);
		btnPanel.appendChild(btnClose);
		south.appendChild(btnPanel);
	}
  
	// -----------------------------------------------------------------------
	@Override
	public void onEvent(Event event) throws Exception {
		if (Events.ON_SELECT.equals(event.getName()) && event.getTarget() == fileList) {
			Listitem sel = fileList.getSelectedItem();
			if (sel != null)
				previewEntry((Integer) sel.getValue());
		} else {
			super.onEvent(event);
		}
	}
  
	// -----------------------------------------------------------------------
	private void previewEntry(int index) {
		MAttachmentEntry entry = m_attachment.getEntry(index);
		if (entry == null)
			return;
		currentEntry = entry;
		btnDownload.setVisible(true);
  
		// 用 clearPreview() 统一清除，替代原来分散的三段清除代码
		clearPreview();
  
		String mimeType = entry.getContentType();
		if (mimeType == null)
			mimeType = "application/octet-stream";
  
		if (PREVIEWABLE.contains(mimeType)) {
			// 直接用 Iframe 预览（图片、PDF、文本等）
			try {
				byte[] data = entry.getData();
				AMedia media = new AMedia(entry.getName(), null, mimeType, data);
				preview.setContent(media);
				preview.setVisible(true);
			} catch (Exception e) {
				log.log(Level.WARNING, "Cannot preview: " + entry.getName(), e);
				showStatus("预览失败：" + e.getMessage());
			}
		} else {
			// 尝试通过 IMediaView 扩展点预览（如 Excel、CSV 等）
			String extension = getExtension(entry.getName());
			IMediaView view = Extensions.getMediaView(mimeType, extension, false);
			if (view != null) {
				try {
					byte[] data = entry.getData();
					AMedia media = new AMedia(entry.getName(), null, mimeType, new ByteArrayInputStream(data));
					customPreviewComponent = view.renderMediaView(previewContainer, media, true);
				} catch (Exception e) {
					log.log(Level.WARNING, "Cannot preview: " + entry.getName(), e);
					showStatus("预览失败：" + e.getMessage());
				}
			} else {
				showStatus("文件类型 [" + mimeType + "] 不支持预览，请点击下载按钮");
			}
		}
	}
  
	// -----------------------------------------------------------------------
	/**
	 * 清除预览区域的所有内容（Iframe src、自定义预览组件、状态标签）。 参考 WAttachment.clearPreview()
	 * 的实现，统一管理预览资源的释放。
	 */
	private void clearPreview() {
		preview.setSrc(null);
		preview.setVisible(false);
		statusLabel.setVisible(false);
		if (customPreviewComponent != null) {
			customPreviewComponent.detach();
			customPreviewComponent = null;
		}
	}
  
	// -----------------------------------------------------------------------
	/**
	 * 从文件名中提取小写扩展名（不含点），例如 "report.xlsx" → "xlsx"。
	 */
	private String getExtension(String fileName) {
		if (fileName == null)
			return "";
		int dot = fileName.lastIndexOf('.');
		return dot >= 0 ? fileName.substring(dot + 1).toLowerCase() : "";
	}
  
	// -----------------------------------------------------------------------
	private void downloadCurrent() {
		if (currentEntry == null)
			return;
		try {
			String mimeType = currentEntry.getContentType();
			if (mimeType == null)
				mimeType = "application/octet-stream";
			byte[] data = currentEntry.getData();
			AMedia media = new AMedia(currentEntry.getName(), null, mimeType, data);
			Filedownload.save(media);
		} catch (Exception e) {
			log.log(Level.WARNING, "Cannot download: " + currentEntry.getName(), e);
		}
	}
  
	// -----------------------------------------------------------------------
	private void showStatus(String msg) {
		clearPreview();
		statusLabel.setValue(msg);
		statusLabel.setVisible(true);
	}
  
	// -----------------------------------------------------------------------
	/** 以弹窗形式打开，而非嵌入标签页 */
	@Override
	public Window.Mode getWindowMode() {
		return Window.Mode.HIGHLIGHTED;
	}
}