package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum InventoryStatus {
    NOT_RESERVED("NOT_RESERVED", "未预占"),
    RESERVED("RESERVED", "已预占"),
    CONSUMED("CONSUMED", "已扣减"),
    RELEASED("RELEASED", "已释放");

    private final String code;
    private final String text;

    InventoryStatus(String code, String text) {
        this.code = code;
        this.text = text;
    }
}
