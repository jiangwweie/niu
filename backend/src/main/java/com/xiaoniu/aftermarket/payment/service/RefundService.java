package com.xiaoniu.aftermarket.payment.service;

import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.dto.RefundRecordResponse;
import java.math.BigDecimal;
import java.util.List;

public interface RefundService {

    Long recordRefund(RecordRefundCommand command);

    BigDecimal sumRefundAmount(Long workOrderId);

    BigDecimal calculateRefundableAmount(Long workOrderId);

    List<RefundRecordResponse> listByWorkOrderId(Long workOrderId);
}
