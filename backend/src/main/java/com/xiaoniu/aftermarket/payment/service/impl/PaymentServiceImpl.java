package com.xiaoniu.aftermarket.payment.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.PaymentMethod;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.payment.dto.PaymentQueryRequest;
import com.xiaoniu.aftermarket.payment.dto.PaymentQueryResponse;
import com.xiaoniu.aftermarket.payment.dto.PaymentRecordResponse;
import com.xiaoniu.aftermarket.payment.dto.PaymentSummaryResponse;
import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.entity.PaymentRecordEntity;
import com.xiaoniu.aftermarket.payment.mapper.PaymentRecordMapper;
import com.xiaoniu.aftermarket.payment.service.PaymentService;
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
public class PaymentServiceImpl implements PaymentService {

    private static final Set<String> PAYMENT_METHOD_CODES = Arrays.stream(PaymentMethod.values())
            .map(PaymentMethod::getCode)
            .collect(Collectors.toUnmodifiableSet());
    private static final Set<String> PAYABLE_STATUSES = Set.of(
            WorkOrderStatus.PENDING_ACCEPT.getCode(),
            WorkOrderStatus.ACCEPTED.getCode(),
            WorkOrderStatus.PART_ORDERED.getCode(),
            WorkOrderStatus.PART_ARRIVED.getCode()
    );

    private final PaymentRecordMapper paymentRecordMapper;
    private final WorkOrderMapper workOrderMapper;
    private final SequenceService sequenceService;
    private final PaymentAmountService paymentAmountService;

