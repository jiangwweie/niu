package com.xiaoniu.aftermarket.funding.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("funding_installment_plan")
public class FundingInstallmentPlanEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long ledgerId;
    private Integer phaseNo;
    private String phaseName;
    private LocalDate dueDate;
    private BigDecimal receivableAmount;
    private BigDecimal receivedAmount;
    private String status;
    private String remark;
}
