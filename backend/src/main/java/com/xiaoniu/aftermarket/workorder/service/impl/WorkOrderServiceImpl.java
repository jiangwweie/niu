package com.xiaoniu.aftermarket.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.ChargeType;
import com.xiaoniu.aftermarket.common.enums.CashierStatus;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.InventoryFlowType;
import com.xiaoniu.aftermarket.common.enums.InventoryStatus;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.official.entity.OfficialAfterSalesEntity;
import com.xiaoniu.aftermarket.official.mapper.OfficialAfterSalesMapper;
import com.xiaoniu.aftermarket.payment.dto.CashierSummary;
import com.xiaoniu.aftermarket.payment.service.CashierStatusService;
import com.xiaoniu.aftermarket.payment.service.impl.PaymentAmountService;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.part.service.PartService;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.AddNonInventoryChargeCommand;
import com.xiaoniu.aftermarket.workorder.dto.AdjustChargeItemsCommand;
import com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.DeliverWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.MarkRepairDoneWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SettleWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderDraftCommand;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderDetailResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryRequest;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemInput;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderChargeItemEntity;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderStatusLogEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderChargeItemMapper;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderStatusLogMapper;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderChargeItemMapper chargeItemMapper;
    private final WorkOrderStatusLogMapper statusLogMapper;
    private final SequenceService sequenceService;
    private final PartMapper partMapper;
    private final PartService partService;
    private final InventoryStockMapper inventoryStockMapper;
    private final InventoryFlowMapper inventoryFlowMapper;
    private final PaymentAmountService paymentAmountService;
    private final CashierStatusService cashierStatusService;
    private final OfficialAfterSalesMapper officialAfterSalesMapper;

    public WorkOrderServiceImpl(WorkOrderMapper workOrderMapper,
                                WorkOrderChargeItemMapper chargeItemMapper,
                                WorkOrderStatusLogMapper statusLogMapper,
                                SequenceService sequenceService,
	                                PartMapper partMapper,
                                    PartService partService,
	                                InventoryStockMapper inventoryStockMapper,
	                                InventoryFlowMapper inventoryFlowMapper,
	                                PaymentAmountService paymentAmountService,
                                    CashierStatusService cashierStatusService,
	                                OfficialAfterSalesMapper officialAfterSalesMapper) {
        this.workOrderMapper = workOrderMapper;
        this.chargeItemMapper = chargeItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.sequenceService = sequenceService;
        this.partMapper = partMapper;
        this.partService = partService;
        this.inventoryStockMapper = inventoryStockMapper;
        this.inventoryFlowMapper = inventoryFlowMapper;
        this.paymentAmountService = paymentAmountService;
        this.cashierStatusService = cashierStatusService;
        this.officialAfterSalesMapper = officialAfterSalesMapper;
    }

    @Override
    @Transactional
    public Long createDraft(CreateDraftWorkOrderCommand command) {
        // 草稿阶段仅创建工单和收费项，不预占库存，避免用户放弃草稿时产生无效库存变动
        WorkOrderEntity entity = new WorkOrderEntity();
        entity.setStoreId(command.getStoreId());
        entity.setWorkOrderNo(sequenceService.next("WORK_ORDER"));
        entity.setCustomerId(command.getCustomerId());
        entity.setVehicleId(command.getVehicleId());
        entity.setCustomerNameSnapshot(command.getCustomerNameSnapshot());
        entity.setCustomerPhoneSnapshot(command.getCustomerPhoneSnapshot());
        entity.setVehicleModelSnapshot(command.getVehicleModelSnapshot());
        entity.setFrameNoSnapshot(command.getFrameNoSnapshot());
        entity.setBatteryNoSnapshot(command.getBatteryNoSnapshot());
        entity.setRepairItem(command.getRepairItem());
        entity.setStatus(WorkOrderStatus.DRAFT.getCode());
        entity.setReceivableAmount(BigDecimal.ZERO);
        entity.setReceivedAmount(BigDecimal.ZERO);
        entity.setRemark(command.getRemark());
        entity.setCreatedBy(command.getOperatorId());
        workOrderMapper.insert(entity);

        if (command.getChargeItems() != null) {
            for (WorkOrderChargeItemInput input : command.getChargeItems()) {
                WorkOrderChargeItemEntity item = buildChargeItemEntity(
                        entity.getStoreId(), entity.getId(), input);
                chargeItemMapper.insert(item);
            }
            recalculateReceivableAmount(entity.getId());
        }

        writeStatusLog(entity.getStoreId(), entity.getId(), null,
                WorkOrderStatus.DRAFT.getCode(), "CREATE", command.getOperatorId());

        return entity.getId();
    }

    @Override
    @Transactional
    public WorkOrderEntity updateDraft(Long workOrderId, UpdateWorkOrderDraftCommand command) {
        // 只有草稿状态的工单允许编辑，已提交后必须走状态动作接口
        if (command.getStoreId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "storeId不能为空");
        }
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)
                || !command.getStoreId().equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.DRAFT.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_DRAFT);
        }

        if (command.getCustomerId() != null || command.getVehicleId() != null) {
            entity.setCustomerId(command.getCustomerId());
            entity.setVehicleId(command.getVehicleId());
        }
        if (StringUtils.hasText(command.getCustomerNameSnapshot())) {
            entity.setCustomerNameSnapshot(command.getCustomerNameSnapshot());
        }
        if (command.getCustomerPhoneSnapshot() != null) {
            entity.setCustomerPhoneSnapshot(command.getCustomerPhoneSnapshot());
        }
        if (command.getVehicleModelSnapshot() != null) {
            entity.setVehicleModelSnapshot(command.getVehicleModelSnapshot());
        }
        if (command.getFrameNoSnapshot() != null) {
            entity.setFrameNoSnapshot(command.getFrameNoSnapshot());
        }
        if (command.getBatteryNoSnapshot() != null) {
            entity.setBatteryNoSnapshot(command.getBatteryNoSnapshot());
        }
        if (StringUtils.hasText(command.getRepairItem())) {
            entity.setRepairItem(command.getRepairItem());
        }
        if (command.getRemark() != null) {
            entity.setRemark(command.getRemark());
        }
        entity.setUpdatedBy(command.getOperatorId());
        workOrderMapper.updateById(entity);
        return entity;
    }

    @Override
    @Transactional
    public void deleteDraft(Long storeId, Long workOrderId, Long operatorId) {
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)
                || !storeId.equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.DRAFT.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_DRAFT, "仅新建中的草稿工单允许删除");
        }
        entity.setDeleted(1);
        entity.setUpdatedBy(operatorId);
        entity.setUpdatedAt(LocalDateTime.now());
        workOrderMapper.updateById(entity);
    }

    @Override
    public WorkOrderDetailResponse getById(Long workOrderId) {
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        if (entity == null || (entity.getDeleted() != null && entity.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }

        List<WorkOrderChargeItemEntity> items = chargeItemMapper.selectByWorkOrderId(workOrderId);

        WorkOrderDetailResponse response = new WorkOrderDetailResponse();
        response.setId(entity.getId());
        response.setStoreId(entity.getStoreId());
        response.setWorkOrderNo(entity.getWorkOrderNo());
        response.setCustomerId(entity.getCustomerId());
        response.setVehicleId(entity.getVehicleId());
        response.setCustomerNameSnapshot(entity.getCustomerNameSnapshot());
        response.setCustomerPhoneSnapshot(entity.getCustomerPhoneSnapshot());
        response.setVehicleModelSnapshot(entity.getVehicleModelSnapshot());
        response.setFrameNoSnapshot(entity.getFrameNoSnapshot());
        response.setBatteryNoSnapshot(entity.getBatteryNoSnapshot());
        response.setRepairItem(entity.getRepairItem());
        response.setStatus(entity.getStatus());
        response.setReceivableAmount(entity.getReceivableAmount());
        response.setReceivedAmount(entity.getReceivedAmount());
        enrichStatusFields(response, entity);
        response.setNoChargeReason(entity.getNoChargeReason());
        response.setNoChargeRemark(entity.getNoChargeRemark());
        response.setRemark(entity.getRemark());
        response.setCreatedAt(entity.getCreatedAt());
        response.setChargeItems(items.stream().map(this::toChargeItemResponse).toList());
        return response;
    }

    @Override
    public WorkOrderEntity getByWorkOrderNo(String workOrderNo) {
        QueryWrapper<WorkOrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("work_order_no", workOrderNo).eq("deleted", 0);
        return workOrderMapper.selectOne(wrapper);
    }

    @Override
    public PageResponse<WorkOrderQueryResponse> pageQuery(WorkOrderQueryRequest request) {
        int pn = request.getPageNo() == null ? 1 : request.getPageNo();
        int ps = request.getPageSize() == null ? 20 : request.getPageSize();

        Map<Long, OfficialAfterSalesEntity> officialFilterMap = Map.of();
        if (Boolean.TRUE.equals(request.getOfficialOnly())) {
            officialFilterMap = loadOfficialAfterSalesByStore(request.getStoreId());
            if (officialFilterMap.isEmpty()) {
                return new PageResponse<>(List.of(), pn, ps, 0);
            }
        }

        QueryWrapper<WorkOrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId()).eq("deleted", 0);

        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().trim();
            List<Long> officialWorkOrderIds = findOfficialWorkOrderIdsByKeyword(request.getStoreId(), keyword);
            wrapper.and(group -> group
                    .like("work_order_no", keyword)
                    .or().like("customer_name_snapshot", keyword)
                    .or().like("customer_phone_snapshot", keyword)
                    .or().like("frame_no_snapshot", keyword)
                    .or().like("vehicle_model_snapshot", keyword)
                    .or(sub -> {
                        if (officialWorkOrderIds.isEmpty()) {
                            sub.apply("1 = 0");
                        } else {
                            sub.in("id", officialWorkOrderIds);
                        }
                    }));
        }

        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq("status", request.getStatus());
        }
        if (StringUtils.hasText(request.getWorkOrderNo())) {
            wrapper.eq("work_order_no", request.getWorkOrderNo());
        }
        if (StringUtils.hasText(request.getCustomerName())) {
            wrapper.like("customer_name_snapshot", request.getCustomerName());
        }
        if (StringUtils.hasText(request.getCustomerPhone())) {
            wrapper.like("customer_phone_snapshot", request.getCustomerPhone().trim());
        }
        if (StringUtils.hasText(request.getVehicleFrameNo())) {
            wrapper.like("frame_no_snapshot", request.getVehicleFrameNo().trim());
        }
        if (request.getStartTime() != null) {
            wrapper.ge("created_at", request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le("created_at", request.getEndTime());
        }
        if (Boolean.TRUE.equals(request.getOfficialOnly())) {
            wrapper.in("id", officialFilterMap.keySet());
        }

        long total = workOrderMapper.selectCount(wrapper);

        List<WorkOrderQueryResponse> records;
        if (total == 0) {
            records = List.of();
        } else {
            wrapper.last("ORDER BY id DESC LIMIT " + ps + " OFFSET " + (long) (pn - 1) * ps);
            List<WorkOrderEntity> entities = workOrderMapper.selectList(wrapper);
            Map<Long, OfficialAfterSalesEntity> officialMap = Boolean.TRUE.equals(request.getOfficialOnly())
                    ? officialFilterMap
                    : loadOfficialAfterSalesByWorkOrderIds(entities.stream().map(WorkOrderEntity::getId).toList());
            records = entities.stream()
                    .map(entity -> toQueryResponse(entity, officialMap.get(entity.getId())))
                    .toList();
        }

        return new PageResponse<>(records, pn, ps, total);
    }

    @Override
    @Transactional
    public Long addChargeItem(Long workOrderId, AddWorkOrderChargeItemCommand command) {
        WorkOrderEntity workOrder = loadAndValidateDraft(workOrderId, command.getStoreId());

        WorkOrderChargeItemEntity entity = new WorkOrderChargeItemEntity();
        entity.setStoreId(workOrder.getStoreId());
        entity.setWorkOrderId(workOrderId);
        entity.setChargeType(command.getChargeType());
        entity.setItemName(command.getItemName());
        entity.setQuantity(command.getQuantity());
        entity.setUnit(command.getUnit());
        entity.setUnitPrice(command.getUnitPrice());
        entity.setRemark(command.getRemark());

        resolveChargeItemPart(command, workOrder.getStoreId());
        validateChargeItemFields(command, workOrder.getStoreId());
        populateChargeItemByType(entity, command);

        BigDecimal lineAmount = new BigDecimal(command.getQuantity())
                .multiply(command.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        entity.setLineAmount(lineAmount);
        entity.setStatus("ACTIVE");
        entity.setTempPart(Boolean.TRUE.equals(command.getTempPart()));

        chargeItemMapper.insert(entity);
        recalculateReceivableAmount(workOrderId);
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateChargeItem(Long workOrderId, Long chargeItemId,
                                 UpdateWorkOrderChargeItemCommand command) {
        // 收费项编辑仅限草稿状态，已提交工单的收费项不可直接修改
        loadAndValidateDraft(workOrderId, command.getStoreId());

        WorkOrderChargeItemEntity entity = chargeItemMapper.selectById(chargeItemId);
        if (entity == null || !workOrderId.equals(entity.getWorkOrderId())
                || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.CHARGE_ITEM_NOT_FOUND);
        }

        if (command.getQuantity() == null || command.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.CHARGE_QUANTITY_INVALID);
        }
        if (command.getUnitPrice() != null && command.getUnitPrice().signum() < 0) {
            throw new BusinessException(ErrorCode.CHARGE_PRICE_INVALID);
        }

        if (StringUtils.hasText(command.getItemName())) {
            entity.setItemName(command.getItemName());
        }
        entity.setQuantity(command.getQuantity());
        if (command.getUnit() != null) {
            entity.setUnit(command.getUnit());
        }
        if (command.getUnitPrice() != null) {
            entity.setUnitPrice(command.getUnitPrice());
        }
        if (command.getRemark() != null) {
            entity.setRemark(command.getRemark());
        }

        BigDecimal lineAmount = new BigDecimal(command.getQuantity())
                .multiply(entity.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        entity.setLineAmount(lineAmount);

        if (ChargeType.PART.getCode().equals(entity.getChargeType())
                && entity.getCostPriceSnapshot() != null) {
            BigDecimal lineCost = new BigDecimal(command.getQuantity())
                    .multiply(entity.getCostPriceSnapshot())
                    .setScale(2, RoundingMode.HALF_UP);
            entity.setLineCostAmount(lineCost);
        }

        chargeItemMapper.updateById(entity);
        recalculateReceivableAmount(workOrderId);
    }

    @Override
    @Transactional
    public void removeChargeItem(Long storeId, Long workOrderId, Long chargeItemId) {
        // 删除收费项仅限草稿状态，防止已预占库存的收费项被误删
        loadAndValidateDraft(workOrderId, storeId);

        WorkOrderChargeItemEntity entity = chargeItemMapper.selectById(chargeItemId);
        if (entity == null || !workOrderId.equals(entity.getWorkOrderId())
                || entity.getDeleted() != null && entity.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.CHARGE_ITEM_NOT_FOUND);
        }

        entity.setDeleted(1);
        chargeItemMapper.updateById(entity);
        recalculateReceivableAmount(workOrderId);
    }

    private void recalculateReceivableAmount(Long workOrderId) {
        List<WorkOrderChargeItemEntity> items = chargeItemMapper.selectByWorkOrderId(workOrderId);
        BigDecimal total = calculateReceivableAmount(items);

        UpdateWrapper<WorkOrderEntity> uw = new UpdateWrapper<>();
        uw.eq("id", workOrderId).set("receivable_amount", total);
        workOrderMapper.update(null, uw);
    }

    @Override
    @Transactional
    public void submit(SubmitWorkOrderCommand command) {
        if (command.getStoreId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "storeId不能为空");
        }
        if (command.getWorkOrderId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "workOrderId不能为空");
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "operatorId不能为空");
        }

        WorkOrderEntity workOrder = workOrderMapper.selectByIdForUpdate(command.getWorkOrderId());
        if (workOrder == null || workOrder.getStoreId() == null
                || !command.getStoreId().equals(workOrder.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.DRAFT.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_DRAFT);
        }

        List<WorkOrderChargeItemEntity> items = chargeItemMapper.selectByWorkOrderId(workOrder.getId());
        BigDecimal receivableAmount = calculateReceivableAmount(items);
        Map<Long, Integer> reserveQuantities = summarizeReserveQuantities(items, workOrder.getStoreId());

        // 提交工单时才预占库存：扣减可用库存，增加预占库存
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, Integer> entry : reserveQuantities.entrySet()) {
            reservePartStock(workOrder, entry.getKey(), entry.getValue(), command, now);
        }

        workOrder.setStatus(WorkOrderStatus.REPAIRING.getCode());
        workOrder.setReceivableAmount(receivableAmount);
        workOrder.setSubmittedBy(command.getOperatorId());
        workOrder.setSubmittedAt(now);
        workOrder.setUpdatedBy(command.getOperatorId());
        workOrderMapper.updateById(workOrder);

        writeStatusLog(workOrder.getStoreId(), workOrder.getId(),
                WorkOrderStatus.DRAFT.getCode(), WorkOrderStatus.REPAIRING.getCode(),
                "SUBMIT", command.getOperatorId(), now, "提交工单", command.getRemark());
    }

    @Override
    @Transactional
    public void cancel(CancelWorkOrderCommand command) {
        if (command.getStoreId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "storeId不能为空");
        }
        if (command.getWorkOrderId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "workOrderId不能为空");
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.OPERATOR_REQUIRED);
        }
        if (!StringUtils.hasText(command.getReason())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_CANCEL_REASON_REQUIRED);
        }

        WorkOrderEntity workOrder = workOrderMapper.selectByIdForUpdate(command.getWorkOrderId());
        if (workOrder == null || workOrder.getStoreId() == null
                || !command.getStoreId().equals(workOrder.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }

        String fromStatus = workOrder.getStatus();
        boolean draftCancel = WorkOrderStatus.DRAFT.getCode().equals(fromStatus);
        boolean submittedCancel = WorkOrderStatus.REPAIRING.getCode().equals(fromStatus);
        if (!draftCancel && !submittedCancel) {
            throw new BusinessException(ErrorCode.WORK_ORDER_CANCEL_NOT_ALLOWED);
        }

        LocalDateTime now = LocalDateTime.now();
        if (submittedCancel) {
            // 取消已提交的工单必须释放预占库存，否则库存会被永久占用
            List<WorkOrderChargeItemEntity> items = chargeItemMapper.selectByWorkOrderId(workOrder.getId());
            Map<Long, Integer> releaseQuantities = summarizeInventoryAffectingQuantities(items);
            for (Map.Entry<Long, Integer> entry : releaseQuantities.entrySet()) {
                releasePartStock(workOrder, entry.getKey(), entry.getValue(), command, now);
            }
        }

        workOrder.setStatus(WorkOrderStatus.CANCELLED.getCode());
        workOrder.setCancelledBy(command.getOperatorId());
        workOrder.setCancelledAt(now);
        workOrder.setCancelReason(command.getReason());
        workOrder.setUpdatedBy(command.getOperatorId());
        workOrderMapper.updateById(workOrder);

        writeStatusLog(workOrder.getStoreId(), workOrder.getId(), fromStatus,
                WorkOrderStatus.CANCELLED.getCode(), "CANCEL", command.getOperatorId(),
                now, command.getReason(), command.getReason());
    }

    @Override
    @Transactional
    public void settle(SettleWorkOrderCommand command) {
        throw new BusinessException(ErrorCode.WORK_ORDER_LEGACY_SETTLE_DISABLED);
    }

    @Override
    @Transactional
    public void markRepairDone(MarkRepairDoneWorkOrderCommand command) {
        validateActionCommand(command.getStoreId(), command.getWorkOrderId(), command.getOperatorId());
        WorkOrderEntity workOrder = loadForAction(command.getStoreId(), command.getWorkOrderId());
        if (!WorkOrderStatus.REPAIRING.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_REPAIR_DONE_NOT_ALLOWED);
        }

        List<WorkOrderChargeItemEntity> items = chargeItemMapper.selectByWorkOrderId(workOrder.getId());
        BigDecimal receivableAmount = calculateReceivableAmount(items);
        if (receivableAmount.compareTo(BigDecimal.ZERO) == 0) {
            applyNoChargeReason(workOrder, command.getNoChargeReason(), command.getNoChargeRemark());
        }

        Map<Long, Integer> consumeQuantities = summarizeInventoryAffectingQuantities(items);
        LocalDateTime now = LocalDateTime.now();
        for (Map.Entry<Long, Integer> entry : consumeQuantities.entrySet()) {
            consumePartStock(workOrder, entry.getKey(), entry.getValue(), command.getOperatorId(),
                    "标记维修完成扣减库存", command.getRemark(), now);
        }

        workOrder.setStatus(WorkOrderStatus.REPAIR_DONE.getCode());
        workOrder.setReceivableAmount(receivableAmount);
        workOrder.setReceivedAmount(paymentAmountService.calculateReceivedAmount(workOrder.getId()));
        workOrder.setRepairDoneBy(command.getOperatorId());
        workOrder.setRepairDoneAt(now);
        workOrder.setUpdatedBy(command.getOperatorId());
        workOrderMapper.updateById(workOrder);

        writeStatusLog(workOrder.getStoreId(), workOrder.getId(), WorkOrderStatus.REPAIRING.getCode(),
                WorkOrderStatus.REPAIR_DONE.getCode(), "MARK_REPAIR_DONE", command.getOperatorId(),
                now, "标记维修完成", command.getRemark());
    }

    @Override
    @Transactional
    public void deliver(DeliverWorkOrderCommand command) {
        validateActionCommand(command.getStoreId(), command.getWorkOrderId(), command.getOperatorId());
        WorkOrderEntity workOrder = loadForAction(command.getStoreId(), command.getWorkOrderId());
        if (!WorkOrderStatus.REPAIR_DONE.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_DELIVER_NOT_ALLOWED);
        }
        if (paymentAmountService.normalizeAmount(workOrder.getReceivableAmount()).compareTo(BigDecimal.ZERO) == 0) {
            applyNoChargeReason(workOrder, command.getNoChargeReason(), command.getNoChargeRemark());
        }
        CashierSummary summary = cashierStatusService.summarize(workOrder);
        boolean payable = CashierStatus.PAID.getCode().equals(summary.getCashierStatus())
                || CashierStatus.NO_CHARGE.getCode().equals(summary.getCashierStatus());
        if (!payable) {
            throw new BusinessException(ErrorCode.WORK_ORDER_RECEIVED_AMOUNT_NOT_ENOUGH);
        }

        LocalDateTime now = LocalDateTime.now();
        workOrder.setStatus(WorkOrderStatus.DELIVERED.getCode());
        workOrder.setDeliveredBy(command.getOperatorId());
        workOrder.setDeliveredAt(now);
        workOrder.setUpdatedBy(command.getOperatorId());
        workOrderMapper.updateById(workOrder);

        writeStatusLog(workOrder.getStoreId(), workOrder.getId(), WorkOrderStatus.REPAIR_DONE.getCode(),
                WorkOrderStatus.DELIVERED.getCode(), "DELIVER", command.getOperatorId(),
                now, "交付关闭", command.getRemark());
    }

    @Override
    @Transactional
    public Long addNonInventoryCharge(AddNonInventoryChargeCommand command) {
        validateActionCommand(command.getStoreId(), command.getWorkOrderId(), command.getOperatorId());
        if (!StringUtils.hasText(command.getReason())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NON_INVENTORY_CHARGE_REASON_REQUIRED);
        }
        WorkOrderEntity workOrder = loadForAction(command.getStoreId(), command.getWorkOrderId());
        if (!WorkOrderStatus.REPAIR_DONE.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NON_INVENTORY_CHARGE_NOT_ALLOWED);
        }
        if (!ChargeType.LABOR.getCode().equals(command.getChargeType())
                && !ChargeType.OTHER.getCode().equals(command.getChargeType())) {
            throw new BusinessException(ErrorCode.CHARGE_TYPE_INVALID);
        }

        AddWorkOrderChargeItemCommand addCommand = new AddWorkOrderChargeItemCommand();
        addCommand.setStoreId(command.getStoreId());
        addCommand.setChargeType(command.getChargeType());
        addCommand.setItemName(command.getItemName());
        addCommand.setQuantity(command.getQuantity());
        addCommand.setUnit(command.getUnit());
        addCommand.setUnitPrice(command.getUnitPrice());
        addCommand.setRemark(command.getReason() + (StringUtils.hasText(command.getRemark())
                ? "；" + command.getRemark() : ""));
        resolveChargeItemPart(addCommand, workOrder.getStoreId());
        validateChargeItemFields(addCommand, workOrder.getStoreId());

        WorkOrderChargeItemEntity entity = new WorkOrderChargeItemEntity();
        entity.setStoreId(workOrder.getStoreId());
        entity.setWorkOrderId(workOrder.getId());
        entity.setChargeType(command.getChargeType());
        entity.setItemName(command.getItemName());
        entity.setQuantity(command.getQuantity());
        entity.setUnit(command.getUnit());
        entity.setUnitPrice(command.getUnitPrice());
        entity.setRemark(addCommand.getRemark());
        populateChargeItemByType(entity, addCommand);
        BigDecimal lineAmount = new BigDecimal(command.getQuantity()).multiply(command.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        entity.setLineAmount(lineAmount);
        entity.setStatus("ACTIVE");
        entity.setTempPart(Boolean.TRUE.equals(addCommand.getTempPart()));
        chargeItemMapper.insert(entity);
        recalculateReceivableAmount(workOrder.getId());
        paymentAmountService.updateReceivedAmount(workOrder.getId());
        return entity.getId();
    }

    @Override
    public void adjustChargeItems(AdjustChargeItemsCommand command) {
        throw new UnsupportedOperationException("TODO: implement charge item adjustment");
    }

    @Override
    public void moveToAccepted(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement accepted transition");
    }

    @Override
    public void markPartOrdered(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement part-ordered transition");
    }

    @Override
    public void markPartArrived(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement part-arrived transition");
    }

    // --- private helpers ---

    private BigDecimal calculateReceivableAmount(List<WorkOrderChargeItemEntity> items) {
        return items.stream()
                .filter(item -> item.getDeleted() == null || item.getDeleted() == 0)
                .map(WorkOrderChargeItemEntity::getLineAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<Long, Integer> summarizeReserveQuantities(List<WorkOrderChargeItemEntity> items,
                                                           Long storeId) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        items.stream()
                .filter(item -> ChargeType.PART.getCode().equals(item.getChargeType()))
                .filter(item -> Boolean.TRUE.equals(item.getInventoryAffecting()))
                .filter(item -> item.getDeleted() == null || item.getDeleted() == 0)
                .sorted(Comparator.comparing(WorkOrderChargeItemEntity::getPartId,
                        Comparator.nullsLast(Long::compareTo)))
                .forEach(item -> {
                    if (item.getPartId() == null) {
                        throw new BusinessException(ErrorCode.PART_REQUIRED_FOR_PART_CHARGE);
                    }
                    if (item.getQuantity() == null || item.getQuantity() <= 0) {
                        throw new BusinessException(ErrorCode.CHARGE_QUANTITY_INVALID);
                    }
                    // TODO: Batch-load parts when submit volume grows beyond the single-store MVP.
                    PartEntity part = partMapper.selectById(item.getPartId());
                    if (part == null || (part.getDeleted() != null && part.getDeleted() == 1)
                            || !storeId.equals(part.getStoreId())) {
                        throw new BusinessException(ErrorCode.PART_NOT_FOUND);
                    }
                    if (!CommonStatus.ENABLED.getCode().equals(part.getStatus())) {
                        throw new BusinessException(ErrorCode.PART_DISABLED);
                    }
                    quantities.merge(item.getPartId(), item.getQuantity(), Integer::sum);
                });
        return quantities;
    }

    private Map<Long, Integer> summarizeInventoryAffectingQuantities(List<WorkOrderChargeItemEntity> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        items.stream()
                .filter(item -> ChargeType.PART.getCode().equals(item.getChargeType()))
                .filter(item -> Boolean.TRUE.equals(item.getInventoryAffecting()))
                .filter(item -> item.getDeleted() == null || item.getDeleted() == 0)
                .sorted(Comparator.comparing(WorkOrderChargeItemEntity::getPartId,
                        Comparator.nullsLast(Long::compareTo)))
                .forEach(item -> {
                    if (item.getPartId() == null) {
                        throw new BusinessException(ErrorCode.PART_REQUIRED_FOR_PART_CHARGE);
                    }
                    if (item.getQuantity() == null || item.getQuantity() <= 0) {
                        throw new BusinessException(ErrorCode.CHARGE_QUANTITY_INVALID);
                    }
                    quantities.merge(item.getPartId(), item.getQuantity(), Integer::sum);
                });
        return quantities;
    }

    private void reservePartStock(WorkOrderEntity workOrder, Long partId, Integer quantity,
                                  SubmitWorkOrderCommand command,
                                  LocalDateTime operatedAt) {
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartIdForUpdate(
                workOrder.getStoreId(), partId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.PART_STOCK_NOT_FOUND);
        }

        int beforeActual = stock.getActualQty();
        int beforeAvailable = stock.getAvailableQty();
        int beforeReserved = stock.getReservedQty();
        if (beforeAvailable < quantity) {
            throw new BusinessException(ErrorCode.INVENTORY_AVAILABLE_NOT_ENOUGH);
        }

        int afterAvailable = beforeAvailable - quantity;
        int afterReserved = beforeReserved + quantity;

        stock.setAvailableQty(afterAvailable);
        stock.setReservedQty(afterReserved);
        stock.setUpdatedBy(command.getOperatorId());
        inventoryStockMapper.updateById(stock);

        InventoryFlowEntity flow = new InventoryFlowEntity();
        flow.setStoreId(workOrder.getStoreId());
        flow.setInventoryStockId(stock.getId());
        flow.setPartId(partId);
        flow.setFlowType(InventoryFlowType.RESERVE.getCode());
        flow.setQuantityDelta(quantity);
        flow.setActualBefore(beforeActual);
        flow.setActualAfter(beforeActual);
        flow.setAvailableBefore(beforeAvailable);
        flow.setAvailableAfter(afterAvailable);
        flow.setReservedBefore(beforeReserved);
        flow.setReservedAfter(afterReserved);
        flow.setBusinessType("WORK_ORDER");
        flow.setBusinessId(workOrder.getId());
        flow.setWorkOrderId(workOrder.getId());
        flow.setOperatorId(command.getOperatorId());
        flow.setOperatedAt(operatedAt);
        flow.setReason("提交工单预占库存");
        flow.setRemark(command.getRemark());
        flow.setCreatedBy(command.getOperatorId());
        inventoryFlowMapper.insert(flow);

        stock.setLastFlowId(flow.getId());
        stock.setLastChangedAt(operatedAt);
        inventoryStockMapper.updateById(stock);
    }

    private void consumePartStock(WorkOrderEntity workOrder, Long partId, Integer quantity,
                                  SettleWorkOrderCommand command,
                                  LocalDateTime operatedAt) {
        consumePartStock(workOrder, partId, quantity, command.getOperatorId(),
                "工单结算扣减库存", command.getRemark(), operatedAt);
    }

    private void consumePartStock(WorkOrderEntity workOrder, Long partId, Integer quantity,
                                  Long operatorId, String reason, String remark,
                                  LocalDateTime operatedAt) {
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartIdForUpdate(
                workOrder.getStoreId(), partId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.PART_STOCK_NOT_FOUND);
        }

        int beforeActual = stock.getActualQty();
        int beforeAvailable = stock.getAvailableQty();
        int beforeReserved = stock.getReservedQty();
        if (beforeReserved < quantity) {
            throw new BusinessException(ErrorCode.INVENTORY_RESERVED_NOT_ENOUGH);
        }
        if (beforeActual < quantity) {
            throw new BusinessException(ErrorCode.INVENTORY_AVAILABLE_NOT_ENOUGH);
        }

        int afterActual = beforeActual - quantity;
        int afterReserved = beforeReserved - quantity;

        stock.setActualQty(afterActual);
        stock.setReservedQty(afterReserved);
        stock.setUpdatedBy(operatorId);
        inventoryStockMapper.updateById(stock);

        InventoryFlowEntity flow = new InventoryFlowEntity();
        flow.setStoreId(workOrder.getStoreId());
        flow.setInventoryStockId(stock.getId());
        flow.setPartId(partId);
        flow.setFlowType(InventoryFlowType.CONSUME.getCode());
        flow.setQuantityDelta(quantity);
        flow.setActualBefore(beforeActual);
        flow.setActualAfter(afterActual);
        flow.setAvailableBefore(beforeAvailable);
        flow.setAvailableAfter(beforeAvailable);
        flow.setReservedBefore(beforeReserved);
        flow.setReservedAfter(afterReserved);
        flow.setBusinessType("WORK_ORDER");
        flow.setBusinessId(workOrder.getId());
        flow.setWorkOrderId(workOrder.getId());
        flow.setOperatorId(operatorId);
        flow.setOperatedAt(operatedAt);
        flow.setReason(reason);
        flow.setRemark(remark);
        flow.setCreatedBy(operatorId);
        inventoryFlowMapper.insert(flow);

        stock.setLastFlowId(flow.getId());
        stock.setLastChangedAt(operatedAt);
        inventoryStockMapper.updateById(stock);
    }

    private void releasePartStock(WorkOrderEntity workOrder, Long partId, Integer quantity,
                                  CancelWorkOrderCommand command,
                                  LocalDateTime operatedAt) {
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartIdForUpdate(
                workOrder.getStoreId(), partId);
        if (stock == null) {
            throw new BusinessException(ErrorCode.PART_STOCK_NOT_FOUND);
        }

        int beforeActual = stock.getActualQty();
        int beforeAvailable = stock.getAvailableQty();
        int beforeReserved = stock.getReservedQty();
        if (beforeReserved < quantity) {
            throw new BusinessException(ErrorCode.INVENTORY_RESERVED_NOT_ENOUGH);
        }

        int afterAvailable = beforeAvailable + quantity;
        int afterReserved = beforeReserved - quantity;

        stock.setAvailableQty(afterAvailable);
        stock.setReservedQty(afterReserved);
        stock.setUpdatedBy(command.getOperatorId());
        inventoryStockMapper.updateById(stock);

        InventoryFlowEntity flow = new InventoryFlowEntity();
        flow.setStoreId(workOrder.getStoreId());
        flow.setInventoryStockId(stock.getId());
        flow.setPartId(partId);
        flow.setFlowType(InventoryFlowType.RELEASE.getCode());
        flow.setQuantityDelta(quantity);
        flow.setActualBefore(beforeActual);
        flow.setActualAfter(beforeActual);
        flow.setAvailableBefore(beforeAvailable);
        flow.setAvailableAfter(afterAvailable);
        flow.setReservedBefore(beforeReserved);
        flow.setReservedAfter(afterReserved);
        flow.setBusinessType("WORK_ORDER");
        flow.setBusinessId(workOrder.getId());
        flow.setWorkOrderId(workOrder.getId());
        flow.setOperatorId(command.getOperatorId());
        flow.setOperatedAt(operatedAt);
        flow.setReason("工单取消释放库存");
        flow.setRemark(command.getReason());
        flow.setCreatedBy(command.getOperatorId());
        inventoryFlowMapper.insert(flow);

        stock.setLastFlowId(flow.getId());
        stock.setLastChangedAt(operatedAt);
        inventoryStockMapper.updateById(stock);
    }

    private WorkOrderEntity loadAndValidateDraft(Long workOrderId, Long storeId) {
        if (storeId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "storeId不能为空");
        }
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        if (entity == null || !storeId.equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.DRAFT.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_DRAFT);
        }
        return entity;
    }

    private void validateActionCommand(Long storeId, Long workOrderId, Long operatorId) {
        if (storeId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "storeId不能为空");
        }
        if (workOrderId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "workOrderId不能为空");
        }
        if (operatorId == null) {
            throw new BusinessException(ErrorCode.OPERATOR_REQUIRED);
        }
    }

    private WorkOrderEntity loadForAction(Long storeId, Long workOrderId) {
        WorkOrderEntity entity = workOrderMapper.selectByIdForUpdate(workOrderId);
        if (entity == null || entity.getStoreId() == null || !storeId.equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        return entity;
    }

    private void applyNoChargeReason(WorkOrderEntity workOrder, String reason, String remark) {
        if (StringUtils.hasText(reason)) {
            workOrder.setNoChargeReason(reason);
            workOrder.setNoChargeRemark(remark);
        }
        if (!StringUtils.hasText(workOrder.getNoChargeReason())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NO_CHARGE_REASON_REQUIRED);
        }
    }

    private void validateChargeItemFields(AddWorkOrderChargeItemCommand command,
                                           Long storeId) {
        if (!StringUtils.hasText(command.getChargeType())) {
            throw new BusinessException(ErrorCode.CHARGE_TYPE_INVALID);
        }

        String chargeType = command.getChargeType();
        boolean validType = ChargeType.PART.getCode().equals(chargeType)
                || ChargeType.LABOR.getCode().equals(chargeType)
                || ChargeType.OTHER.getCode().equals(chargeType);
        if (!validType) {
            throw new BusinessException(ErrorCode.CHARGE_TYPE_INVALID);
        }

        if (command.getQuantity() == null || command.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.CHARGE_QUANTITY_INVALID);
        }
        if (command.getUnitPrice() == null || command.getUnitPrice().signum() < 0) {
            throw new BusinessException(ErrorCode.CHARGE_PRICE_INVALID);
        }

        if (ChargeType.PART.getCode().equals(chargeType)) {
            if (command.getPartId() == null) {
                throw new BusinessException(ErrorCode.PART_REQUIRED_FOR_PART_CHARGE);
            }
            PartEntity part = partMapper.selectById(command.getPartId());
            if (part == null || (part.getDeleted() != null && part.getDeleted() == 1)) {
                throw new BusinessException(ErrorCode.PART_NOT_FOUND);
            }
            if (!storeId.equals(part.getStoreId())) {
                throw new BusinessException(ErrorCode.PART_NOT_FOUND, "配件不属于当前门店");
            }
            if (!CommonStatus.ENABLED.getCode().equals(part.getStatus())) {
                throw new BusinessException(ErrorCode.PART_DISABLED);
            }
        } else {
            if (command.getPartId() != null) {
                throw new BusinessException(ErrorCode.PART_FORBIDDEN_FOR_NON_PART_CHARGE);
            }
        }
    }

    private void resolveChargeItemPart(AddWorkOrderChargeItemCommand command, Long storeId) {
        if (!ChargeType.PART.getCode().equals(command.getChargeType()) || command.getPartId() != null) {
            return;
        }
        String code = firstText(command.getBarcode(), command.getCode());
        if (!StringUtils.hasText(code)) {
            return;
        }
        PartEntity part = partService.lookupEnabledPartByCode(storeId, code);
        if (part == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND, "未找到对应配件");
        }
        command.setPartId(part.getId());
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : null;
    }

    private void populateChargeItemByType(WorkOrderChargeItemEntity entity,
                                           AddWorkOrderChargeItemCommand command) {
        String chargeType = command.getChargeType();

        if (ChargeType.PART.getCode().equals(chargeType)) {
            PartEntity part = partMapper.selectById(command.getPartId());
            entity.setPartId(command.getPartId());
            entity.setPartCodeSnapshot(part.getPartCode());
            entity.setPartNameSnapshot(part.getPartName());
            entity.setPartSourceSnapshot(part.getSource());
            entity.setInventoryAffecting(true);

            BigDecimal costPrice = part.getReferenceCostPrice();
            entity.setCostPriceSnapshot(costPrice != null ? costPrice : BigDecimal.ZERO);
            entity.setLineCostAmount(new BigDecimal(command.getQuantity())
                    .multiply(costPrice != null ? costPrice : BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP));
        } else {
            entity.setInventoryAffecting(false);
            entity.setCostPriceSnapshot(BigDecimal.ZERO);
            entity.setLineCostAmount(BigDecimal.ZERO);
        }
    }

    private WorkOrderChargeItemEntity buildChargeItemEntity(Long storeId, Long workOrderId,
                                                             WorkOrderChargeItemInput input) {
        AddWorkOrderChargeItemCommand command = new AddWorkOrderChargeItemCommand();
        command.setChargeType(input.getChargeType());
        command.setItemName(input.getItemName());
        command.setPartId(input.getPartId());
        command.setQuantity(input.getQuantity());
        command.setUnit(input.getUnit());
        command.setUnitPrice(input.getUnitPrice());
        command.setRemark(input.getRemark());

        validateChargeItemFields(command, storeId);

        WorkOrderChargeItemEntity entity = new WorkOrderChargeItemEntity();
        entity.setStoreId(storeId);
        entity.setWorkOrderId(workOrderId);
        entity.setChargeType(input.getChargeType());
        entity.setItemName(input.getItemName());
        entity.setQuantity(input.getQuantity());
        entity.setUnit(input.getUnit());
        entity.setUnitPrice(input.getUnitPrice());
        entity.setRemark(input.getRemark());

        populateChargeItemByType(entity, command);

        BigDecimal lineAmount = new BigDecimal(input.getQuantity())
                .multiply(input.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        entity.setLineAmount(lineAmount);
        entity.setStatus("ACTIVE");
        entity.setTempPart(false);

        return entity;
    }

    private void writeStatusLog(Long storeId, Long workOrderId, String fromStatus,
                                String toStatus, String actionType, Long operatorId) {
        writeStatusLog(storeId, workOrderId, fromStatus, toStatus, actionType, operatorId,
                LocalDateTime.now(), null, null);
    }

    private void writeStatusLog(Long storeId, Long workOrderId, String fromStatus,
                                String toStatus, String actionType, Long operatorId,
                                LocalDateTime operatedAt, String reason, String remark) {
        WorkOrderStatusLogEntity log = new WorkOrderStatusLogEntity();
        log.setStoreId(storeId);
        log.setWorkOrderId(workOrderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActionType(actionType);
        log.setOperatorId(operatorId);
        log.setOperatedAt(operatedAt);
        log.setReason(reason);
        log.setRemark(remark);
        log.setCreatedBy(operatorId);
        statusLogMapper.insert(log);
    }

    private WorkOrderChargeItemResponse toChargeItemResponse(WorkOrderChargeItemEntity entity) {
        WorkOrderChargeItemResponse response = new WorkOrderChargeItemResponse();
        response.setId(entity.getId());
        response.setWorkOrderId(entity.getWorkOrderId());
        response.setChargeType(entity.getChargeType());
        response.setItemName(entity.getItemName());
        response.setPartId(entity.getPartId());
        response.setPartCodeSnapshot(entity.getPartCodeSnapshot());
        response.setPartNameSnapshot(entity.getPartNameSnapshot());
        response.setPartSourceSnapshot(entity.getPartSourceSnapshot());
        response.setQuantity(entity.getQuantity());
        response.setUnit(entity.getUnit());
        response.setUnitPrice(entity.getUnitPrice());
        response.setLineAmount(entity.getLineAmount());
        response.setCostPriceSnapshot(entity.getCostPriceSnapshot());
        response.setLineCostAmount(entity.getLineCostAmount());
        response.setInventoryAffecting(entity.getInventoryAffecting());
        response.setTempPart(entity.getTempPart());
        response.setStatus(entity.getStatus());
        response.setRemark(entity.getRemark());
        return response;
    }

    private Map<Long, OfficialAfterSalesEntity> loadOfficialAfterSalesByStore(Long storeId) {
        QueryWrapper<OfficialAfterSalesEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
                .eq("deleted", 0)
                .eq("is_official_after_sales", true);
        return officialAfterSalesMapper.selectList(wrapper).stream()
                .collect(java.util.stream.Collectors.toMap(
                        OfficialAfterSalesEntity::getWorkOrderId,
                        java.util.function.Function.identity(),
                        (left, right) -> left));
    }

    private Map<Long, OfficialAfterSalesEntity> loadOfficialAfterSalesByWorkOrderIds(List<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<OfficialAfterSalesEntity> wrapper = new QueryWrapper<>();
        wrapper.in("work_order_id", workOrderIds)
                .eq("deleted", 0)
                .eq("is_official_after_sales", true);
        return officialAfterSalesMapper.selectList(wrapper).stream()
                .collect(java.util.stream.Collectors.toMap(
                        OfficialAfterSalesEntity::getWorkOrderId,
                        java.util.function.Function.identity(),
                        (left, right) -> left));
    }

    private List<Long> findOfficialWorkOrderIdsByKeyword(Long storeId, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        QueryWrapper<OfficialAfterSalesEntity> wrapper = new QueryWrapper<>();
        wrapper.select("work_order_id")
                .eq("store_id", storeId)
                .eq("deleted", 0)
                .eq("is_official_after_sales", true)
                .like("official_order_no", keyword.trim());
        return officialAfterSalesMapper.selectList(wrapper).stream()
                .map(OfficialAfterSalesEntity::getWorkOrderId)
                .distinct()
                .toList();
    }

    private WorkOrderQueryResponse toQueryResponse(WorkOrderEntity entity, OfficialAfterSalesEntity officialAfterSales) {
        WorkOrderQueryResponse response = new WorkOrderQueryResponse();
        response.setId(entity.getId());
        response.setWorkOrderNo(entity.getWorkOrderNo());
        response.setCustomerNameSnapshot(entity.getCustomerNameSnapshot());
        response.setCustomerPhoneSnapshot(entity.getCustomerPhoneSnapshot());
        response.setVehicleModelSnapshot(entity.getVehicleModelSnapshot());
        response.setFrameNoSnapshot(entity.getFrameNoSnapshot());
        response.setOfficialAfterSales(officialAfterSales != null
                && Boolean.TRUE.equals(officialAfterSales.getOfficialAfterSales()));
        response.setOfficialOrderNo(officialAfterSales == null ? null : officialAfterSales.getOfficialOrderNo());
        response.setStatus(entity.getStatus());
        response.setReceivableAmount(entity.getReceivableAmount());
        response.setReceivedAmount(entity.getReceivedAmount());
        enrichStatusFields(response, entity);
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }

    private void enrichStatusFields(WorkOrderDetailResponse response, WorkOrderEntity entity) {
        CashierSummary cashier = cashierStatusService.summarize(entity);
        InventoryStatus inventoryStatus = deriveInventoryStatus(entity);
        response.setProgressStatus(entity.getStatus());
        response.setProgressStatusText(progressStatusText(entity.getStatus()));
        response.setCashierStatus(cashier.getCashierStatus());
        response.setCashierStatusText(cashier.getCashierStatusText());
        response.setInventoryStatus(inventoryStatus.getCode());
        response.setInventoryStatusText(inventoryStatus.getText());
        response.setPaymentTotal(cashier.getPaymentTotal());
        response.setRefundTotal(cashier.getRefundTotal());
        response.setNetReceived(cashier.getNetReceived());
        response.setOutstandingAmount(cashier.getOutstandingAmount());
        response.setRefundableAmount(cashier.getRefundableAmount());
        response.setCanMarkRepairDone(WorkOrderStatus.REPAIRING.getCode().equals(entity.getStatus()));
        response.setCanDeliver(WorkOrderStatus.REPAIR_DONE.getCode().equals(entity.getStatus())
                && (CashierStatus.PAID.getCode().equals(cashier.getCashierStatus())
                || CashierStatus.NO_CHARGE.getCode().equals(cashier.getCashierStatus())));
        response.setCanCancel(WorkOrderStatus.DRAFT.getCode().equals(entity.getStatus())
                || WorkOrderStatus.REPAIRING.getCode().equals(entity.getStatus()));
        response.setCanRecordPayment((WorkOrderStatus.REPAIRING.getCode().equals(entity.getStatus())
                || WorkOrderStatus.REPAIR_DONE.getCode().equals(entity.getStatus()))
                && cashier.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0);
        response.setCanRecordRefund((WorkOrderStatus.REPAIRING.getCode().equals(entity.getStatus())
                || WorkOrderStatus.REPAIR_DONE.getCode().equals(entity.getStatus())
                || WorkOrderStatus.CANCELLED.getCode().equals(entity.getStatus())
                || WorkOrderStatus.DELIVERED.getCode().equals(entity.getStatus()))
                && cashier.getRefundableAmount().compareTo(BigDecimal.ZERO) > 0);
        response.setCanRefundAfterDelivery(WorkOrderStatus.DELIVERED.getCode().equals(entity.getStatus())
                && cashier.getRefundableAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    private void enrichStatusFields(WorkOrderQueryResponse response, WorkOrderEntity entity) {
        CashierSummary cashier = cashierStatusService.summarize(entity);
        InventoryStatus inventoryStatus = deriveInventoryStatus(entity);
        response.setProgressStatus(entity.getStatus());
        response.setProgressStatusText(progressStatusText(entity.getStatus()));
        response.setCashierStatus(cashier.getCashierStatus());
        response.setCashierStatusText(cashier.getCashierStatusText());
        response.setInventoryStatus(inventoryStatus.getCode());
        response.setInventoryStatusText(inventoryStatus.getText());
        response.setPaymentTotal(cashier.getPaymentTotal());
        response.setRefundTotal(cashier.getRefundTotal());
        response.setNetReceived(cashier.getNetReceived());
        response.setOutstandingAmount(cashier.getOutstandingAmount());
        response.setRefundableAmount(cashier.getRefundableAmount());
    }

    private InventoryStatus deriveInventoryStatus(WorkOrderEntity entity) {
        String status = entity.getStatus();
        if (WorkOrderStatus.REPAIRING.getCode().equals(status)) {
            return InventoryStatus.RESERVED;
        }
        if (WorkOrderStatus.REPAIR_DONE.getCode().equals(status)
                || WorkOrderStatus.DELIVERED.getCode().equals(status)) {
            return InventoryStatus.CONSUMED;
        }
        if (WorkOrderStatus.CANCELLED.getCode().equals(status)) {
            QueryWrapper<InventoryFlowEntity> wrapper = new QueryWrapper<>();
            wrapper.eq("work_order_id", entity.getId())
                    .eq("flow_type", InventoryFlowType.RELEASE.getCode());
            return inventoryFlowMapper.selectCount(wrapper) > 0
                    ? InventoryStatus.RELEASED : InventoryStatus.NOT_RESERVED;
        }
        return InventoryStatus.NOT_RESERVED;
    }

    private String progressStatusText(String status) {
        if (WorkOrderStatus.DRAFT.getCode().equals(status)) {
            return "新建中";
        }
        if (WorkOrderStatus.REPAIRING.getCode().equals(status)) {
            return "维修中";
        }
        if (WorkOrderStatus.REPAIR_DONE.getCode().equals(status)) {
            return "维修完成";
        }
        if (WorkOrderStatus.DELIVERED.getCode().equals(status)) {
            return "已交付";
        }
        if (WorkOrderStatus.CANCELLED.getCode().equals(status)) {
            return "已取消";
        }
        return status;
    }
}
