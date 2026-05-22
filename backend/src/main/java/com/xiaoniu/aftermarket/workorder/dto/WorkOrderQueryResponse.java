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
    private String progressStatus;
    private String progressStatusText;
    private String cashierStatus;
    private String cashierStatusText;
    private String inventoryStatus;
    private String inventoryStatusText;
    private BigDecimal receivableAmount;
    private BigDecimal receivedAmount;
    private BigDecimal paymentTotal;
    private BigDecimal refundTotal;
    private BigDecimal netReceived;
    private BigDecimal outstandingAmount;
    private BigDecimal refundableAmount;
    private LocalDateTime createdAt;
}
