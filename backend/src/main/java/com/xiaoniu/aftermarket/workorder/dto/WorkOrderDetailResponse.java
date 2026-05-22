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
    private String noChargeReason;
    private String noChargeRemark;
    private Boolean canMarkRepairDone;
    private Boolean canDeliver;
    private Boolean canCancel;
    private Boolean canRecordPayment;
    private Boolean canRecordRefund;
    private Boolean canRefundAfterDelivery;
    private String remark;
    private LocalDateTime createdAt;
    private List<WorkOrderChargeItemResponse> chargeItems;
}
