package com.action.kinggdee.wf;

import weaver.interfaces.workflow.action.Action;

import java.util.Map;

public class ActionResult {
    public ActionResult() {
        this.init();
    }

    private String status;
    private String message;
    private Throwable exception;

    private Map<String, Object> data;


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public void init() {
        this.status = Action.FAILURE_AND_CONTINUE;
    }

    public void success() {
        this.status = Action.SUCCESS;
    }

    public void failure(String msg) {
        this.status = Action.FAILURE_AND_CONTINUE;
        this.message = msg;
    }

    public void failureSAP(String code, String msg) {
        msg = "(SAP)返回信息：CODE=" + code + ", MSG=" + msg;
        this.failure(msg);
    }

    public Boolean isFailure() {
        return this.status == Action.FAILURE_AND_CONTINUE;
    }

    public Boolean isSuccess() {
        return this.status == Action.SUCCESS;
    }

    // 审批人IDs
    private String approvalIds;

    public String getApprovalIds() {
        return approvalIds;
    }

    public void setApprovalIds(String approvalIds) {
        this.approvalIds = approvalIds;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

}
