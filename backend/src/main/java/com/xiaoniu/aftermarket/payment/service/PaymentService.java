package com.xiaoniu.aftermarket.payment.service;

import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import java.math.BigDecimal;

public interface PaymentService {

    Long recordPayment(RecordPaymentCommand command);

    BigDecimal sumPaidAmount(Long workOrderId);

    BigDecimal calculateReceivedAmount(Long workOrderId);
}
