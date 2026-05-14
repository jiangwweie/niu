package com.xiaoniu.aftermarket.reimbursement.dto;

import com.xiaoniu.aftermarket.common.enums.ReimbursementStatus;
import com.xiaoniu.aftermarket.reimbursement.entity.ReimbursementEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReimbursementResponse {

    private Long id;
    private String reimbursementNo;
    private Long applicantId;
    private String purpose;
    private BigDecimal amount;
    private String status;
    private String remark;
    private LocalDateTime submittedAt;
    private BigDecimal confirmedAmount;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;
    private Long rejectedBy;
    private LocalDateTime rejectedAt;
    private String rejectReason;
    private Long cancelledBy;
    private LocalDateTime cancelledAt;
    private String cancelReason;
    private Boolean costIncluded;

    public static ReimbursementResponse from(ReimbursementEntity entity) {
        ReimbursementResponse response = new ReimbursementResponse();
        response.setId(entity.getId());
        response.setReimbursementNo(entity.getReimbursementNo());
        response.setApplicantId(entity.getApplicantId());
        response.setPurpose(entity.getPurpose());
        response.setAmount(entity.getAmount());
        response.setStatus(entity.getStatus());
        response.setRemark(entity.getRemark());
        response.setSubmittedAt(entity.getSubmittedAt());
        response.setConfirmedAmount(entity.getConfirmedAmount());
        response.setConfirmedBy(entity.getConfirmedBy());
        response.setConfirmedAt(entity.getConfirmedAt());
        response.setRejectedBy(entity.getRejectedBy());
        response.setRejectedAt(entity.getRejectedAt());
        response.setRejectReason(entity.getRejectReason());
        response.setCancelledBy(entity.getCancelledBy());
        response.setCancelledAt(entity.getCancelledAt());
        response.setCancelReason(entity.getCancelReason());
        response.setCostIncluded(ReimbursementStatus.CONFIRMED.getCode().equals(entity.getStatus()));
        return response;
    }
}
