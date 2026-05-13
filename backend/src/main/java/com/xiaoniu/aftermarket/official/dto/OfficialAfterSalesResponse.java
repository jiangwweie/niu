package com.xiaoniu.aftermarket.official.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfficialAfterSalesResponse {

    private Long id;
    private Long storeId;
    private Long workOrderId;
    private String workOrderNo;
    private Boolean officialAfterSales;
    private String officialOrderNo;
    private BigDecimal settlementAmount;
    private String settlementStatus;
    private LocalDateTime settlementTime;
    private Long operatorId;
    private String settlementRemark;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
