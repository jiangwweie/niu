package com.xiaoniu.aftermarket.payment.service;

import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.dto.PaymentRecordResponse;
import com.xiaoniu.aftermarket.payment.dto.PaymentSummaryResponse;
import java.math.BigDecimal;
import java.util.List;

public interface PaymentService {

    Long recordPayment(RecordPaymentCommand command);

    BigDecimal sumPaidAmount(Long workOrderId);

    BigDecimal calculateReceivedAmount(Long workOrderId);

    List<PaymentRecordResponse> listByWorkOrderId(Long workOrderId);

    PaymentSummaryResponse getPaymentSummary(Long storeId, Long workOrderId);
}
