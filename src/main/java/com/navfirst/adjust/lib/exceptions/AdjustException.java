package com.navfirst.adjust.lib.exceptions;

import lombok.Getter;

/** 保留 Go 的稳定错误码、字段及记录 ID；message 用于日志，不用于分支判断。 */
@Getter
public class AdjustException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String field;
    private final String id;

    public AdjustException() {
        this("internal_error", "Unknown adjustment error");
    }

    public AdjustException(String message) {
        this("internal_error", message);
    }

    public AdjustException(Exception cause) {
        this("internal_error", cause.getMessage(), cause);
    }

    public AdjustException(int code, String message) {
        this(Integer.toString(code), message);
    }

    public AdjustException(String code, String message) {
        this(code, message, null, null, null);
    }

    public AdjustException(String code, String message, Throwable cause) {
        this(code, message, null, null, cause);
    }

    public AdjustException(String code, String message, String field, String id) {
        this(code, message, field, id, null);
    }

    private AdjustException(String code, String message, String field, String id, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.field = field;
        this.id = id;
    }

    public String getMsg() {
        return getMessage();
    }
}
