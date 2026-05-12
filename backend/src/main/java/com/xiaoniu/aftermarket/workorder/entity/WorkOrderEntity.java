package com.xiaoniu.aftermarket.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("work_order")
public class WorkOrderEntity extends SoftDeleteEntity {

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
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private Long settledBy;
    private LocalDateTime settledAt;
    private Long cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private String remark;
}
