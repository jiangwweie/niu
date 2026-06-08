package com.xiaoniu.aftermarket.official.service.impl;

import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.buildContainsPattern;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.containsCondition;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.normalize;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.OfficialSettlementStatus;
import com.xiaoniu.aftermarket.common.enums.WorkOrderStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.official.dto.MarkNoSettlementRequiredCommand;
import com.xiaoniu.aftermarket.official.dto.MarkOfficialSettledCommand;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesQueryRequest;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesQueryResponse;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesResponse;
import com.xiaoniu.aftermarket.official.dto.SaveOfficialOrderInfoCommand;
import com.xiaoniu.aftermarket.official.entity.OfficialAfterSalesEntity;
import com.xiaoniu.aftermarket.official.mapper.OfficialAfterSalesMapper;
import com.xiaoniu.aftermarket.official.service.OfficialAfterSalesService;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class OfficialAfterSalesServiceImpl implements OfficialAfterSalesService {

    private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final OfficialAfterSalesMapper officialAfterSalesMapper;
    private final WorkOrderMapper workOrderMapper;

    public OfficialAfterSalesServiceImpl(OfficialAfterSalesMapper officialAfterSalesMapper,
                                         WorkOrderMapper workOrderMapper) {
        this.officialAfterSalesMapper = officialAfterSalesMapper;
        this.workOrderMapper = workOrderMapper;
    }

    @Override
    @Transactional
    public Long saveOfficialOrderInfo(SaveOfficialOrderInfoCommand command) {
        validateOrderInfoCommand(command);

        WorkOrderEntity workOrder = loadWorkOrderForUpdate(command.getStoreId(), command.getWorkOrderId());
        String officialOrderNo = command.getOfficialOrderNo().trim();
        validateOfficialOrderNoUnique(workOrder.getStoreId(), workOrder.getId(), officialOrderNo);

        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectByWorkOrderIdForUpdate(workOrder.getId());
        if (entity == null) {
            entity = new OfficialAfterSalesEntity();
            entity.setStoreId(workOrder.getStoreId());
            entity.setWorkOrderId(workOrder.getId());
            entity.setOfficialAfterSales(true);
            entity.setOfficialSettlementStatus(OfficialSettlementStatus.PENDING.getCode());
            entity.setOfficialOrderNo(officialOrderNo);
            entity.setRemark(command.getRemark());
            entity.setCreatedBy(command.getOperatorId());
            insertOfficialAfterSales(entity);
            return entity.getId();
        }

        // 已结算/无需结算是终态，不允许修改官方售后单号（防止绕过终态回退）
        if (isFinalSettlementStatus(entity.getOfficialSettlementStatus())
                && !officialOrderNo.equals(entity.getOfficialOrderNo())) {
            throw new BusinessException(ErrorCode.OFFICIAL_SETTLEMENT_STATUS_INVALID);
        }
        entity.setOfficialAfterSales(true);
        entity.setOfficialOrderNo(officialOrderNo);
        if (!StringUtils.hasText(entity.getOfficialSettlementStatus())) {
            entity.setOfficialSettlementStatus(OfficialSettlementStatus.PENDING.getCode());
        }
        entity.setRemark(command.getRemark());
        entity.setUpdatedBy(command.getOperatorId());
        updateOfficialAfterSales(entity);
        return entity.getId();
    }

    // 官方结算金额：第一版为人工录入，不对接官方系统自动获取
    @Override
    @Transactional
    public void markOfficialSettled(MarkOfficialSettledCommand command) {
        validateSettlementCommand(command);

        WorkOrderEntity workOrder = loadWorkOrderForUpdate(command.getStoreId(), command.getWorkOrderId());
        if (!WorkOrderStatus.DELIVERED.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.OFFICIAL_SETTLEMENT_NOT_ALLOWED);
        }

        OfficialAfterSalesEntity entity = loadOfficialAfterSalesForUpdate(workOrder.getId());
        validateOfficialAfterSales(entity);
        if (!StringUtils.hasText(entity.getOfficialOrderNo())) {
            throw new BusinessException(ErrorCode.OFFICIAL_ORDER_NO_REQUIRED);
        }
        validateEditableSettlementStatus(entity);

        LocalDateTime now = LocalDateTime.now();
        entity.setOfficialSettlementStatus(OfficialSettlementStatus.SETTLED.getCode());
        // 官方结算金额与客户已收金额互相独立，此处仅记录官方维度的结算，不影响工单的客户已收金额
        entity.setOfficialSettlementAmount(normalizeAmount(command.getSettlementAmount()));
        entity.setOfficialSettlementTime(command.getSettlementTime() != null ? command.getSettlementTime() : now);
        entity.setOfficialSettlementOperatorId(command.getOperatorId());
        entity.setOfficialSettlementRemark(command.getRemark());
        entity.setUpdatedBy(command.getOperatorId());
        officialAfterSalesMapper.updateById(entity);
    }

    // 标记无需结算：已结算状态不可回退，仅允许从待结算状态流转
    @Override
    @Transactional
    public void markNoSettlementRequired(MarkNoSettlementRequiredCommand command) {
        validateNoSettlementCommand(command);

        WorkOrderEntity workOrder = loadWorkOrderForUpdate(command.getStoreId(), command.getWorkOrderId());
        if (WorkOrderStatus.CANCELLED.getCode().equals(workOrder.getStatus())) {
            throw new BusinessException(ErrorCode.OFFICIAL_SETTLEMENT_NOT_ALLOWED);
        }

        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectByWorkOrderIdForUpdate(workOrder.getId());
        if (entity == null) {
            entity = new OfficialAfterSalesEntity();
            entity.setStoreId(workOrder.getStoreId());
            entity.setWorkOrderId(workOrder.getId());
            entity.setOfficialAfterSales(true);
            entity.setCreatedBy(command.getOperatorId());
        } else if (OfficialSettlementStatus.SETTLED.getCode().equals(entity.getOfficialSettlementStatus())) {
            throw new BusinessException(ErrorCode.OFFICIAL_SETTLEMENT_STATUS_INVALID);
        }

        LocalDateTime now = LocalDateTime.now();
        String remark = StringUtils.hasText(command.getRemark()) ? command.getRemark().trim() : command.getReason().trim();
        entity.setOfficialAfterSales(true);
        entity.setOfficialSettlementStatus(OfficialSettlementStatus.NOT_REQUIRED.getCode());
        entity.setOfficialSettlementAmount(ZERO_AMOUNT);
        entity.setOfficialSettlementTime(now);
        entity.setOfficialSettlementOperatorId(command.getOperatorId());
        entity.setOfficialSettlementRemark(remark);
        entity.setRemark(remark);
        entity.setUpdatedBy(command.getOperatorId());
        if (entity.getId() == null) {
            officialAfterSalesMapper.insert(entity);
        } else {
            officialAfterSalesMapper.updateById(entity);
        }
    }

    @Override
    public OfficialAfterSalesResponse getByWorkOrderId(Long storeId, Long workOrderId) {
        if (storeId == null || workOrderId == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null || workOrder.getStoreId() == null || !storeId.equals(workOrder.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectByWorkOrderId(workOrderId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.OFFICIAL_AFTER_SALES_NOT_FOUND);
        }
        return toResponse(entity, workOrder);
    }

    @Override
    public PageResponse<OfficialAfterSalesQueryResponse> pageQuery(OfficialAfterSalesQueryRequest request) {
        if (request == null || request.getStoreId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }

        List<Long> filteredWorkOrderIds = findWorkOrderIds(request);
        if (filteredWorkOrderIds != null && filteredWorkOrderIds.isEmpty()) {
            return new PageResponse<>(List.of(), request.normalizedPageNo(), request.normalizedPageSize(), 0);
        }

        QueryWrapper<OfficialAfterSalesEntity> wrapper = buildPageQueryWrapper(request, filteredWorkOrderIds);
        Long total = officialAfterSalesMapper.selectCount(wrapper);
        if (total == 0) {
            return new PageResponse<>(List.of(), request.normalizedPageNo(), request.normalizedPageSize(), 0);
        }

        int pageNo = request.normalizedPageNo();
        int pageSize = request.normalizedPageSize();
        long offset = (long) (pageNo - 1) * pageSize;
        wrapper.orderByDesc("id").last("LIMIT " + pageSize + " OFFSET " + offset);
        List<OfficialAfterSalesEntity> entities = officialAfterSalesMapper.selectList(wrapper);
        Map<Long, WorkOrderEntity> workOrderMap = loadWorkOrders(entities);
        List<OfficialAfterSalesQueryResponse> records = entities.stream()
                .map(entity -> toQueryResponse(entity, workOrderMap.get(entity.getWorkOrderId())))
                .toList();
        return new PageResponse<>(records, pageNo, pageSize, total);
    }

    private void validateOrderInfoCommand(SaveOfficialOrderInfoCommand command) {
        if (command == null || command.getStoreId() == null || command.getWorkOrderId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.OPERATOR_REQUIRED);
        }
        if (!StringUtils.hasText(command.getOfficialOrderNo())) {
            throw new BusinessException(ErrorCode.OFFICIAL_ORDER_NO_REQUIRED);
        }
    }

    private void validateSettlementCommand(MarkOfficialSettledCommand command) {
        if (command == null || command.getStoreId() == null || command.getWorkOrderId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.OPERATOR_REQUIRED);
        }
        if (command.getSettlementAmount() == null || command.getSettlementAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.OFFICIAL_SETTLEMENT_AMOUNT_INVALID);
        }
    }

    private void validateNoSettlementCommand(MarkNoSettlementRequiredCommand command) {
        if (command == null || command.getStoreId() == null || command.getWorkOrderId() == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
        if (command.getOperatorId() == null) {
            throw new BusinessException(ErrorCode.OPERATOR_REQUIRED);
        }
        if (!StringUtils.hasText(command.getReason()) && !StringUtils.hasText(command.getRemark())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST);
        }
    }

    private WorkOrderEntity loadWorkOrderForUpdate(Long storeId, Long workOrderId) {
        WorkOrderEntity workOrder = workOrderMapper.selectByIdForUpdate(workOrderId);
        if (workOrder == null || workOrder.getStoreId() == null || !storeId.equals(workOrder.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND);
        }
        return workOrder;
    }

    private OfficialAfterSalesEntity loadOfficialAfterSalesForUpdate(Long workOrderId) {
        OfficialAfterSalesEntity entity = officialAfterSalesMapper.selectByWorkOrderIdForUpdate(workOrderId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_OFFICIAL_AFTER_SALES);
        }
        return entity;
    }

    private void validateOfficialAfterSales(OfficialAfterSalesEntity entity) {
        if (!Boolean.TRUE.equals(entity.getOfficialAfterSales())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_OFFICIAL_AFTER_SALES);
        }
    }

    private void validateEditableSettlementStatus(OfficialAfterSalesEntity entity) {
        if (OfficialSettlementStatus.SETTLED.getCode().equals(entity.getOfficialSettlementStatus())
                || OfficialSettlementStatus.NOT_REQUIRED.getCode().equals(entity.getOfficialSettlementStatus())) {
            throw new BusinessException(ErrorCode.OFFICIAL_SETTLEMENT_STATUS_INVALID);
        }
    }

    private boolean isFinalSettlementStatus(String status) {
        return OfficialSettlementStatus.SETTLED.getCode().equals(status)
                || OfficialSettlementStatus.NOT_REQUIRED.getCode().equals(status);
    }

    private void validateOfficialOrderNoUnique(Long storeId, Long workOrderId, String officialOrderNo) {
        OfficialAfterSalesEntity existing = officialAfterSalesMapper
                .selectByStoreIdAndOfficialOrderNo(storeId, officialOrderNo);
        if (existing != null && !workOrderId.equals(existing.getWorkOrderId())) {
            throw new BusinessException(ErrorCode.OFFICIAL_ORDER_NO_DUPLICATED);
        }
    }

    private void insertOfficialAfterSales(OfficialAfterSalesEntity entity) {
        try {
            officialAfterSalesMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OFFICIAL_ORDER_NO_DUPLICATED);
        }
    }

    private void updateOfficialAfterSales(OfficialAfterSalesEntity entity) {
        try {
            officialAfterSalesMapper.updateById(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.OFFICIAL_ORDER_NO_DUPLICATED);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private List<Long> findWorkOrderIds(OfficialAfterSalesQueryRequest request) {
        String workOrderNo = normalize(request.getWorkOrderNo());
        if (workOrderNo == null) {
            return null;
        }
        QueryWrapper<WorkOrderEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId())
                .eq("deleted", 0)
                .apply(containsCondition("work_order_no"), buildContainsPattern(workOrderNo));
        return workOrderMapper.selectList(wrapper).stream()
                .map(WorkOrderEntity::getId)
                .toList();
    }

    private QueryWrapper<OfficialAfterSalesEntity> buildPageQueryWrapper(OfficialAfterSalesQueryRequest request,
                                                                         List<Long> workOrderIds) {
        QueryWrapper<OfficialAfterSalesEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId()).eq("deleted", 0);
        String officialOrderNo = normalize(request.getOfficialOrderNo());
        if (officialOrderNo != null) {
            wrapper.apply(containsCondition("official_order_no"), buildContainsPattern(officialOrderNo));
        }
        if (StringUtils.hasText(request.getSettlementStatus())) {
            wrapper.eq("official_settlement_status", request.getSettlementStatus().trim());
        }
        if (request.getSettlementStartTime() != null) {
            wrapper.ge("official_settlement_time", request.getSettlementStartTime());
        }
        if (request.getSettlementEndTime() != null) {
            wrapper.le("official_settlement_time", request.getSettlementEndTime());
        }
        if (workOrderIds != null) {
            wrapper.in("work_order_id", workOrderIds);
        }
        return wrapper;
    }

    private Map<Long, WorkOrderEntity> loadWorkOrders(List<OfficialAfterSalesEntity> entities) {
        List<Long> ids = entities.stream()
                .map(OfficialAfterSalesEntity::getWorkOrderId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return workOrderMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(WorkOrderEntity::getId, Function.identity()));
    }

    private OfficialAfterSalesResponse toResponse(OfficialAfterSalesEntity entity, WorkOrderEntity workOrder) {
        OfficialAfterSalesResponse response = new OfficialAfterSalesResponse();
        response.setId(entity.getId());
        response.setStoreId(entity.getStoreId());
        response.setWorkOrderId(entity.getWorkOrderId());
        response.setWorkOrderNo(workOrder == null ? null : workOrder.getWorkOrderNo());
        response.setOfficialAfterSales(entity.getOfficialAfterSales());
        response.setOfficialOrderNo(entity.getOfficialOrderNo());
        response.setSettlementAmount(entity.getOfficialSettlementAmount());
        response.setSettlementStatus(entity.getOfficialSettlementStatus());
        response.setSettlementTime(entity.getOfficialSettlementTime());
        response.setOperatorId(entity.getOfficialSettlementOperatorId());
        response.setSettlementRemark(entity.getOfficialSettlementRemark());
        response.setRemark(entity.getRemark());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    private OfficialAfterSalesQueryResponse toQueryResponse(OfficialAfterSalesEntity entity, WorkOrderEntity workOrder) {
        // receivedAmount 是客户已收金额（workOrder维度），settlementAmount 是官方结算金额（official维度），两者独立
        OfficialAfterSalesQueryResponse response = new OfficialAfterSalesQueryResponse();
        response.setId(entity.getId());
        response.setStoreId(entity.getStoreId());
        response.setWorkOrderId(entity.getWorkOrderId());
        response.setWorkOrderNo(workOrder == null ? null : workOrder.getWorkOrderNo());
        response.setCustomerNameSnapshot(workOrder == null ? null : workOrder.getCustomerNameSnapshot());
        response.setCustomerPhoneSnapshot(workOrder == null ? null : workOrder.getCustomerPhoneSnapshot());
        response.setVehicleModelSnapshot(workOrder == null ? null : workOrder.getVehicleModelSnapshot());
        response.setFrameNoSnapshot(workOrder == null ? null : workOrder.getFrameNoSnapshot());
        response.setReceivedAmount(workOrder == null ? null : workOrder.getReceivedAmount());
        response.setWorkOrderStatus(workOrder == null ? null : workOrder.getStatus());
        response.setOfficialOrderNo(entity.getOfficialOrderNo());
        response.setSettlementAmount(entity.getOfficialSettlementAmount());
        response.setSettlementStatus(entity.getOfficialSettlementStatus());
        response.setSettlementTime(entity.getOfficialSettlementTime());
        return response;
    }
}
