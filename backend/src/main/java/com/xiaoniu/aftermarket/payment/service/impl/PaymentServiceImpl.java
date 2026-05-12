package com.xiaoniu.aftermarket.payment.service.impl;

import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.service.PaymentService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRecordMapper paymentRecordMapper;

    public PaymentServiceImpl(PaymentRecordMapper paymentRecordMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
    }

    @Override
    public Long recordPayment(RecordPaymentCommand command) {
        throw new UnsupportedOperationException("TODO: implement payment recording in Phase 4");
    }

    @Override
    public BigDecimal sumPaidAmount(Long workOrderId) {
        return paymentRecordMapper.sumAmountByWorkOrderId(workOrderId);
    }

    @Override
    public BigDecimal calculateReceivedAmount(Long workOrderId) {
        throw new UnsupportedOperationException("TODO: implement received amount calculation in Phase 4");
    }
}
