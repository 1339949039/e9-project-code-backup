package com.action.archives;
import com.action.archives.server.ArchivePreArchiveServer;
import com.action.archives.wf.AbsAction;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.RequestInfo;
/**
 * 档案预备归档案
 *
 * @author Li Yu Feng
 * @date 2023-03-07 14:09
 */
public class ArchivePreArchiveAction extends AbsAction {


    @Override
    public String execute(RequestInfo requestInfo) {

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                ArchivePreArchiveServer server = new ArchivePreArchiveServer();
                server.sendRequestArchive(Integer.valueOf(requestInfo.getRequestid()));
            }
        };
        new Thread(runnable).start();
        return Action.SUCCESS;//不拦截
    }
}
