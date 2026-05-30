package com.xiaoniu.aftermarket.inventory.dto;

import org.springframework.util.StringUtils;

public enum InventoryViewType {
    DEFAULT,
    ALL,
    HAS_STOCK,
    HAS_RESERVED,
    ZERO_STOCK,
    DISABLED_WITH_STOCK,
    ARCHIVED;

    public static InventoryViewType from(String value) {
        if (!StringUtils.hasText(value)) {
            return DEFAULT;
        }
        for (InventoryViewType type : values()) {
            if (type.name().equalsIgnoreCase(value.trim())) {
                return type;
            }
        }
        return DEFAULT;
    }
}
