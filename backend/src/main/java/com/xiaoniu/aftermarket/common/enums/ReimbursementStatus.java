package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum ReimbursementStatus {
    PENDING("PENDING"),
    CONFIRMED("CONFIRMED"),
    REJECTED("REJECTED"),
    CANCELLED("CANCELLED");

    private final String code;

    ReimbursementStatus(String code) {
        this.code = code;
    }
}
