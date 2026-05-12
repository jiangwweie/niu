package com.xiaoniu.aftermarket.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("refund_record")
public class RefundRecordEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long workOrderId;
    private String refundNo;
    private BigDecimal amount;
    private String refundMethod;
    private LocalDateTime refundedAt;
    private Long operatorId;
    private String reason;
    private String remark;
}
