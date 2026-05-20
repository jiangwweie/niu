package com.xiaoniu.aftermarket.customer.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class VehicleResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String model;
    private String frameNo;
    private String batteryNo;
    private String remark;
    private LocalDateTime lastRepairAt;
    private LocalDateTime createdAt;
}
