package com.hoifu.process;  
  
import org.adempiere.exceptions.AdempiereException;  
import org.compiere.model.MOrg;  
import org.compiere.model.MTable;  
import org.compiere.process.SvrProcess;  
import org.compiere.util.Msg;  
  
import com.hoifu.model.MInOutNotice;  
import com.hoifu.service.InOutNoticeService;  
  
@org.adempiere.base.annotation.Process  
public class CreateNoticeFromNoticeProcess extends SvrProcess {  
  
    @Override  
    protected void prepare() {  
    }  
  
    @Override  
    protected String doIt() throws Exception {  
        int bNoticeId = getRecord_ID();  
        if (bNoticeId <= 0)  
            throw new AdempiereException("未找到发货通知单，请在发货通知单窗口中运行此流程");  
  
        MInOutNotice bNotice = new MInOutNotice(getCtx(), bNoticeId, get_TrxName());  
        if (bNotice.get_ID() == 0)  
            throw new AdempiereException("发货通知单不存在，ID=" + bNoticeId);  
  
        if (!"CO".equals(bNotice.getDocStatus()))  
            throw new AdempiereException(  
                    "发货通知单必须是已完成状态，当前状态：" + bNotice.getDocStatus());  
  
        InOutNoticeService service = new InOutNoticeService();  
  
        if (!service.isBToANotice(bNotice, get_TrxName())) {  
            String bOrgName = MOrg.get(getCtx(), bNotice.getAD_Org_ID()).getName();  
            throw new AdempiereException(  
                    "【" + bOrgName + "】的发货通知单关联销售订单无委外关联，无法创建对应的发货通知单");  
        }  
  
        MInOutNotice aNotice = service.createNoticeFromBNotice(bNotice, get_TrxName());  
  
        String aOrgName = MOrg.get(getCtx(), aNotice.getAD_Org_ID()).getName();  
        
//        addLog(0, null, null,  
//                Msg.parseTranslation(getCtx(), "@M_InOutNotice_ID@: " + aNotice.getDocumentNo()),  
//                MTable.getTable_ID(MInOutNotice.Table_Name),  
//                aNotice.getM_InOutNotice_ID());  
  
        return "已创建【" + aOrgName + "】发货通知单：" + aNotice.getDocumentNo();  
    }  
}