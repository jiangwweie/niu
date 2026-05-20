package com.xiaoniu.aftermarket.customer.dto;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class CustomerResponse {
    private Long id;
    private String customerName;
    private String phone;
    private String remark;
    private Integer vehicleCount;
    private LocalDateTime lastRepairAt;
    private LocalDateTime createdAt;
}
