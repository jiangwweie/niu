package com.xiaoniu.aftermarket.reimbursement.service;

import com.xiaoniu.aftermarket.reimbursement.dto.ConfirmReimbursementCommand;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.reimbursement.dto.CancelReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.dto.ReimbursementQueryRequest;
import com.xiaoniu.aftermarket.reimbursement.dto.ReimbursementResponse;
import com.xiaoniu.aftermarket.reimbursement.dto.RejectReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.dto.SubmitReimbursementCommand;

public interface ReimbursementService {

    Long submit(SubmitReimbursementCommand command);

    PageResponse<ReimbursementResponse> pageQuery(ReimbursementQueryRequest request);

    ReimbursementResponse getById(Long storeId, Long reimbursementId);

    void confirm(ConfirmReimbursementCommand command);

    void reject(RejectReimbursementCommand command);

    void cancel(CancelReimbursementCommand command);
}
