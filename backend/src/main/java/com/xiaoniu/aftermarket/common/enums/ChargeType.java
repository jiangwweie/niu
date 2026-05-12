package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum ChargeType {
    PART("PART"),
    LABOR("LABOR"),
    OTHER("OTHER");

    private final String code;

    ChargeType(String code) {
        this.code = code;
    }
}
