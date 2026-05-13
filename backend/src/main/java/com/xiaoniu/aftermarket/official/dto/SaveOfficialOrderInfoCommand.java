package com.xiaoniu.aftermarket.official.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveOfficialOrderInfoCommand {

    private Long storeId;
    private Long workOrderId;
    private String officialOrderNo;
    private Long operatorId;
    private String remark;
}