    public PaymentServiceImpl(PaymentRecordMapper paymentRecordMapper,
                              WorkOrderMapper workOrderMapper,
                              SequenceService sequenceService,
                              PaymentAmountService paymentAmountService) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.workOrderMapper = workOrderMapper;
        this.sequenceService = sequenceService;
        this.paymentAmountService = paymentAmountService;
    }

    @Override
    @Transactional
    public Long recordPayment(RecordPaymentCommand command) {
        validatePaymentCommand(command);

        WorkOrderEntity workOrder = loadWorkOrderForPayment(command.getStoreId(), command.getWorkOrderId());
        validatePayableStatus(workOrder);

        // Overpayment guard: check inside @Transactional, after selectByIdForUpdate lock on work_order.
        // Uses net received (payments - refunds), so after a refund the freed amount can be re-collected.
        // "Overpayment" means: netReceived + thisPayment > receivable.
        // Concurrent payment requests are serialized by the pessimistic lock, so two requests cannot
        // both read a stale receivedAmount and pass this check simultaneously.
        BigDecimal normalizedAmount = paymentAmountService.normalizeAmount(command.getAmount());
        BigDecimal netReceived = paymentAmountService.calculateReceivedAmount(command.getWorkOrderId());
        BigDecimal newTotal = netReceived.add(normalizedAmount);
        BigDecimal receivable = paymentAmountService.normalizeAmount(workOrder.getReceivableAmount());
        if (newTotal.compareTo(receivable) > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_EXCEEDS_RECEIVABLE);
        }

        PaymentRecordEntity entity = new PaymentRecordEntity();
        entity.setStoreId(workOrder.getStoreId());
        entity.setWorkOrderId(workOrder.getId());
        entity.setPaymentNo(sequenceService.next("PAYMENT"));
        entity.setAmount(paymentAmountService.normalizeAmount(command.getAmount()));
        entity.setPaymentMethod(command.getPaymentMethod());
        entity.setPaidAt(command.getPaidAt() != null ? command.getPaidAt() : LocalDateTime.now());
        entity.setReceiverId(command.getReceiverId());
        entity.setOperatorId(command.getOperatorId());
        entity.setRemark(command.getRemark());
        entity.setCreatedBy(command.getOperatorId());
        paymentRecordMapper.insert(entity);

        paymentAmountService.updateReceivedAmount(workOrder.getId());
        return entity.getId();
    }

    @Override
    public BigDecimal sumPaidAmount(Long workOrderId) {
        return paymentAmountService.sumPaidAmount(workOrderId);
    }

    @Override
    public BigDecimal calculateReceivedAmount(Long workOrderId) {
        return paymentAmountService.calculateReceivedAmount(workOrderId);
    }

    @Override
    public List<PaymentRecordResponse> listByWorkOrderId(Long workOrderId) {
        return paymentRecordMapper.selectByWorkOrderId(workOrderId).stream()
                .map(this::toPaymentRecordResponse)
                .toList();
    }

    @Override
    public PageResponse<PaymentQueryResponse> pageQuery(PaymentQueryRequest request) {
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

        QueryWrapper<PaymentRecordEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId()).eq("deleted", 0);
        if (filteredWorkOrderIds != null) {
            wrapper.in("work_order_id", filteredWorkOrderIds);
        }
        if (StringUtils.hasText(request.getPaymentMethod())) {
            wrapper.eq("payment_method", request.getPaymentMethod().trim());
        }
        if (request.getStartTime() != null) {
            wrapper.ge("paid_at", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le("paid_at", request.getEndTime());
        }

        long total = paymentRecordMapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResponse<>(List.of(), pageNo, pageSize, 0);
        }

        wrapper.orderByDesc("paid_at").orderByDesc("id")
                .last("LIMIT " + pageSize + " OFFSET " + (long) (pageNo - 1) * pageSize);
        List<PaymentRecordEntity> entities = paymentRecordMapper.selectList(wrapper);
        Map<Long, WorkOrderEntity> workOrderMap = loadWorkOrders(entities.stream()
                .map(PaymentRecordEntity::getWorkOrderId)
                .toList());
        List<PaymentQueryResponse> records = entities.stream()
                .map(entity -> toPaymentQueryResponse(entity, workOrderMap.get(entity.getWorkOrderId())))
                .toList();
        return new PageResponse<>(records, pageNo, pageSize, total);
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

        BigDecimal paymentTotal = paymentAmountService.sumPaidAmount(workOrderId);
        BigDecimal refundTotal = paymentAmountService.sumRefundAmount(workOrderId);
        BigDecimal receivedAmount = paymentAmountService.calculateReceivedAmount(workOrderId);

        PaymentSummaryResponse response = new PaymentSummaryResponse();
        response.setWorkOrderId(workOrderId);
        response.setReceivableAmount(paymentAmountService.normalizeAmount(workOrder.getReceivableAmount()));
        response.setPaymentTotal(paymentTotal);
        response.setRefundTotal(refundTotal);
        response.setReceivedAmount(receivedAmount);
        response.setCanSettle(PAYABLE_STATUSES.contains(workOrder.getStatus())
                && receivedAmount.compareTo(response.getReceivableAmount()) >= 0);
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
        if (!PAYABLE_STATUSES.contains(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.PAYMENT_WORK_ORDER_STATUS_INVALID);
        }
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

    private PaymentQueryResponse toPaymentQueryResponse(PaymentRecordEntity entity, WorkOrderEntity workOrder) {
        PaymentQueryResponse response = new PaymentQueryResponse();
        response.setId(entity.getId());
        response.setWorkOrderId(entity.getWorkOrderId());
        response.setWorkOrderNo(workOrder == null ? null : workOrder.getWorkOrderNo());
        response.setCustomerNameSnapshot(workOrder == null ? null : workOrder.getCustomerNameSnapshot());
        response.setPaymentNo(entity.getPaymentNo());
        response.setAmount(entity.getAmount());
        response.setPaymentMethod(entity.getPaymentMethod());
        response.setPaidAt(entity.getPaidAt());
        response.setReceiverId(entity.getReceiverId());
        response.setOperatorId(entity.getOperatorId());
        response.setRemark(entity.getRemark());
        return response;
    }

}
