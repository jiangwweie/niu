package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;

public record StaffInventoryStockItem(
        Long partId,
        String partCode,
        String partName,
        String partSource,
        Integer actualQty,
        Integer availableQty,
        Integer reservedQty
) {

    public static StaffInventoryStockItem from(InventoryStockQueryResponse r) {
        return new StaffInventoryStockItem(
                r.getPartId(),
                r.getPartCode(),
                r.getPartName(),
                r.getPartSource(),
                r.getActualQty(),
                r.getAvailableQty(),
                r.getReservedQty()
        );
    }
}
