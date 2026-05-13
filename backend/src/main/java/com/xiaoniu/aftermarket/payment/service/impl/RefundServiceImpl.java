package com.xiaoniu.aftermarket.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.PaymentMethod;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.dto.RefundQueryRequest;
import com.xiaoniu.aftermarket.payment.dto.RefundQueryResponse;
import com.xiaoniu.aftermarket.payment.dto.RefundRecordResponse;
import com.xiaoniu.aftermarket.payment.entity.RefundRecordEntity;
import com.xiaoniu.aftermarket.payment.mapper.RefundRecordMapper;
import com.xiaoniu.aftermarket.payment.service.RefundService;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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

    @Override
    public PageResponse<RefundQueryResponse> pageQuery(RefundQueryRequest request) {
        if (request == null || request.getStoreId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }

        int pageNo = request.normalizedPageNo();
        int pageSize = request.normalizedPageSize();
        List<Long> filteredWorkOrderIds = findWorkOrderIds(request.getStoreId(),
                request.getWorkOrderNo(), request.getCustomerName());
        if (filteredWorkOrderIds != null && filteredWorkOrderIds.isEmpty()) {
            return new PageResponse<>(List.of(), pageNo, pageSize, 0);
        }

        QueryWrapper<RefundRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId()).eq("deleted", 0);
        if (filteredWorkOrderIds != null) {
            wrapper.in("work_order_id", filteredWorkOrderIds);
        }
        if (StringUtils.hasText(request.getRefundMethod())) {
            wrapper.eq("refund_method", request.getRefundMethod().trim());
        }
        if (request.getStartTime() != null) {
            wrapper.ge("refunded_at", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le("refunded_at", request.getEndTime());
        }

        long total = refundRecordMapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResponse<>(List.of(), pageNo, pageSize, 0);
        }

        wrapper.orderByDesc("refunded_at").orderByDesc("id")
                .last("LIMIT " + pageSize + " OFFSET " + (long) (pageNo - 1) * pageSize);
        List<RefundRecordEntity> entities = refundRecordMapper.selectList(wrapper);
        Map<Long, WorkOrderEntity> workOrderMap = loadWorkOrders(entities.stream()
                .map(RefundRecordEntity::getWorkOrderId)
                .toList());
        List<RefundQueryResponse> records = entities.stream()
                .map(entity -> toRefundQueryResponse(entity, workOrderMap.get(entity.getWorkOrderId())))
                .toList();
        return new PageResponse<>(records, pageNo, pageSize, total);
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

    private List<Long> findWorkOrderIds(Long storeId, String workOrderNo, String customerName) {
        if (!StringUtils.hasText(workOrderNo) && !StringUtils.hasText(customerName)) {
            return null;
        }
        QueryWrapper<WorkOrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId).eq("deleted", 0);
        if (StringUtils.hasText(workOrderNo)) {
            wrapper.like("work_order_no", workOrderNo.trim());
        }
        if (StringUtils.hasText(customerName)) {
            wrapper.like("customer_name_snapshot", customerName.trim());
        }
        return workOrderMapper.selectList(wrapper).stream()
                .map(WorkOrderEntity::getId)
                .toList();
    }

    private Map<Long, WorkOrderEntity> loadWorkOrders(List<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return workOrderMapper.selectBatchIds(workOrderIds).stream()
                .collect(Collectors.toMap(WorkOrderEntity::getId, Function.identity(), (left, right) -> left));
    }

    private RefundQueryResponse toRefundQueryResponse(RefundRecordEntity entity, WorkOrderEntity workOrder) {
        RefundQueryResponse response = new RefundQueryResponse();
        response.setId(entity.getId());
        response.setWorkOrderId(entity.getWorkOrderId());
        response.setWorkOrderNo(workOrder == null ? null : workOrder.getWorkOrderNo());
        response.setCustomerNameSnapshot(workOrder == null ? null : workOrder.getCustomerNameSnapshot());
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
