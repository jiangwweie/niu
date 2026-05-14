package com.xiaoniu.aftermarket.reimbursement.application;

import com.xiaoniu.aftermarket.reimbursement.dto.ConfirmReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.service.ReimbursementService;
import org.springframework.stereotype.Service;

@Service
public class ConfirmReimbursementApplicationService {

    private final ReimbursementService reimbursementService;

    public ConfirmReimbursementApplicationService(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    public void execute(ConfirmReimbursementCommand command) {
        reimbursementService.confirm(command);
    }
}
