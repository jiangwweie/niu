package com.xiaoniu.aftermarket.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.AuditableEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("work_order_status_log")
public class WorkOrderStatusLogEntity extends AuditableEntity {

    private Long storeId;
    private Long workOrderId;
    private String fromStatus;
    private String toStatus;
    private String actionType;
    private Long operatorId;
    private LocalDateTime operatedAt;
    private String reason;
    private String remark;
}
