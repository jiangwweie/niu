package com.xiaoniu.aftermarket.workorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WorkOrderQueryResponse {

    private Long id;
    private String workOrderNo;
    private String customerNameSnapshot;
    private String customerPhoneSnapshot;
    private String vehicleModelSnapshot;
    private String frameNoSnapshot;
    private Boolean officialAfterSales;
    private String officialOrderNo;
    private String status;
    private BigDecimal receivableAmount;
    private BigDecimal receivedAmount;
    private LocalDateTime createdAt;
}
