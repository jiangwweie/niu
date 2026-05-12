package com.xiaoniu.aftermarket.reimbursement.service.impl;

import com.xiaoniu.aftermarket.reimbursement.dto.ConfirmReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.entity.ReimbursementEntity;
import com.xiaoniu.aftermarket.reimbursement.service.ReimbursementService;
import org.springframework.stereotype.Service;

@Service
public class ReimbursementServiceImpl implements ReimbursementService {

    @Override
    public Long submit(ReimbursementEntity reimbursement) {
        throw new UnsupportedOperationException("TODO: implement reimbursement submission in Phase 6");
    }

    @Override
    public void confirm(ConfirmReimbursementCommand command) {
        throw new UnsupportedOperationException("TODO: implement reimbursement confirmation in Phase 6");
    }

    @Override
    public void reject(Long reimbursementId, Long operatorId, String reason) {
        throw new UnsupportedOperationException("TODO: implement reimbursement rejection in Phase 6");
    }

    @Override
    public void cancel(Long reimbursementId, Long operatorId, String reason) {
        throw new UnsupportedOperationException("TODO: implement reimbursement cancellation in Phase 6");
    }
}
