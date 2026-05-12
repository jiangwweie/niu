package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum InventoryFlowType {
    INBOUND("INBOUND"),
    RESERVE("RESERVE"),
    RELEASE("RELEASE"),
    CONSUME("CONSUME"),
    ADJUST("ADJUST");

    private final String code;

    InventoryFlowType(String code) {
        this.code = code;
    }
}
