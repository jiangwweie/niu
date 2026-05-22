package com.xiaoniu.aftermarket.common.enums;

import lombok.Getter;

@Getter
public enum WorkOrderStatus {
    DRAFT("DRAFT"),
    REPAIRING("REPAIRING"),
    REPAIR_DONE("REPAIR_DONE"),
    DELIVERED("DELIVERED"),
    CANCELLED("CANCELLED"),
    // Kept only so clean-start preflight and legacy tests can name old values.
    // New business actions must not transition to or from these states.
    PENDING_ACCEPT("PENDING_ACCEPT"),
    ACCEPTED("ACCEPTED"),
    PART_ORDERED("PART_ORDERED"),
    PART_ARRIVED("PART_ARRIVED"),
    SETTLED("SETTLED");

    private final String code;

    WorkOrderStatus(String code) {
        this.code = code;
    }
}
