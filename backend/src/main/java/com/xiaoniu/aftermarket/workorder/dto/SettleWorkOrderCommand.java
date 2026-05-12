package com.xiaoniu.aftermarket.workorder.dto;

import lombok.Data;

@Data
public class SettleWorkOrderCommand {

    private Long workOrderId;
    private Long operatorId;
    private String remark;
}
