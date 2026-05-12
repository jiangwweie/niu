package com.xiaoniu.aftermarket.reimbursement.service;

import com.xiaoniu.aftermarket.reimbursement.dto.ConfirmReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.entity.ReimbursementEntity;

public interface ReimbursementService {

    Long submit(ReimbursementEntity reimbursement);

    void confirm(ConfirmReimbursementCommand command);

    void reject(Long reimbursementId, Long operatorId, String reason);

    void cancel(Long reimbursementId, Long operatorId, String reason);
}
