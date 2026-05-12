package com.xiaoniu.aftermarket.common.api;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS("SUCCESS", "OK"),
    COMMON_BAD_REQUEST("COMMON_BAD_REQUEST", "请求参数错误"),
    COMMON_NOT_FOUND("COMMON_NOT_FOUND", "资源不存在"),
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", "系统异常"),
    SEQUENCE_TYPE_UNKNOWN("SEQUENCE_TYPE_UNKNOWN", "未知编号类型"),
    PART_NAME_REQUIRED("PART_NAME_REQUIRED", "配件名称不能为空"),
    PART_CODE_DUPLICATE("PART_CODE_DUPLICATE", "配件编码已存在"),
    PART_OFFICIAL_CODE_REQUIRED("PART_OFFICIAL_CODE_REQUIRED", "官方配件必须填写品号"),
    PART_NOT_FOUND("PART_NOT_FOUND", "配件不存在"),
    PART_DISABLED("PART_DISABLED", "配件已停用"),
    PART_STOCK_NOT_FOUND("PART_STOCK_NOT_FOUND", "库存记录不存在"),
    INVENTORY_QTY_MUST_POSITIVE("INVENTORY_QTY_MUST_POSITIVE", "入库数量必须大于0"),
    INVENTORY_ADJUST_ZERO("INVENTORY_ADJUST_ZERO", "调整数量不能为0"),
    INVENTORY_ADJUST_REASON_REQUIRED("INVENTORY_ADJUST_REASON_REQUIRED", "调整原因不能为空"),
    INVENTORY_ADJUST_WOULD_NEGATIVE("INVENTORY_ADJUST_WOULD_NEGATIVE", "调整后可用库存不能为负数"),
    INVENTORY_ADJUST_ACTUAL_NEGATIVE("INVENTORY_ADJUST_ACTUAL_NEGATIVE", "调整后实际库存不能为负数");

    private final String code;
    private final String message;

    ErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
