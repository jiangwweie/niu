package com.xiaoniu.aftermarket.payment.application;

import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.service.RefundService;
import org.springframework.stereotype.Service;

@Service
public class RecordRefundApplicationService {

    private final RefundService refundService;

    public RecordRefundApplicationService(RefundService refundService) {
        this.refundService = refundService;
    }

    public Long execute(RecordRefundCommand command) {
        return refundService.recordRefund(command);
    }
}
