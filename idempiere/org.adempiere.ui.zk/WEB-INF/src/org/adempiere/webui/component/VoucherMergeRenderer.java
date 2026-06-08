package org.adempiere.webui.component;

import java.util.List;
import java.util.Objects;

import org.compiere.report.core.RModel;
import org.compiere.util.CLogger;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;

/**
 * 自定义凭证列表渲染器，实现合并单元格效果
 */  
public class VoucherMergeRenderer extends WListItemRenderer {

	/** 日志记录器 */
	private static final CLogger logger = CLogger.getCLogger(VoucherMergeRenderer.class);

	/** 上一个凭证号 */
    private Integer lastVoucherSeqNo = null;  
	/** 数据模型引用 */
	private RModel m_rmodel = null;
	/** 表格模型引用 */
	private ListModelTable m_tableModel = null;

	/** 需要合并的列名 */
	private static final String[] MERGE_COLUMN_NAMES = { "DateAcct", // 日期
			"C_Period_ID", // 期间
			"Voucher_SeqNo", // 凭证号
			"Voucher_Description", // 摘要
			"AD_Org_ID" // 组织
	};

	/**
	 * 设置数据模型
	 */
	public void setRModel(RModel rmodel) {
		if (rmodel == null) {
			logger.warning("尝试设置空的RModel");
			return;
		}
		this.m_rmodel = rmodel;
		logger.info("RModel已设置，列数: " + rmodel.getColumnCount());
	}

	/**
	 * 设置表格模型
	 */
	public void setTableModel(ListModelTable tableModel) {
		if (tableModel == null) {
			logger.warning("尝试设置空的ListModelTable");
			return;
		}
		this.m_tableModel = tableModel;
		logger.info("ListModelTable已设置，行数: " + tableModel.getSize());
	}
      
	@Override
	public void render(Listitem item, Object data, int index) throws Exception {
		super.render(item, data, index);

		if (m_rmodel == null || m_tableModel == null) {
			logger.warning("数据模型未初始化，跳过渲染，索引: " + index);
			return;
		}

		// 获取当前行的凭证号
		Integer currentVoucherSeqNo = null;

		try {
			int voucherSeqNoColumn = m_rmodel.getColumnIndex("Voucher_SeqNo");
			if (voucherSeqNoColumn >= 0) {
				Object voucherSeqNoObj = m_tableModel.getDataAt(index, voucherSeqNoColumn);
				if (voucherSeqNoObj instanceof Integer) {
					currentVoucherSeqNo = (Integer) voucherSeqNoObj;
				}
			}
		} catch (Exception e) {
			logger.warning("获取凭证号失败，索引: " + index + ", 错误: " + e.getMessage());
			return;
		}

		// 判断是否为新的凭证行
		boolean isNewVoucher = !Objects.equals(currentVoucherSeqNo, lastVoucherSeqNo);

		// 如果不是新凭证且凭证号不为空，则隐藏冗余字段
		if (!isNewVoucher && currentVoucherSeqNo != null) {
			List<?> cells = item.getChildren();

			// 处理固定列名的列
			for (String columnName : MERGE_COLUMN_NAMES) {
				try {
					int colIndex = m_rmodel.getColumnIndex(columnName);
					if (colIndex >= 0 && colIndex < cells.size()) {
						Component cell = (Component) cells.get(colIndex);
						if (cell instanceof Listcell) {
							Listcell listcell = (Listcell) cell;
							listcell.setLabel("");
							listcell.getChildren().clear();
							listcell.setValue("");
						}
					}
				} catch (Exception e) {
					continue;
				}
			}

			// 单独处理凭证字列（使用显示名称匹配）
			try {
				for (int i = 0; i < m_rmodel.getColumnCount(); i++) {
					String displayHeader = m_rmodel.getRColumn(i).getColHeader();
					if (displayHeader.contains("凭证字")) {
						Component cell = (Component) cells.get(i);
						if (cell instanceof Listcell) {
							Listcell listcell = (Listcell) cell;
							listcell.setLabel("");
							listcell.getChildren().clear();
							listcell.setValue("");
						}
						break;
					}
				}
			} catch (Exception e) {
				logger.warning("隐藏凭证字列失败，错误: " + e.getMessage());
			}
		}
          
        // 更新状态  
        lastVoucherSeqNo = currentVoucherSeqNo;  
    }  
      
    @Override  
	public Listitem newListitem(Listbox listbox) {
		Listitem item = super.newListitem(listbox);
		// 重置状态，用于分页或刷新
        lastVoucherSeqNo = null;  
		return item;
    }  
}