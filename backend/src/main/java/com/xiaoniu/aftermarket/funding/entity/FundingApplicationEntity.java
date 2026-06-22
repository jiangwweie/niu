package com.xiaoniu.aftermarket.funding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("funding_application")
public class FundingApplicationEntity extends SoftDeleteEntity {

    private Long storeId;
    private String applicationNo;
    private String customerName;
    private String phone;
    private String idCardNo;
    private String vehicleModel;
    private LocalDate pickupDate;
    private String paymentType;
    private BigDecimal purchaseCost;
    private BigDecimal incentiveAmount;
    private BigDecimal upstreamAmount;
    private BigDecimal totalCost;
    private BigDecimal retailPrice;
    private BigDecimal receivableAmount;
    private BigDecimal downPayment;
    private Integer installmentCount;
    private BigDecimal installmentAmount;
    private LocalDate firstDueDate;
    private String groupLeader;
    private String handlerName;
    private String addOnRemark;
    private String status;
    private String auditRemark;
    private Long auditedBy;
    private LocalDateTime auditedAt;
    private Long submittedBy;
    private LocalDateTime submittedAt;
    private String remark;
}
