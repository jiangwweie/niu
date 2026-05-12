package com.xiaoniu.aftermarket.payment.service.impl;

import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.payment.service.RefundService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class RefundServiceImpl implements RefundService {

    private final RefundRecordMapper refundRecordMapper;

    public RefundServiceImpl(RefundRecordMapper refundRecordMapper) {
        this.refundRecordMapper = refundRecordMapper;
    }

    @Override
    public Long recordRefund(RecordRefundCommand command) {
        throw new UnsupportedOperationException("TODO: implement refund recording in Phase 4");
    }

    @Override
    public BigDecimal sumRefundAmount(Long workOrderId) {
        return refundRecordMapper.sumAmountByWorkOrderId(workOrderId);
    }

    @Override
    public BigDecimal calculateRefundableAmount(Long workOrderId) {
        throw new UnsupportedOperationException("TODO: implement refundable amount calculation in Phase 4");
    }
}
