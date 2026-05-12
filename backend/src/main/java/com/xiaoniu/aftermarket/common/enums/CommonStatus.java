package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum CommonStatus {
    ENABLED("ENABLED"),
    DISABLED("DISABLED");

    private final String code;

    CommonStatus(String code) {
        this.code = code;
    }
}
