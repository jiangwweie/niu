package com.xiaoniu.aftermarket.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class InventoryFlowQueryResponse {

    private Long id;
    private Long storeId;
    private Long partId;
    private String partCode;
    private String partName;
    private String flowType;
    private Integer quantityDelta;
    private Integer actualBefore;
    private Integer actualAfter;
    private Integer availableBefore;
    private Integer availableAfter;
    private Integer reservedBefore;
    private Integer reservedAfter;
    private String businessType;
    private Long businessId;
    private Long operatorId;
    private LocalDateTime operatedAt;
    private String reason;
    private String remark;
    private BigDecimal unitCost;
}
