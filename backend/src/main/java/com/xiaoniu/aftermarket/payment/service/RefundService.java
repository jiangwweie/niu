package com.xiaoniu.aftermarket.payment.service;

import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import java.math.BigDecimal;

public interface RefundService {

    Long recordRefund(RecordRefundCommand command);

    BigDecimal sumRefundAmount(Long workOrderId);

    BigDecimal calculateRefundableAmount(Long workOrderId);
}
