package com.xiaoniu.aftermarket.inventory.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InventoryStockQueryResponse {

    private Long id;
    private Long storeId;
    private Long partId;
    private String partCode;
    private String partName;
    private String partSource;
    private Integer actualQty;
    private Integer availableQty;
    private Integer reservedQty;
    private Long lastFlowId;
    private LocalDateTime lastChangedAt;
}
