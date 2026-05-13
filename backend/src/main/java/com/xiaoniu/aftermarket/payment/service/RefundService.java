package com.xiaoniu.aftermarket.payment.service;

import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.dto.RefundRecordResponse;
import com.xiaoniu.aftermarket.payment.dto.RefundQueryRequest;
import com.xiaoniu.aftermarket.payment.dto.RefundQueryResponse;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import java.math.BigDecimal;
import java.util.List;

public interface RefundService {

    Long recordRefund(RecordRefundCommand command);

    BigDecimal sumRefundAmount(Long workOrderId);

    BigDecimal calculateRefundableAmount(Long workOrderId);

    List<RefundRecordResponse> listByWorkOrderId(Long workOrderId);

    PageResponse<RefundQueryResponse> pageQuery(RefundQueryRequest request);
}
