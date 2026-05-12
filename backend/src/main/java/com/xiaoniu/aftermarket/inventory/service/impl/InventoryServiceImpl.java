package com.xiaoniu.aftermarket.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.InventoryFlowType;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryAdjustCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryFlowQueryRequest;
import com.xiaoniu.aftermarket.inventory.dto.InventoryFlowQueryResponse;
import com.xiaoniu.aftermarket.inventory.dto.InventoryInboundCommand;
import com.xiaoniu.aftermarket.inventory.dto.InventoryStockQueryResponse;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InventoryServiceImpl implements InventoryService {

    private final InventoryStockMapper inventoryStockMapper;
    private final InventoryFlowMapper inventoryFlowMapper;
    private final PartMapper partMapper;

    public InventoryServiceImpl(InventoryStockMapper inventoryStockMapper,
                                InventoryFlowMapper inventoryFlowMapper,
                                PartMapper partMapper) {
        this.inventoryStockMapper = inventoryStockMapper;
        this.inventoryFlowMapper = inventoryFlowMapper;
        this.partMapper = partMapper;
    }

    @Override
    @Transactional
    public void inbound(InventoryInboundCommand command) {
        validateInbound(command);

        PartEntity part = partMapper.selectById(command.getPartId());
        if (part == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        if (!CommonStatus.ENABLED.getCode().equals(part.getStatus())) {
            throw new BusinessException(ErrorCode.PART_DISABLED);
        }
        validatePartInStore(part, command.getStoreId());

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartIdForUpdate(
                command.getStoreId(), command.getPartId());

        int beforeActual;
        int beforeAvailable;
        int beforeReserved;

        if (stock == null) {
            stock = new InventoryStockEntity();
            stock.setStoreId(command.getStoreId());
            stock.setPartId(command.getPartId());
            stock.setActualQty(0);
            stock.setAvailableQty(0);
            stock.setReservedQty(0);
            stock.setCreatedBy(command.getOperatorId());
            inventoryStockMapper.insert(stock);
            beforeActual = 0;
            beforeAvailable = 0;
            beforeReserved = 0;
        } else {
            beforeActual = stock.getActualQty();
            beforeAvailable = stock.getAvailableQty();
            beforeReserved = stock.getReservedQty();
        }

        int afterActual = beforeActual + command.getQuantity();
        int afterAvailable = beforeAvailable + command.getQuantity();

        stock.setActualQty(afterActual);
        stock.setAvailableQty(afterAvailable);
        inventoryStockMapper.updateById(stock);

        LocalDateTime now = LocalDateTime.now();
        InventoryFlowEntity flow = new InventoryFlowEntity();
        flow.setStoreId(command.getStoreId());
        flow.setInventoryStockId(stock.getId());
        flow.setPartId(command.getPartId());
        flow.setFlowType(InventoryFlowType.INBOUND.getCode());
        flow.setQuantityDelta(command.getQuantity());
        flow.setActualBefore(beforeActual);
        flow.setActualAfter(afterActual);
        flow.setAvailableBefore(beforeAvailable);
        flow.setAvailableAfter(afterAvailable);
        flow.setReservedBefore(beforeReserved);
        flow.setReservedAfter(beforeReserved);
        flow.setBusinessType("MANUAL_INBOUND");
        flow.setOperatorId(command.getOperatorId());
        flow.setOperatedAt(now);
        flow.setReason(command.getReason());
        flow.setRemark(command.getRemark());
        flow.setUnitCost(command.getUnitCost());
        flow.setCreatedBy(command.getOperatorId());
        inventoryFlowMapper.insert(flow);

        stock.setLastFlowId(flow.getId());
        stock.setLastChangedAt(now);
        inventoryStockMapper.updateById(stock);

        if (command.getUnitCost() != null) {
            part.setReferenceCostPrice(command.getUnitCost());
            partMapper.updateById(part);
        }
    }

    @Override
    @Transactional
    public void adjust(InventoryAdjustCommand command) {
        validateAdjust(command);

        PartEntity part = partMapper.selectById(command.getPartId());
        if (part == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        validatePartInStore(part, command.getStoreId());

        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartIdForUpdate(
                command.getStoreId(), command.getPartId());
        if (stock == null) {
            throw new BusinessException(ErrorCode.PART_STOCK_NOT_FOUND);
        }

        int beforeActual = stock.getActualQty();
        int beforeAvailable = stock.getAvailableQty();
        int beforeReserved = stock.getReservedQty();

        int afterActual = beforeActual + command.getQuantityDelta();
        int afterAvailable = beforeAvailable + command.getQuantityDelta();

        if (afterAvailable < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_ADJUST_WOULD_NEGATIVE);
        }
        if (afterActual < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_ADJUST_ACTUAL_NEGATIVE);
        }

        stock.setActualQty(afterActual);
        stock.setAvailableQty(afterAvailable);
        inventoryStockMapper.updateById(stock);

        LocalDateTime now = LocalDateTime.now();
        InventoryFlowEntity flow = new InventoryFlowEntity();
        flow.setStoreId(command.getStoreId());
        flow.setInventoryStockId(stock.getId());
        flow.setPartId(command.getPartId());
        flow.setFlowType(InventoryFlowType.ADJUST.getCode());
        flow.setQuantityDelta(command.getQuantityDelta());
        flow.setActualBefore(beforeActual);
        flow.setActualAfter(afterActual);
        flow.setAvailableBefore(beforeAvailable);
        flow.setAvailableAfter(afterAvailable);
        flow.setReservedBefore(beforeReserved);
        flow.setReservedAfter(beforeReserved);
        flow.setBusinessType("MANUAL_ADJUST");
        flow.setOperatorId(command.getOperatorId());
        flow.setOperatedAt(now);
        flow.setReason(command.getReason());
        flow.setRemark(command.getRemark());
        flow.setCreatedBy(command.getOperatorId());
        inventoryFlowMapper.insert(flow);

        stock.setLastFlowId(flow.getId());
        stock.setLastChangedAt(now);
        inventoryStockMapper.updateById(stock);
    }

    @Override
    public InventoryStockEntity getByPartId(Long storeId, Long partId) {
        return inventoryStockMapper.selectByStoreIdAndPartId(storeId, partId);
    }

    @Override
    public PageResponse<InventoryStockQueryResponse> pageQuery(Long storeId, String partCode,
                                                                String partName, Integer pageNo,
                                                                Integer pageSize) {
        int pn = pageNo == null ? 1 : pageNo;
        int ps = pageSize == null ? 20 : pageSize;

        List<Long> partIds = findPartIdsByFilters(storeId, partCode, partName);

        QueryWrapper<InventoryStockEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
               .eq("deleted", 0);

        if (partIds != null) {
            if (partIds.isEmpty()) {
                return new PageResponse<>(List.of(), pn, ps, 0);
            }
            wrapper.in("part_id", partIds);
        }

        long total = inventoryStockMapper.selectCount(wrapper);

        List<InventoryStockQueryResponse> records;
        if (total == 0) {
            records = List.of();
        } else {
            wrapper.last("ORDER BY id DESC LIMIT " + ps + " OFFSET " + (long) (pn - 1) * ps);
            List<InventoryStockEntity> entities = inventoryStockMapper.selectList(wrapper);

            Set<Long> stockPartIds = entities.stream()
                    .map(InventoryStockEntity::getPartId)
                    .collect(Collectors.toSet());

            Map<Long, PartEntity> partMap = loadPartsMap(stockPartIds);

            records = entities.stream()
                    .map(stock -> toStockQueryResponse(stock, partMap.get(stock.getPartId())))
                    .toList();
        }

        return new PageResponse<>(records, pn, ps, total);
    }

    @Override
    public PageResponse<InventoryFlowQueryResponse> pageFlowQuery(InventoryFlowQueryRequest request) {
        int pn = request.getPageNo() == null ? 1 : request.getPageNo();
        int ps = request.getPageSize() == null ? 20 : request.getPageSize();

        List<Long> partIds = findPartIdsByFilters(request.getStoreId(),
                request.getPartCode(), request.getPartName());

        QueryWrapper<InventoryFlowEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId());

        if (partIds != null) {
            if (partIds.isEmpty()) {
                return new PageResponse<>(List.of(), pn, ps, 0);
            }
            wrapper.in("part_id", partIds);
        }
        if (StringUtils.hasText(request.getFlowType())) {
            wrapper.eq("flow_type", request.getFlowType());
        }

        long total = inventoryFlowMapper.selectCount(wrapper);

        List<InventoryFlowQueryResponse> records;
        if (total == 0) {
            records = List.of();
        } else {
            wrapper.last("ORDER BY id DESC LIMIT " + ps + " OFFSET " + (long) (pn - 1) * ps);
            List<InventoryFlowEntity> entities = inventoryFlowMapper.selectList(wrapper);

            Set<Long> flowPartIds = entities.stream()
                    .map(InventoryFlowEntity::getPartId)
                    .collect(Collectors.toSet());

            Map<Long, PartEntity> partMap = loadPartsMap(flowPartIds);

            records = entities.stream()
                    .map(flow -> toFlowQueryResponse(flow, partMap.get(flow.getPartId())))
                    .toList();
        }

        return new PageResponse<>(records, pn, ps, total);
    }

    private Map<Long, PartEntity> loadPartsMap(Set<Long> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }
        List<PartEntity> parts = partMapper.selectBatchIds(new ArrayList<>(partIds));
        return parts.stream()
                .collect(Collectors.toMap(PartEntity::getId, p -> p));
    }

    private List<Long> findPartIdsByFilters(Long storeId, String partCode, String partName) {
        if (!StringUtils.hasText(partCode) && !StringUtils.hasText(partName)) {
            return null;
        }
        QueryWrapper<PartEntity> pw = new QueryWrapper<>();
        pw.eq("store_id", storeId).eq("deleted", 0);
        if (StringUtils.hasText(partCode)) {
            pw.eq("part_code", partCode);
        }
        if (StringUtils.hasText(partName)) {
            pw.like("part_name", partName);
        }
        List<PartEntity> parts = partMapper.selectList(pw);
        return parts.stream().map(PartEntity::getId).toList();
    }

    private void validateInbound(InventoryInboundCommand command) {
        if (command.getQuantity() == null || command.getQuantity() <= 0) {
            throw new BusinessException(ErrorCode.INVENTORY_QTY_MUST_POSITIVE);
        }
        if (command.getUnitCost() != null && command.getUnitCost().signum() < 0) {
            throw new BusinessException(ErrorCode.INBOUND_UNIT_COST_NEGATIVE);
        }
    }

    private void validateAdjust(InventoryAdjustCommand command) {
        if (command.getQuantityDelta() == null || command.getQuantityDelta() == 0) {
            throw new BusinessException(ErrorCode.INVENTORY_ADJUST_ZERO);
        }
        if (!StringUtils.hasText(command.getReason())) {
            throw new BusinessException(ErrorCode.INVENTORY_ADJUST_REASON_REQUIRED);
        }
    }

    private void validatePartInStore(PartEntity part, Long storeId) {
        if (!storeId.equals(part.getStoreId())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "配件不属于当前门店");
        }
    }

    private InventoryStockQueryResponse toStockQueryResponse(InventoryStockEntity stock,
                                                              PartEntity part) {
        InventoryStockQueryResponse response = new InventoryStockQueryResponse();
        response.setId(stock.getId());
        response.setStoreId(stock.getStoreId());
        response.setPartId(stock.getPartId());
        response.setActualQty(stock.getActualQty());
        response.setAvailableQty(stock.getAvailableQty());
        response.setReservedQty(stock.getReservedQty());
        response.setLastFlowId(stock.getLastFlowId());
        response.setLastChangedAt(stock.getLastChangedAt());
        if (part != null) {
            response.setPartCode(part.getPartCode());
            response.setPartName(part.getPartName());
            response.setPartSource(part.getSource());
        }
        return response;
    }

    private InventoryFlowQueryResponse toFlowQueryResponse(InventoryFlowEntity flow,
                                                            PartEntity part) {
        InventoryFlowQueryResponse response = new InventoryFlowQueryResponse();
        response.setId(flow.getId());
        response.setStoreId(flow.getStoreId());
        response.setPartId(flow.getPartId());
        response.setFlowType(flow.getFlowType());
        response.setQuantityDelta(flow.getQuantityDelta());
        response.setActualBefore(flow.getActualBefore());
        response.setActualAfter(flow.getActualAfter());
        response.setAvailableBefore(flow.getAvailableBefore());
        response.setAvailableAfter(flow.getAvailableAfter());
        response.setReservedBefore(flow.getReservedBefore());
        response.setReservedAfter(flow.getReservedAfter());
        response.setBusinessType(flow.getBusinessType());
        response.setBusinessId(flow.getBusinessId());
        response.setOperatorId(flow.getOperatorId());
        response.setOperatedAt(flow.getOperatedAt());
        response.setReason(flow.getReason());
        response.setRemark(flow.getRemark());
        response.setUnitCost(flow.getUnitCost());
        if (part != null) {
            response.setPartCode(part.getPartCode());
            response.setPartName(part.getPartName());
        }
        return response;
    }
}
