package com.xiaoniu.aftermarket.workorder.dto;

import lombok.Data;

@Data
public class CancelWorkOrderCommand {

    private Long storeId;
    private Long workOrderId;
    private Long operatorId;
    private String reason;
}
