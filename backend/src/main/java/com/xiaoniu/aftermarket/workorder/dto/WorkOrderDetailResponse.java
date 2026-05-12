package com.xiaoniu.aftermarket.workorder.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class WorkOrderDetailResponse {

    private Long id;
    private Long storeId;
    private String workOrderNo;
    private Long customerId;
    private Long vehicleId;
    private String customerNameSnapshot;
    private String customerPhoneSnapshot;
    private String vehicleModelSnapshot;
    private String frameNoSnapshot;
    private String batteryNoSnapshot;
    private String repairItem;
    private String status;
    private BigDecimal receivableAmount;
    private String remark;
    private LocalDateTime createdAt;
    private List<WorkOrderChargeItemResponse> chargeItems;
}
