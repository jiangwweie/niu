package com.xiaoniu.aftermarket.workorder.dto;

import lombok.Data;

@Data
public class WorkOrderQueryRequest {

    private Long storeId;
    private String status;
    private String workOrderNo;
    private String customerName;
    private Integer pageNo;
    private Integer pageSize;
}
