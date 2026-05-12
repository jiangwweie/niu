package com.xiaoniu.aftermarket.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("payment_record")
public class PaymentRecordEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long workOrderId;
    private String paymentNo;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private Long receiverId;
    private Long operatorId;
    private String remark;
}
