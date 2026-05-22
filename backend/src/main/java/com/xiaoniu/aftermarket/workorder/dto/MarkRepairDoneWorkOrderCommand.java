package com.xiaoniu.aftermarket.workorder.dto;

import lombok.Data;

@Data
public class MarkRepairDoneWorkOrderCommand {
    private Long storeId;
    private Long workOrderId;
    private Long operatorId;
    private String noChargeReason;
    private String noChargeRemark;
    private String remark;
}
