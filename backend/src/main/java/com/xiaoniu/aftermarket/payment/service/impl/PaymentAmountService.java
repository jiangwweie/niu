package com.xiaoniu.aftermarket.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class PaymentAmountService {

    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final WorkOrderMapper workOrderMapper;

    PaymentAmountService(PaymentRecordMapper paymentRecordMapper,
                         RefundRecordMapper refundRecordMapper,
                         WorkOrderMapper workOrderMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.workOrderMapper = workOrderMapper;
    }

    public BigDecimal sumPaidAmount(Long workOrderId) {
        return normalizeAmount(paymentRecordMapper.sumAmountByWorkOrderId(workOrderId));
    }

    public BigDecimal sumRefundAmount(Long workOrderId) {
        return normalizeAmount(refundRecordMapper.sumAmountByWorkOrderId(workOrderId));
    }

    public BigDecimal calculateReceivedAmount(Long workOrderId) {
        return sumPaidAmount(workOrderId)
                .subtract(sumRefundAmount(workOrderId))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal updateReceivedAmount(Long workOrderId) {
        BigDecimal receivedAmount = calculateReceivedAmount(workOrderId);
        UpdateWrapper<WorkOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", workOrderId).set("received_amount", receivedAmount);
        workOrderMapper.update(null, wrapper);
        return receivedAmount;
    }

    public BigDecimal normalizeAmount(BigDecimal amount) {
        return (amount != null ? amount : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
