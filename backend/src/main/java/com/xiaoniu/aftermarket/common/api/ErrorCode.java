package com.xiaoniu.aftermarket.common.api;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS("SUCCESS", "OK"),
    COMMON_BAD_REQUEST("COMMON_BAD_REQUEST", "请求参数错误"),
    COMMON_NOT_FOUND("COMMON_NOT_FOUND", "资源不存在"),
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", "系统异常"),
    SEQUENCE_TYPE_UNKNOWN("SEQUENCE_TYPE_UNKNOWN", "未知编号类型");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
