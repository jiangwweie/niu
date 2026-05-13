package com.xiaoniu.aftermarket.inventory.controller;

import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record InventoryStockDetailResponse(
        Long id,
        Long storeId,
        Long partId,
        String partCode,
        String partName,
        String partSource,
        Integer actualQty,
        Integer availableQty,
        Integer reservedQty,
        Long lastFlowId,
        LocalDateTime lastChangedAt
) {

    public static InventoryStockDetailResponse fromQueryResponse(InventoryStockQueryResponse r) {
        return new InventoryStockDetailResponse(
                r.getId(),
                r.getStoreId(),
                r.getPartId(),
                r.getPartCode(),
                r.getPartName(),
                r.getPartSource(),
                r.getActualQty(),
                r.getAvailableQty(),
                r.getReservedQty(),
                r.getLastFlowId(),
                r.getLastChangedAt()
        );
    }
}
