package com.gitssie.openapi.data;

import io.vavr.Value;
import io.vavr.control.Option;

public class Code {
    public final int code;
    private String errCode;
    private String message;
    private Value errors;
    public static final Code OK = new Code(0, "OK", "OK");
    public static final Code CANCELLED = new Code(1, "CANCELLED", "CANCELLED");
    public static final Code UNKNOWN = new Code(2, "UNKNOWN", "UNKNOWN");
    public static final Code INVALID_ARGUMENT = new Code(3, "INVALID_ARGUMENT", "INVALID_ARGUMENT");
    public static final Code DEADLINE_EXCEEDED = new Code(4, "DEADLINE_EXCEEDED", "DEADLINE_EXCEEDED");
    public static final Code NOT_FOUND = new Code(5, "NOT_FOUND", "NOT_FOUND");
    public static final Code ALREADY_EXISTS = new Code(6, "ALREADY_EXISTS", "ALREADY_EXISTS");
    public static final Code PERMISSION_DENIED = new Code(7, "PERMISSION_DENIED", "PERMISSION_DENIED");
    public static final Code RESOURCE_EXHAUSTED = new Code(8, "RESOURCE_EXHAUSTED", "RESOURCE_EXHAUSTED");
    public static final Code FAILED_PRECONDITION = new Code(9, "FAILED_PRECONDITION", "FAILED_PRECONDITION");
    public static final Code ABORTED = new Code(10, "ABORTED", "ABORTED");
    public static final Code OUT_OF_RANGE = new Code(11, "OUT_OF_RANGE", "OUT_OF_RANGE");
    public static final Code UNIMPLEMENTED = new Code(12, "UNIMPLEMENTED", "UNIMPLEMENTED");
    public static final Code INTERNAL = new Code(13, "INTERNAL", "INTERNAL ERROR");
    public static final Code UNAVAILABLE = new Code(14, "UNAVAILABLE", "SYSTEM UNAVAILABLE");
    public static final Code DATA_LOSS = new Code(15, "DATA_LOSS", "DATA_LOSS");
    public static final Code UNAUTHENTICATED = new Code(16, "UNAUTHENTICATED", "UNAUTHENTICATED");
    public static final Code INCORRECT_PARAME = new Code(303, "INCORRECT_PARAME", "参数不正确");
    public static final Code CONTRACT_EXIST = new Code(303009, "CONTRACT_EXIST", "合同号已经存在");
    public static final Code MCH_ERROR = new Code(303015, "MCH_ERROR", "商户信息不正确");

    public static final Code SYSTEM_ERROR = new Code(500, "SYSTEM_ERROR", "系统内部错误");
    public static final Code PROCESS_FAIL = new Code(999, "PROCESS_FAIL", "处理失败");
    public static final Code SIGN_ERROR = new Code(303003, "SIGN_ERROR", "签名错误");

    public static final Code TRADENO_NOT_EXIST = new Code(303010, "TRADENO_NOT_EXIST", "交易号不存在");
    public static final Code CONTRACT_SUCCESS = new Code(303020, "CONTRACT_SUCCESS", "该合同号已上报成功");
    public static final Code CONTRACT_NOT_FOUND = new Code(303021, "CONTRACT_NOT_FOUND", "未找到对应合同");

    public static final Code CONTRACT_NOT_REPORT = new Code(303022, "CONTRACT_NOT_REPORT", "该合同还未上报");

    public static final Code CHANNEL_ERROR = new Code(303023, "CHANNEL_ERROR", "渠道信息错误");

    public static final Code CHANNEL_MCH_NOT_MATCH = new Code(303024, "CHANNEL_MCH_NOT_MATCH", "渠道与商户不匹配");

    public Code(int code) {
        this.code = code;
    }

    public Code(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public Code(String errCode, String message) {
        this(FAILED_PRECONDITION.code, errCode, message);
    }

    public Code(int code, String errCode, String message) {
        this.code = code;
        this.errCode = errCode;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getErrCode() {
        return errCode;
    }

    public String getMessage() {
        return message;
    }

    public Code withMessage(String message) {
        return new Code(code, errCode, message);
    }

    public Value getErrors() {
        return errors;
    }

    public Code withErrors(Value errors) {
        Code res = new Code(code, errCode, message);
        res.errors = errors;
        return res;
    }

    public Code withErrors(Throwable ex) {
        return withErrors(Option.of(ex));
    }

    @Override
    public String toString() {
        return code + ":" + errCode + ":" + message;
    }

    public static Code fromThrowable(Throwable e) {
        return new Code(INTERNAL.getCode(), INTERNAL.getErrCode(), e.getMessage());
    }

    public static Code notFound(String message) {
        return Code.NOT_FOUND.withMessage(message);
    }

    public static Code notFound() {
        return Code.NOT_FOUND;
    }

    public CodeException toException() {
        return new CodeException(toString());
    }
}
