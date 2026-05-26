package com.xiaoniu.aftermarket.customer.dto;

import lombok.Data;

@Data
public class VehiclePageQuery {
    private Long storeId;
    private String keyword; // searches frameNo, model, or customer phone
    private String vin;
    private String model;
    private String batteryNo;
    private String customerPhone;
    private Long customerId;
    private Integer pageNo;
    private Integer pageSize;

    public int normalizedPageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public int normalizedPageSize() {
        return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
    }
}
