package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import java.time.LocalDateTime;

public record StaffInventoryStockDetail(
        Long partId,
        String partCode,
        String partName,
        String partSource,
        Integer actualQty,
        Integer availableQty,
        Integer reservedQty,
        LocalDateTime lastChangedAt
) {

    public static StaffInventoryStockDetail from(InventoryStockEntity entity, PartEntity part) {
        return new StaffInventoryStockDetail(
                entity.getPartId(),
                part != null ? part.getPartCode() : null,
                part != null ? part.getPartName() : null,
                part != null ? part.getSource() : null,
                entity.getActualQty(),
                entity.getAvailableQty(),
                entity.getReservedQty(),
                entity.getLastChangedAt()
        );
    }
}
