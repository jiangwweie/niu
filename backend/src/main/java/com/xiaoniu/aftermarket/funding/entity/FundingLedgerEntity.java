package com.xiaoniu.aftermarket.funding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("funding_ledger")
public class FundingLedgerEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long applicationId;
    private Long contractId;
    private String ledgerNo;
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
    private BigDecimal receivedAmount;
    private BigDecimal outstandingAmount;
    private String groupLeader;
    private String handlerName;
    private String status;
    private String remark;
}
