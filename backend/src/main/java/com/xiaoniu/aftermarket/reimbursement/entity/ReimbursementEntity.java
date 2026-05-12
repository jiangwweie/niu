package com.xiaoniu.aftermarket.reimbursement.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("reimbursement")
public class ReimbursementEntity extends SoftDeleteEntity {

    private Long storeId;
    private String reimbursementNo;
    private Long applicantId;
    private String purpose;
    private BigDecimal amount;
    private BigDecimal confirmedAmount;
    private String status;
    private LocalDateTime submittedAt;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectReason;
    private String remark;
}
