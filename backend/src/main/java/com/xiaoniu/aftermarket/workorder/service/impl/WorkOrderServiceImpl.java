package com.xiaoniu.aftermarket.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.ChargeType;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
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
import java.util.List;
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

    public WorkOrderServiceImpl(WorkOrderMapper workOrderMapper,
                                WorkOrderChargeItemMapper chargeItemMapper,
                                WorkOrderStatusLogMapper statusLogMapper,
                                SequenceService sequenceService,
                                PartMapper partMapper) {
        this.workOrderMapper = workOrderMapper;
        this.chargeItemMapper = chargeItemMapper;
        this.statusLogMapper = statusLogMapper;
        this.sequenceService = sequenceService;
        this.partMapper = partMapper;
    }

    @Override
    @Transactional
    public Long createDraft(CreateDraftWorkOrderCommand command) {
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
        if (command.getStoreId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "storeId不能为空");
        }
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        if (entity == null || !command.getStoreId().equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        if (!WorkOrderStatus.DRAFT.getCode().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_DRAFT);
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
    public WorkOrderDetailResponse getById(Long workOrderId) {
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        if (entity == null) {
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

        QueryWrapper<WorkOrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId()).eq("deleted", 0);

        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq("status", request.getStatus());
        }
        if (StringUtils.hasText(request.getWorkOrderNo())) {
            wrapper.eq("work_order_no", request.getWorkOrderNo());
        }
        if (StringUtils.hasText(request.getCustomerName())) {
            wrapper.like("customer_name_snapshot", request.getCustomerName());
        }

        long total = workOrderMapper.selectCount(wrapper);

        List<WorkOrderQueryResponse> records;
        if (total == 0) {
            records = List.of();
        } else {
            wrapper.last("ORDER BY id DESC LIMIT " + ps + " OFFSET " + (long) (pn - 1) * ps);
            List<WorkOrderEntity> entities = workOrderMapper.selectList(wrapper);
            records = entities.stream().map(this::toQueryResponse).toList();
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

        validateChargeItemFields(command, workOrder.getStoreId());
        populateChargeItemByType(entity, command);

        BigDecimal lineAmount = new BigDecimal(command.getQuantity())
                .multiply(command.getUnitPrice())
                .setScale(2, RoundingMode.HALF_UP);
        entity.setLineAmount(lineAmount);
        entity.setStatus("ACTIVE");
        entity.setTempPart(false);

        chargeItemMapper.insert(entity);
        recalculateReceivableAmount(workOrderId);
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateChargeItem(Long workOrderId, Long chargeItemId,
                                 UpdateWorkOrderChargeItemCommand command) {
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
        BigDecimal total = items.stream()
                .map(WorkOrderChargeItemEntity::getLineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        UpdateWrapper<WorkOrderEntity> uw = new UpdateWrapper<>();
        uw.eq("id", workOrderId).set("receivable_amount", total);
        workOrderMapper.update(null, uw);
    }

    @Override
    public void submit(com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand command) {
        throw new UnsupportedOperationException("TODO: implement submit flow");
    }

    @Override
    public void cancel(com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand command) {
        throw new UnsupportedOperationException("TODO: implement cancel flow");
    }

    @Override
    public void settle(com.xiaoniu.aftermarket.workorder.dto.SettleWorkOrderCommand command) {
        throw new UnsupportedOperationException("TODO: implement settlement flow");
    }

    @Override
    public void adjustChargeItems(
            com.xiaoniu.aftermarket.workorder.dto.AdjustChargeItemsCommand command) {
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
        WorkOrderStatusLogEntity log = new WorkOrderStatusLogEntity();
        log.setStoreId(storeId);
        log.setWorkOrderId(workOrderId);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActionType(actionType);
        log.setOperatorId(operatorId);
        log.setOperatedAt(LocalDateTime.now());
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

    private WorkOrderQueryResponse toQueryResponse(WorkOrderEntity entity) {
        WorkOrderQueryResponse response = new WorkOrderQueryResponse();
        response.setId(entity.getId());
        response.setWorkOrderNo(entity.getWorkOrderNo());
        response.setCustomerNameSnapshot(entity.getCustomerNameSnapshot());
        response.setVehicleModelSnapshot(entity.getVehicleModelSnapshot());
        response.setStatus(entity.getStatus());
        response.setReceivableAmount(entity.getReceivableAmount());
        response.setCreatedAt(entity.getCreatedAt());
        return response;
    }
}
