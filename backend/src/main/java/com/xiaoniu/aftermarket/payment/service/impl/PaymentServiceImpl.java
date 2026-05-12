package com.xiaoniu.aftermarket.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.PaymentMethod;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.payment.dto.PaymentRecordResponse;
import com.xiaoniu.aftermarket.payment.dto.PaymentSummaryResponse;
import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.entity.PaymentRecordEntity;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.payment.service.PaymentService;
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

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> PAYMENT_METHOD_CODES = Arrays.stream(PaymentMethod.values())
            .map(PaymentMethod::getCode)
            .collect(Collectors.toUnmodifiableSet());

    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final WorkOrderMapper workOrderMapper;
    private final SequenceService sequenceService;

    public PaymentServiceImpl(PaymentRecordMapper paymentRecordMapper,
                              RefundRecordMapper refundRecordMapper,
                              WorkOrderMapper workOrderMapper,
                              SequenceService sequenceService) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.workOrderMapper = workOrderMapper;
        this.sequenceService = sequenceService;
    }

    @Override
    @Transactional
    public Long recordPayment(RecordPaymentCommand command) {
        validatePaymentCommand(command);

        WorkOrderEntity workOrder = loadWorkOrderForPayment(command.getStoreId(), command.getWorkOrderId());
        validatePayableStatus(workOrder);

        PaymentRecordEntity entity = new PaymentRecordEntity();
        entity.setStoreId(workOrder.getStoreId());
        entity.setWorkOrderId(workOrder.getId());
        entity.setPaymentNo(sequenceService.next("PAYMENT"));
        entity.setAmount(normalizeAmount(command.getAmount()));
        entity.setPaymentMethod(command.getPaymentMethod());
        entity.setPaidAt(command.getPaidAt() != null ? command.getPaidAt() : LocalDateTime.now());
        entity.setReceiverId(command.getReceiverId());
        entity.setOperatorId(command.getOperatorId());
        entity.setRemark(command.getRemark());
        entity.setCreatedBy(command.getOperatorId());
        paymentRecordMapper.insert(entity);

        updateReceivedAmount(workOrder.getId());
        return entity.getId();
    }

    @Override
    public BigDecimal sumPaidAmount(Long workOrderId) {
        return normalizeAmount(paymentRecordMapper.sumAmountByWorkOrderId(workOrderId));
    }

    @Override
    public BigDecimal calculateReceivedAmount(Long workOrderId) {
        return sumPaidAmount(workOrderId).subtract(sumRefundAmount(workOrderId)).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public List<PaymentRecordResponse> listByWorkOrderId(Long workOrderId) {
        return paymentRecordMapper.selectByWorkOrderId(workOrderId).stream()
                .map(this::toPaymentRecordResponse)
                .toList();
    }

    @Override
    public PaymentSummaryResponse getPaymentSummary(Long storeId, Long workOrderId) {
        if (storeId == null || workOrderId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null || workOrder.getStoreId() == null || !storeId.equals(workOrder.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }

        BigDecimal paymentTotal = sumPaidAmount(workOrderId);
        BigDecimal refundTotal = sumRefundAmount(workOrderId);
        BigDecimal receivedAmount = paymentTotal.subtract(refundTotal).setScale(2, RoundingMode.HALF_UP);

        PaymentSummaryResponse response = new PaymentSummaryResponse();
        response.setWorkOrderId(workOrderId);
        response.setReceivableAmount(normalizeAmount(workOrder.getReceivableAmount()));
        response.setPaymentTotal(paymentTotal);
        response.setRefundTotal(refundTotal);
        response.setReceivedAmount(receivedAmount);
        response.setCanSettle(receivedAmount.compareTo(response.getReceivableAmount()) >= 0);
        return response;
    }

    private void validatePaymentCommand(RecordPaymentCommand command) {
        if (command.getStoreId() == null || command.getWorkOrderId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.OPERATOR_REQUIRED);
        }
        if (command.getReceiverId() == null) {
            throw new BusinessException(ErrorCode.RECEIVER_REQUIRED);
        }
        if (command.getAmount() == null || command.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_AMOUNT_INVALID);
        }
        if (!PAYMENT_METHOD_CODES.contains(command.getPaymentMethod())) {
            throw new BusinessException(ErrorCode.PAYMENT_METHOD_INVALID);
        }
    }

    private WorkOrderEntity loadWorkOrderForPayment(Long storeId, Long workOrderId) {
        WorkOrderEntity workOrder = workOrderMapper.selectByIdForUpdate(workOrderId);
        if (workOrder == null || workOrder.getStoreId() == null || !storeId.equals(workOrder.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        return workOrder;
    }

    private void validatePayableStatus(WorkOrderEntity workOrder) {
        if (WorkOrderStatus.DRAFT.getCode().equals(workOrder.getStatus())
                || WorkOrderStatus.CANCELLED.getCode().equals(workOrder.getStatus())
                || WorkOrderStatus.SETTLED.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID);
        }
    }

    private BigDecimal sumRefundAmount(Long workOrderId) {
        return normalizeAmount(refundRecordMapper.sumAmountByWorkOrderId(workOrderId));
    }

    private void updateReceivedAmount(Long workOrderId) {
        BigDecimal receivedAmount = calculateReceivedAmount(workOrderId);
        UpdateWrapper<WorkOrderEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", workOrderId).set("received_amount", receivedAmount);
        workOrderMapper.update(null, wrapper);
    }

    private PaymentRecordResponse toPaymentRecordResponse(PaymentRecordEntity entity) {
        PaymentRecordResponse response = new PaymentRecordResponse();
        response.setId(entity.getId());
        response.setWorkOrderId(entity.getWorkOrderId());
        response.setPaymentNo(entity.getPaymentNo());
        response.setAmount(entity.getAmount());
        response.setPaymentMethod(entity.getPaymentMethod());
        response.setPaidAt(entity.getPaidAt());
        response.setReceiverId(entity.getReceiverId());
        response.setOperatorId(entity.getOperatorId());
        response.setRemark(entity.getRemark());
        return response;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return (amount != null ? amount : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
}
