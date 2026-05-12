package com.xiaoniu.aftermarket.inventory.dto;

import lombok.Data;

@Data
public class InventoryFlowQueryRequest {

    private Long storeId;
    private Long partId;
    private String flowType;
    private String partCode;
    private String partName;
    private Integer pageNo;
    private Integer pageSize;
}
