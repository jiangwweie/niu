package com.xiaoniu.aftermarket.payment.service.impl;

import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.PaymentMethod;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.dto.RefundRecordResponse;
import com.xiaoniu.aftermarket.payment.entity.RefundRecordEntity;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.payment.service.RefundService;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import java.math.BigDecimal;
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
    private static final Set<String> REFUNDABLE_STATUSES = Set.of(
            WorkOrderStatus.PENDING_ACCEPT.getCode(),
            WorkOrderStatus.ACCEPTED.getCode(),
            WorkOrderStatus.PART_ORDERED.getCode(),
            WorkOrderStatus.PART_ARRIVED.getCode()
    );

    private final RefundRecordMapper refundRecordMapper;
    private final WorkOrderMapper workOrderMapper;
    private final SequenceService sequenceService;
    private final PaymentAmountService paymentAmountService;

    public RefundServiceImpl(RefundRecordMapper refundRecordMapper,
                             WorkOrderMapper workOrderMapper,
                             SequenceService sequenceService,
                             PaymentAmountService paymentAmountService) {
        this.refundRecordMapper = refundRecordMapper;
        this.workOrderMapper = workOrderMapper;
        this.sequenceService = sequenceService;
        this.paymentAmountService = paymentAmountService;
    }

    @Override
    @Transactional
    public Long recordRefund(RecordRefundCommand command) {
        validateRefundCommand(command);

        WorkOrderEntity workOrder = loadWorkOrderForRefund(command.getStoreId(), command.getWorkOrderId());
        validateRefundableStatus(workOrder);

        BigDecimal refundableAmount = calculateRefundableAmount(workOrder.getId());
        BigDecimal refundAmount = paymentAmountService.normalizeAmount(command.getAmount());
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

        paymentAmountService.updateReceivedAmount(workOrder.getId());
        return entity.getId();
    }

    @Override
    public BigDecimal sumRefundAmount(Long workOrderId) {
        return paymentAmountService.sumRefundAmount(workOrderId);
    }

    @Override
    public BigDecimal calculateRefundableAmount(Long workOrderId) {
        return paymentAmountService.calculateReceivedAmount(workOrderId);
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
        if (!REFUNDABLE_STATUSES.contains(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID);
        }
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

}
