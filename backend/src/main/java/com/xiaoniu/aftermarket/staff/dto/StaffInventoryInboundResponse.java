package com.xiaoniu.aftermarket.staff.dto;

import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import java.time.LocalDateTime;

public record StaffInventoryInboundResponse(
    Long partId,
    String partCode,
    String partName,
    Integer actualQty,
    Integer availableQty,
    Integer reservedQty,
    Long flowId,
    LocalDateTime operatedAt
) {

    public static StaffInventoryInboundResponse from(InventoryStockEntity stock, PartEntity part,
                                                      InventoryFlowEntity flow) {
        return new StaffInventoryInboundResponse(
            stock.getPartId(),
            part != null ? part.getPartCode() : null,
            part != null ? part.getPartName() : null,
            stock.getActualQty(),
            stock.getAvailableQty(),
            stock.getReservedQty(),
            flow != null ? flow.getId() : null,
            flow != null ? flow.getOperatedAt() : null
        );
    }
}