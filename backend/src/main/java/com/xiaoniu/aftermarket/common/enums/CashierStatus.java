package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum CashierStatus {
    NO_CHARGE("NO_CHARGE", "无需收款"),
    UNPAID("UNPAID", "未收款"),
    PARTIAL_PAID("PARTIAL_PAID", "部分收款"),
    PAID("PAID", "已收齐"),
    REFUND_PENDING("REFUND_PENDING", "待退款"),
    PARTIAL_REFUNDED("PARTIAL_REFUNDED", "部分退款"),
    REFUNDED("REFUNDED", "已退清");

    private final String code;
    private final String text;

    CashierStatus(String code, String text) {
        this.code = code;
        this.text = text;
    }
}
