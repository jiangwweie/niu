package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum WorkOrderStatus {
    DRAFT("DRAFT"),
    PENDING_ACCEPT("PENDING_ACCEPT"),
    ACCEPTED("ACCEPTED"),
    PART_ORDERED("PART_ORDERED"),
    PART_ARRIVED("PART_ARRIVED"),
    SETTLED("SETTLED"),
    CANCELLED("CANCELLED");

    private final String code;

    WorkOrderStatus(String code) {
        this.code = code;
    }
}
