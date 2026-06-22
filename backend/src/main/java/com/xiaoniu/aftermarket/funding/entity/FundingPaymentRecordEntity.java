package com.xiaoniu.aftermarket.funding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("funding_payment_record")
public class FundingPaymentRecordEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long ledgerId;
    private Long installmentPlanId;
    private String paymentNo;
    private BigDecimal amount;
    private String paymentMethod;
    private LocalDateTime paidAt;
    private Long operatorId;
    private String remark;
}
