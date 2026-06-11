package com.archstudy.checkin.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // Auth related
    USER_ALREADY_EXISTS(1001, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(1002, "邮箱已被注册"),
    INVALID_CREDENTIALS(1003, "用户名或密码错误"),
    USER_DISABLED(1004, "账号已被禁用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
