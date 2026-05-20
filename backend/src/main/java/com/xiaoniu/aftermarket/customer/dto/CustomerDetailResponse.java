package com.xiaoniu.aftermarket.customer.dto;

import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class CustomerDetailResponse {
    private Long id;
    private String customerName;
    private String phone;
    private String remark;
    private LocalDateTime createdAt;
    private List<VehicleBriefResponse> vehicles;
    private List<WorkOrderQueryResponse> recentWorkOrders;
}
