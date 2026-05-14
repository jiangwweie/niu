package com.xiaoniu.aftermarket.reimbursement.dto;

import lombok.Data;

@Data
public class CancelReimbursementCommand {

    private Long storeId;
    private Long reimbursementId;
    private Long operatorId;
    private String cancelReason;
}
