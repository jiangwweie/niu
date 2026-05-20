package com.xiaoniu.aftermarket.dashboard.mapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class RecentWorkOrderRow {

    private Long id;
    private String workOrderNo;
    private String customerName;
    private String status;
    private BigDecimal receivableAmount;
    private BigDecimal receivedAmount;
    private LocalDateTime createdAt;
}
