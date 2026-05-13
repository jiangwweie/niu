package com.xiaoniu.aftermarket.official.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OfficialAfterSalesQueryResponse {

    private Long id;
    private Long storeId;
    private Long workOrderId;
    private String workOrderNo;
    private String customerNameSnapshot;
    private String customerPhoneSnapshot;
    private String vehicleModelSnapshot;
    private String frameNoSnapshot;
    private BigDecimal receivedAmount;
    private String workOrderStatus;
    private String officialOrderNo;
    private BigDecimal settlementAmount;
    private String settlementStatus;
    private LocalDateTime settlementTime;
}
