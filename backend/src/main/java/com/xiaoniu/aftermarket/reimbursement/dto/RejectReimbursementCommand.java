package com.xiaoniu.aftermarket.reimbursement.dto;

import lombok.Data;

@Data
public class RejectReimbursementCommand {

    private Long storeId;
    private Long reimbursementId;
    private Long operatorId;
    private String rejectReason;
}
