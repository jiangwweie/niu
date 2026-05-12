package com.xiaoniu.aftermarket.inventory.dto;

import lombok.Data;

@Data
public class InventoryInboundCommand {

    private Long storeId;
    private Long partId;
    private Integer quantity;
    private Long operatorId;
    private String reason;
    private String remark;
}
