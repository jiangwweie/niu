package com.xiaoniu.aftermarket.payment.application;

import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.service.PaymentService;
import org.springframework.stereotype.Service;

@Service
public class RecordPaymentApplicationService {

    private final PaymentService paymentService;

    public RecordPaymentApplicationService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public Long execute(RecordPaymentCommand command) {
        return paymentService.recordPayment(command);
    }
}
