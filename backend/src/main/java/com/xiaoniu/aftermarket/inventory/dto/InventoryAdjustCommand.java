package com.xiaoniu.aftermarket.inventory.dto;

import lombok.Data;

@Data
public class InventoryAdjustCommand {

    private Long storeId;
    private Long partId;
    private Integer quantityDelta;
    private Long operatorId;
    private String reason;
    private String remark;
}
