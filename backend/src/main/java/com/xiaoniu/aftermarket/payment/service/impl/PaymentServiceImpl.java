package com.xiaoniu.aftermarket.payment.service.impl;

import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.buildContainsPattern;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.containsCondition;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.normalize;

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
import com.xiaoniu.aftermarket.payment.service.CashierStatusService;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
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
            WorkOrderStatus.REPAIRING.getCode(),
            WorkOrderStatus.REPAIR_DONE.getCode()
    );

    private final PaymentRecordMapper paymentRecordMapper;
    private final WorkOrderMapper workOrderMapper;
    private final SequenceService sequenceService;
    private final PaymentAmountService paymentAmountService;
    private final CashierStatusService cashierStatusService;
    private final SysUserMapper userMapper;

    public PaymentServiceImpl(PaymentRecordMapper paymentRecordMapper,
                              WorkOrderMapper workOrderMapper,
                              SequenceService sequenceService,
                              PaymentAmountService paymentAmountService,
                              CashierStatusService cashierStatusService,
                              SysUserMapper userMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.workOrderMapper = workOrderMapper;
        this.sequenceService = sequenceService;
        this.paymentAmountService = paymentAmountService;
        this.cashierStatusService = cashierStatusService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Long recordPayment(RecordPaymentCommand command) {
        validatePaymentCommand(command);

        WorkOrderEntity workOrder = loadWorkOrderForPayment(command.getStoreId(), command.getWorkOrderId());
        validatePayableStatus(workOrder);

        // 超收校验必须在 selectByIdForUpdate 锁之后执行：
        // 悲观锁保证并发支付请求串行化，防止两个请求同时读到旧的 receivedAmount 都通过校验
        BigDecimal normalizedAmount = paymentAmountService.normalizeAmount(command.getAmount());
        // 实收金额 = 支付总额 - 退款总额（退款后释放的额度可以重新收取）
        BigDecimal netReceived = paymentAmountService.calculateReceivedAmount(command.getWorkOrderId());
        BigDecimal newTotal = netReceived.add(normalizedAmount);
        BigDecimal receivable = paymentAmountService.normalizeAmount(workOrder.getReceivableAmount());
        if (newTotal.compareTo(receivable) > 0) {
            throw new BusinessException(ErrorCode.PAYMENT_EXCEEDS_RECEIVABLE);
        }

        // 支付必须明细化：每次收款插入独立记录，不能直接改工单总额
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

        // 每次支付后同步更新工单的 received_amount，保证结算判断用的是最新值
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
        response.setNetReceived(receivedAmount);
        response.setOutstandingAmount(response.getReceivableAmount().subtract(receivedAmount).max(BigDecimal.ZERO));
        response.setRefundableAmount(receivedAmount.max(BigDecimal.ZERO));
        com.xiaoniu.aftermarket.payment.dto.CashierSummary cashier =
                cashierStatusService.summarize(workOrder);
        response.setCashierStatus(cashier.getCashierStatus());
        response.setCashierStatusText(cashier.getCashierStatusText());
        response.setInventoryStatus(deriveInventoryStatus(workOrder.getStatus()));
        response.setInventoryStatusText(deriveInventoryStatusText(response.getInventoryStatus()));
        response.setCanSettle(false);
        response.setCanDeliver(WorkOrderStatus.REPAIR_DONE.getCode().equals(workOrder.getStatus())
                && ("PAID".equals(cashier.getCashierStatus()) || "NO_CHARGE".equals(cashier.getCashierStatus())));
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
        // 悲观锁：后续超收校验依赖此锁保证并发安全
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

    private String deriveInventoryStatus(String status) {
        if (WorkOrderStatus.REPAIRING.getCode().equals(status)) {
            return "RESERVED";
        }
        if (WorkOrderStatus.REPAIR_DONE.getCode().equals(status) || WorkOrderStatus.DELIVERED.getCode().equals(status)) {
            return "CONSUMED";
        }
        if (WorkOrderStatus.CANCELLED.getCode().equals(status)) {
            return "RELEASED";
        }
        return "NOT_RESERVED";
    }

    private String deriveInventoryStatusText(String status) {
        return switch (status) {
            case "RESERVED" -> "已预占";
            case "CONSUMED" -> "已扣减";
            case "RELEASED" -> "已释放";
            default -> "未预占";
        };
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
        response.setReceiverName(userDisplayName(entity.getReceiverId()));
        response.setOperatorId(entity.getOperatorId());
        response.setOperatorName(userDisplayName(entity.getOperatorId()));
        response.setRemark(entity.getRemark());
        return response;
    }

    private List<Long> findWorkOrderIds(Long storeId, String workOrderNo, String customerName) {
        String normalizedWorkOrderNo = normalize(workOrderNo);
        String normalizedCustomerName = normalize(customerName);
        if (normalizedWorkOrderNo == null && normalizedCustomerName == null) {
            return null;
        }
        QueryWrapper<WorkOrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId).eq("deleted", 0);
        if (normalizedWorkOrderNo != null) {
            wrapper.apply(containsCondition("work_order_no"), buildContainsPattern(normalizedWorkOrderNo));
        }
        if (normalizedCustomerName != null) {
            wrapper.apply(containsCondition("customer_name_snapshot"), buildContainsPattern(normalizedCustomerName));
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
        response.setReceiverName(userDisplayName(entity.getReceiverId()));
        response.setOperatorId(entity.getOperatorId());
        response.setOperatorName(userDisplayName(entity.getOperatorId()));
        response.setRemark(entity.getRemark());
        return response;
    }

    private String userDisplayName(Long userId) {
        if (userId == null) {
            return null;
        }
        SysUserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

}
