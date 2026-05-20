package com.xiaoniu.aftermarket.customer.dto;

import lombok.Data;

@Data
public class VehicleBriefResponse {
    private Long id;
    private String model;
    private String frameNo;
    private String batteryNo;
    private String remark;
}
