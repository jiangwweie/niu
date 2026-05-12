package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum OfficialSettlementStatus {
    NOT_REQUIRED("NOT_REQUIRED"),
    PENDING("PENDING"),
    SETTLED("SETTLED");

    private final String code;

    OfficialSettlementStatus(String code) {
        this.code = code;
    }
}
