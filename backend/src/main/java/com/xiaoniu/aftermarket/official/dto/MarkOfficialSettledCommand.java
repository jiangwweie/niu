package com.xiaoniu.aftermarket.official.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarkOfficialSettledCommand {

    private Long storeId;
    private Long workOrderId;
    private BigDecimal settlementAmount;
    private LocalDateTime settlementTime;
    private Long operatorId;
    private String remark;
}
