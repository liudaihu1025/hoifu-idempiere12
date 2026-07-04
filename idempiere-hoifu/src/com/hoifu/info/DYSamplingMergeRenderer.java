package com.hoifu.info;  
  
import java.util.List;
import java.util.Objects;

import org.adempiere.webui.component.ListModelTable;
import org.adempiere.webui.component.WInfoWindowListItemRenderer;
import org.adempiere.webui.component.WListbox;
import org.adempiere.webui.info.InfoWindow;
import org.compiere.minigrid.ColumnInfo;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
  
/**
 * 打样评审信息窗口的合并单元格渲染器。 根据 DocumentNo（打样评审编号）对相同编号的连续行进行视觉合并： 同一编号的第 2
 * 行起，Name/phasetype/DocumentNo/reviewstatus/finalcomment 列内容被清空，
 * 且行与行之间的分隔线也被去掉，实现真正的视觉合并效果。
 */  
public class DYSamplingMergeRenderer extends WInfoWindowListItemRenderer {  
  
    /** 用于判断是否同一评审单的关键列名 */  
    private static final String MERGE_KEY_COLUMN = "DocumentNo";  
  
	/** 同一评审单的第 2 行起需要清空的列名（对应 AD_InfoColumn 中配置的 ColumnName） */
    private static final String[] MERGE_COLUMN_NAMES = {  
        "Name",  
        "phasetype",  
        "DocumentNo",  
        "finalcomment"  
    };  
  
	/** 需要设置 tooltip 的列名（鼠标悬停显示完整内容） */
	private static final String[] TOOLTIP_COLUMNS = { "finalcomment", "Description", "reviewcomment" };

	/** 列元数据，由外部调用 setColumnInfos() 注入 */
    private ColumnInfo[] myColumnInfos = null;  
  
    /** 上一行的关键列值，用于判断是否同组 */  
    private Object lastMergeKeyValue = null;  
  
    /**  
     * 上一个渲染的 Listitem 引用。  
     * 用于在检测到同一组时，去掉上一行的下边框，实现视觉上的行合并。  
     */  
    private Listitem lastListitem = null;  
  
    public DYSamplingMergeRenderer(InfoWindow infoWindow) {  
        super(infoWindow);  
    }  
  
    /**  
     * 注入列元数据，必须在 renderItems() 之前调用。  
     * 类比 VoucherMergeRenderer.setRModel()。  
     *  
     * @param columnInfos InfoWindow.columnInfos（protected 字段，由子类传入）  
     */  
    public void setColumnInfos(ColumnInfo[] columnInfos) {  
        this.myColumnInfos = columnInfos;  
    }  
  
	/**
	 * 按列名查找列索引，使用公开 API，逻辑与 InfoWindow.findColumnIndex() 完全一致。
	 * 
	 * @param columnName AD_InfoColumn 中配置的 ColumnName
	 * @return 列在 ListModelTable 中的索引，找不到时返回 -1
	 */
    private int findColumnIndex(String columnName) {  
        if (myColumnInfos == null) return -1;  
        for (int i = 0; i < myColumnInfos.length; i++) {  
			if (myColumnInfos[i].getGridField() != null
                    && myColumnInfos[i].getGridField().getColumnName().equalsIgnoreCase(columnName)) {  
                return i;  
            }  
        }  
        return -1;  
    }  
  
    @Override  
    public void render(Listitem item, Object data, int index) throws Exception {  
		super.render(item, data, index);

        if (myColumnInfos == null) return;  

		// 对需要 tooltip 的列设置悬停提示，方便查看完整内容。
		// 必须在 isSameGroup 清空操作之前执行，否则同组第 2 行的 label 已被清空，取不到文本。
		for (String tooltipCol : TOOLTIP_COLUMNS) {
			int tooltipColIdx = findColumnIndex(tooltipCol);
			if (tooltipColIdx < 0)
				continue;
			List<?> allCells = item.getChildren();
			if (tooltipColIdx >= allCells.size())
				continue;
			Object cellObj = allCells.get(tooltipColIdx);
			if (cellObj instanceof Listcell) {
				Listcell lc = (Listcell) cellObj;
				String text = lc.getLabel();
				if (text != null && !text.isEmpty()) {
					lc.setTooltiptext(text);
				}
			}
		}

        Listbox listbox = item.getListbox();  
        if (!(listbox instanceof WListbox)) return;  
        ListModelTable model = (ListModelTable) ((WListbox) listbox).getModel();  

		// 取当前行的关键列值（从数据模型取，不依赖 Listcell）
        int mergeKeyColIndex = findColumnIndex(MERGE_KEY_COLUMN);  
        if (mergeKeyColIndex < 0) return;  

        Object currentKey = model.getDataAt(index, mergeKeyColIndex);  

        boolean isSameGroup = Objects.equals(currentKey, lastMergeKeyValue) && currentKey != null;  

        if (isSameGroup) {  
            List<?> cells = item.getChildren();  

            // ① 清空指定列的内容（视觉合并）  
            for (String columnName : MERGE_COLUMN_NAMES) {  
                int colIdx = findColumnIndex(columnName);  
                if (colIdx < 0 || colIdx >= cells.size()) continue;  
                Object cellObj = cells.get(colIdx);  
                if (cellObj instanceof Listcell) {  
                    Listcell lc = (Listcell) cellObj;  
                    lc.setLabel("");  
                    lc.getChildren().clear();  
                    lc.setValue("");  
                }  
            }  

			// ② 只去掉合并列的上边框（其他列保持原样）
			for (String columnName : MERGE_COLUMN_NAMES) {
				int colIdx = findColumnIndex(columnName);
				if (colIdx < 0 || colIdx >= cells.size())
					continue;
				Object cellObj = cells.get(colIdx);
                if (cellObj instanceof Listcell) {  
                    appendCellStyle((Listcell) cellObj, "border-top: none;");  
                }  
            }  

			// ③ 只去掉上一行合并列的下边框（与当前行的上边框配合，彻底消除分隔线）
            if (lastListitem != null) {  
				List<?> prevCells = lastListitem.getChildren();
				for (String columnName : MERGE_COLUMN_NAMES) {
					int colIdx = findColumnIndex(columnName);
					if (colIdx < 0 || colIdx >= prevCells.size())
						continue;
					Object cellObj = prevCells.get(colIdx);
                    if (cellObj instanceof Listcell) {  
                        appendCellStyle((Listcell) cellObj, "border-bottom: none;");  
                    }  
                }  
            }  
        }  

        lastMergeKeyValue = currentKey;  
        lastListitem = item;  
	}
  
	/**
	 * 向 Listcell 追加内联样式，不覆盖已有样式。
	 */
    private void appendCellStyle(Listcell cell, String style) {  
        String existing = cell.getStyle();  
        if (existing == null || existing.isEmpty()) {  
            cell.setStyle(style);  
        } else {  
			// 避免重复追加
            if (!existing.contains(style.trim())) {  
                cell.setStyle(existing + " " + style);  
            }  
        }  
    }  
  
    @Override  
    public Listitem newListitem(Listbox listbox) {  
        // 分页/刷新时重置所有状态，防止跨页错误合并  
        lastMergeKeyValue = null;  
        lastListitem = null;  
        return super.newListitem(listbox);  
    }  
}