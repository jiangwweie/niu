package com.xiaoniu.aftermarket.official.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkNoSettlementRequiredCommand {

    private Long storeId;
    private Long workOrderId;
    private Long operatorId;
    private String reason;
    private String remark;
}
