package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum PartSource {
    OFFICIAL("OFFICIAL"),
    THIRD_PARTY("THIRD_PARTY");

    private final String code;

    PartSource(String code) {
        this.code = code;
    }
}
