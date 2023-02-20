package com.ler.exception;

public enum ErrorCode {

    /**
     *
     */
    userNoLogin(10001, "用户未登录！"),
    paramError(1001, "参数错误！"),
    NO_PERM(1002, "没有操作权限"),
    QUERYFAILS(1003, "操作失败"),
    serverError(1004, "服务器异常"),
    TooFrequentInvoke(1005, "调用太频繁，请稍候重试"),
    // 正确码
    NoData(1006, "没有数据"),
    dberr(1007, "操作数据库失败"),

    //
    ;

    private int code;

    private String desc;

    ErrorCode(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
