package com.xiaoniu.aftermarket.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.PaymentMethod;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.dto.RefundRecordResponse;
import com.xiaoniu.aftermarket.payment.entity.RefundRecordEntity;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.payment.service.RefundService;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class RefundServiceImpl implements RefundService {

    private static final Set<String> PAYMENT_METHOD_CODES = Arrays.stream(PaymentMethod.values())
            .map(PaymentMethod::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private final RefundRecordMapper refundRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final WorkOrderMapper workOrderMapper;
    private final SequenceService sequenceService;

    public RefundServiceImpl(RefundRecordMapper refundRecordMapper,
                             PaymentRecordMapper paymentRecordMapper,
                             WorkOrderMapper workOrderMapper,
                             SequenceService sequenceService) {
        this.refundRecordMapper = refundRecordMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.workOrderMapper = workOrderMapper;
        this.sequenceService = sequenceService;
    }

    @Override
    @Transactional
    public Long recordRefund(RecordRefundCommand command) {
        validateRefundCommand(command);

        WorkOrderEntity workOrder = loadWorkOrderForRefund(command.getStoreId(), command.getWorkOrderId());
        validateRefundableStatus(workOrder);

        BigDecimal refundableAmount = calculateRefundableAmount(workOrder.getId());
        BigDecimal refundAmount = normalizeAmount(command.getAmount());
        if (refundableAmount.compareTo(BigDecimal.ZERO) <= 0 || refundAmount.compareTo(refundableAmount) > 0) {
            throw new BusinessException(ErrorCode.REFUND_EXCEEDS_PAID_AMOUNT);
        }

        RefundRecordEntity entity = new RefundRecordEntity();
        entity.setStoreId(workOrder.getStoreId());
        entity.setWorkOrderId(workOrder.getId());
        entity.setRefundNo(sequenceService.next("REFUND"));
        entity.setAmount(refundAmount);
        entity.setRefundMethod(command.getRefundMethod());
        entity.setRefundedAt(command.getRefundedAt() != null ? command.getRefundedAt() : LocalDateTime.now());
        entity.setOperatorId(command.getOperatorId());
        entity.setReason(command.getReason());
        entity.setRemark(command.getRemark());
        entity.setCreatedBy(command.getOperatorId());
        refundRecordMapper.insert(entity);

        updateReceivedAmount(workOrder.getId());
        return entity.getId();
    }

    @Override
    public BigDecimal sumRefundAmount(Long workOrderId) {
        return normalizeAmount(refundRecordMapper.sumAmountByWorkOrderId(workOrderId));
    }

    @Override
    public BigDecimal calculateRefundableAmount(Long workOrderId) {
        return sumPaidAmount(workOrderId).subtract(sumRefundAmount(workOrderId)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<RefundRecordResponse> listByWorkOrderId(Long workOrderId) {
        return refundRecordMapper.selectByWorkOrderId(workOrderId).stream()
                .map(this::toRefundRecordResponse)
                .toList();
    }

    private void validateRefundCommand(RecordRefundCommand command) {
        if (command.getStoreId() == null || command.getWorkOrderId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.OPERATOR_REQUIRED);
        }
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.REFUND_AMOUNT_INVALID);
        }
        if (!PAYMENT_METHOD_CODES.contains(command.getRefundMethod())) {
            throw new BusinessException(ErrorCode.REFUND_METHOD_INVALID);
        }
        if (!StringUtils.hasText(command.getReason())) {
            throw new BusinessException(ErrorCode.REFUND_REASON_REQUIRED);
        }
    }

    private WorkOrderEntity loadWorkOrderForRefund(Long storeId, Long workOrderId) {
        WorkOrderEntity workOrder = workOrderMapper.selectByIdForUpdate(workOrderId);
        if (workOrder == null || workOrder.getStoreId() == null || !storeId.equals(workOrder.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        return workOrder;
    }

    private void validateRefundableStatus(WorkOrderEntity workOrder) {
        if (WorkOrderStatus.DRAFT.getCode().equals(workOrder.getStatus())
                || WorkOrderStatus.CANCELLED.getCode().equals(workOrder.getStatus())
                || WorkOrderStatus.SETTLED.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID);
        }
    }

    private BigDecimal sumPaidAmount(Long workOrderId) {
        return normalizeAmount(paymentRecordMapper.sumAmountByWorkOrderId(workOrderId));
    }

    private void updateReceivedAmount(Long workOrderId) {
        BigDecimal receivedAmount = calculateRefundableAmount(workOrderId);
        UpdateWrapper<WorkOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", workOrderId).set("received_amount", receivedAmount);
        workOrderMapper.update(null, wrapper);
    }

    private RefundRecordResponse toRefundRecordResponse(RefundRecordEntity entity) {
        RefundRecordResponse response = new RefundRecordResponse();
        response.setId(entity.getId());
        response.setWorkOrderId(entity.getWorkOrderId());
        response.setRefundNo(entity.getRefundNo());
        response.setAmount(entity.getAmount());
        response.setRefundMethod(entity.getRefundMethod());
        response.setRefundedAt(entity.getRefundedAt());
        response.setOperatorId(entity.getOperatorId());
        response.setReason(entity.getReason());
        response.setRemark(entity.getRemark());
        return response;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return (amount != null ? amount : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
