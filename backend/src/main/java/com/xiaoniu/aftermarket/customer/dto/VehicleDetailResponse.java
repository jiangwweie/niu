package com.xiaoniu.aftermarket.customer.dto;

import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class VehicleDetailResponse {
    private Long id;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String model;
    private String frameNo;
    private String batteryNo;
    private String remark;
    private LocalDateTime createdAt;
    private List<WorkOrderQueryResponse> recentWorkOrders;
}
