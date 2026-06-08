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
import com.xiaoniu.aftermarket.inventory.dto.InventoryViewType;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.inventory.service.InventoryService;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.part.service.PartService;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderChargeItemEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderChargeItemMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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
    private final PartService partService;
    private final SysUserMapper userMapper;
    private final WorkOrderChargeItemMapper chargeItemMapper;

    public InventoryServiceImpl(InventoryStockMapper inventoryStockMapper,
                                InventoryFlowMapper inventoryFlowMapper,
                                PartMapper partMapper,
                                PartService partService,
                                SysUserMapper userMapper,
                                WorkOrderChargeItemMapper chargeItemMapper) {
        this.inventoryStockMapper = inventoryStockMapper;
        this.inventoryFlowMapper = inventoryFlowMapper;
        this.partMapper = partMapper;
        this.partService = partService;
        this.userMapper = userMapper;
        this.chargeItemMapper = chargeItemMapper;
    }

    @Override
    @Transactional
    public void inbound(InventoryInboundCommand command) {
        validateInbound(command);
        resolveInboundPart(command);

        PartEntity part = partMapper.selectById(command.getPartId());
        if (part == null || part.getDeleted() != null && part.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        if (!CommonStatus.ENABLED.getCode().equals(part.getStatus())) {
            throw new BusinessException(ErrorCode.PART_DISABLED);
        }
        validatePartInStore(part, command.getStoreId());

        // 行锁 SELECT FOR UPDATE 保证同一门店+配件的并发操作串行化
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

        // 入库时 actual 和 available 同步增加，reserved 不变
        // 规则：actual = available + reserved
        int afterActual = beforeActual + command.getQuantity();
        int afterAvailable = beforeAvailable + command.getQuantity();

        stock.setActualQty(afterActual);
        stock.setAvailableQty(afterAvailable);
        inventoryStockMapper.updateById(stock);

        // 每次库存变化必须生成流水记录，用于对账、审计、回溯
        // 流水记录 before/after 快照，可校验数据一致性
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

        // 更新 lastFlowId 关联最近一笔流水，便于快速追溯
        stock.setLastFlowId(flow.getId());
        stock.setLastChangedAt(now);
        inventoryStockMapper.updateById(stock);

        if (command.getUnitCost() != null) {
            part.setReferenceCostPrice(command.getUnitCost());
            partMapper.updateById(part);
        }

        String inboundBarcode = firstText(command.getBarcode(), command.getCode());
        if (StringUtils.hasText(inboundBarcode)) {
            partService.ensureBarcodeForPart(command.getStoreId(), command.getPartId(),
                    inboundBarcode, command.getOperatorId());
        }
    }

    @Override
    @Transactional
    public void adjust(InventoryAdjustCommand command) {
        validateAdjust(command);

        PartEntity part = partMapper.selectById(command.getPartId());
        if (part == null || part.getDeleted() != null && part.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        validatePartInStore(part, command.getStoreId());

        // 行锁 SELECT FOR UPDATE 保证同一门店+配件的并发操作串行化
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartIdForUpdate(
                command.getStoreId(), command.getPartId());
        if (stock == null) {
            throw new BusinessException(ErrorCode.PART_STOCK_NOT_FOUND);
        }

        int beforeActual = stock.getActualQty();
        int beforeAvailable = stock.getAvailableQty();
        int beforeReserved = stock.getReservedQty();

        // 调整时同步增减 actual 和 available，reserved 不变
        // 负数调整表示盘亏/出库，正数调整表示盘盈/入库
        int afterActual = beforeActual + command.getQuantityDelta();
        int afterAvailable = beforeAvailable + command.getQuantityDelta();

        // 校验调整后数量不能为负，防止数据异常
        if (afterAvailable < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_ADJUST_WOULD_NEGATIVE);
        }
        if (afterActual < 0) {
            throw new BusinessException(ErrorCode.INVENTORY_ADJUST_ACTUAL_NEGATIVE);
        }

        stock.setActualQty(afterActual);
        stock.setAvailableQty(afterAvailable);
        inventoryStockMapper.updateById(stock);

        // 每次库存变化必须生成流水记录，用于对账、审计、回溯
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
                                                                String partName, String source,
                                                                String view, Integer pageNo,
                                                                Integer pageSize) {
        int pn = pageNo == null ? 1 : pageNo;
        int ps = pageSize == null ? 20 : pageSize;
        InventoryViewType viewType = InventoryViewType.from(view);

        List<Long> partIds = findPartIdsByFilters(storeId, partCode, partName, source);

        QueryWrapper<InventoryStockEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
               .eq("deleted", 0);

        if (partIds != null) {
            if (partIds.isEmpty()) {
                return new PageResponse<>(List.of(), pn, ps, 0);
            }
            wrapper.in("part_id", partIds);
        }

        wrapper.last("ORDER BY id DESC");
        List<InventoryStockEntity> entities = inventoryStockMapper.selectList(wrapper);
        if (entities.isEmpty()) {
            return new PageResponse<>(List.of(), pn, ps, 0);
        }

        Set<Long> stockPartIds = entities.stream()
                .map(InventoryStockEntity::getPartId)
                .collect(Collectors.toSet());
        Map<Long, PartEntity> partMap = loadPartsMap(stockPartIds);
        Map<Long, Long> inventoryFlowCountMap = loadInventoryFlowCountMap(storeId, stockPartIds);
        Map<Long, Long> workOrderReferenceCountMap = loadWorkOrderReferenceCountMap(stockPartIds);

        List<InventoryStockQueryResponse> filtered = entities.stream()
                .map(stock -> toStockQueryResponse(
                        stock,
                        partMap.get(stock.getPartId()),
                        inventoryFlowCountMap.getOrDefault(stock.getPartId(), 0L),
                        workOrderReferenceCountMap.getOrDefault(stock.getPartId(), 0L)))
                .filter(response -> matchesInventoryView(response, viewType))
                .toList();

        long total = filtered.size();
        if (total == 0) {
            return new PageResponse<>(List.of(), pn, ps, 0);
        }

        int fromIndex = Math.min((pn - 1) * ps, filtered.size());
        int toIndex = Math.min(fromIndex + ps, filtered.size());
        return new PageResponse<>(filtered.subList(fromIndex, toIndex), pn, ps, total);
    }

    @Override
    public PageResponse<InventoryStockQueryResponse> pageQuery(Long storeId, String keyword,
                                                                String partCode, String partName,
                                                                String source, String view,
                                                                Integer pageNo, Integer pageSize) {
        // If keyword is present, use multi-field keyword search for parts
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Long> keywordPartIds = findPartIdsByKeyword(storeId, kw);
            // Also apply partCode/partName/source filters if present
            List<Long> filterPartIds = findPartIdsByFilters(storeId, partCode, partName, source);
            List<Long> partIds = intersectPartIds(keywordPartIds, filterPartIds);
            return pageQueryWithPartIds(storeId, partIds, view, pageNo, pageSize);
        }
        return pageQuery(storeId, partCode, partName, source, view, pageNo, pageSize);
    }

    private List<Long> findPartIdsByKeyword(Long storeId, String keyword) {
        QueryWrapper<PartEntity> pw = new QueryWrapper<>();
        pw.eq("store_id", storeId).eq("deleted", 0);
        pw.and(g -> g
                .like("part_code", keyword)
                .or().like("part_name", keyword)
                .or().like("official_part_no", keyword)
                .or().like("default_barcode", keyword)
                .or().like("model", keyword)
                .or().like("location_remark", keyword));
        return partMapper.selectList(pw).stream().map(PartEntity::getId).toList();
    }

    private List<Long> intersectPartIds(List<Long> keywordIds, List<Long> filterIds) {
        if (filterIds == null) return keywordIds;
        if (keywordIds.isEmpty()) return List.of();
        Set<Long> filterSet = new HashSet<>(filterIds);
        return keywordIds.stream().filter(filterSet::contains).toList();
    }

    private PageResponse<InventoryStockQueryResponse> pageQueryWithPartIds(Long storeId,
                                                                           List<Long> partIds,
                                                                           String view,
                                                                           Integer pageNo,
                                                                           Integer pageSize) {
        int pn = pageNo == null ? 1 : pageNo;
        int ps = pageSize == null ? 20 : pageSize;
        InventoryViewType viewType = InventoryViewType.from(view);

        QueryWrapper<InventoryStockEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId).eq("deleted", 0);

        if (partIds != null) {
            if (partIds.isEmpty()) {
                return new PageResponse<>(List.of(), pn, ps, 0);
            }
            wrapper.in("part_id", partIds);
        }

        wrapper.last("ORDER BY id DESC");
        List<InventoryStockEntity> entities = inventoryStockMapper.selectList(wrapper);
        if (entities.isEmpty()) {
            return new PageResponse<>(List.of(), pn, ps, 0);
        }

        Set<Long> stockPartIds = entities.stream()
                .map(InventoryStockEntity::getPartId)
                .collect(Collectors.toSet());
        Map<Long, PartEntity> partMap = loadPartsMap(stockPartIds);
        Map<Long, Long> inventoryFlowCountMap = loadInventoryFlowCountMap(storeId, stockPartIds);
        Map<Long, Long> workOrderReferenceCountMap = loadWorkOrderReferenceCountMap(stockPartIds);

        List<InventoryStockQueryResponse> filtered = entities.stream()
                .map(stock -> toStockQueryResponse(
                        stock,
                        partMap.get(stock.getPartId()),
                        inventoryFlowCountMap.getOrDefault(stock.getPartId(), 0L),
                        workOrderReferenceCountMap.getOrDefault(stock.getPartId(), 0L)))
                .filter(response -> matchesInventoryView(response, viewType))
                .toList();

        long total = filtered.size();
        if (total == 0) {
            return new PageResponse<>(List.of(), pn, ps, 0);
        }

        int fromIndex = Math.min((pn - 1) * ps, filtered.size());
        int toIndex = Math.min(fromIndex + ps, filtered.size());
        return new PageResponse<>(filtered.subList(fromIndex, toIndex), pn, ps, total);
    }

    @Override
    public PageResponse<InventoryFlowQueryResponse> pageFlowQuery(InventoryFlowQueryRequest request) {
        int pn = request.getPageNo() == null ? 1 : request.getPageNo();
        int ps = request.getPageSize() == null ? 20 : request.getPageSize();

        List<Long> partIds = findPartIdsByFilters(request.getStoreId(),
                request.getPartCode(), request.getPartName(), null);

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
            Map<Long, SysUserEntity> userMap = loadUsersMap(entities.stream()
                    .map(InventoryFlowEntity::getOperatorId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet()));

            records = entities.stream()
                    .map(flow -> toFlowQueryResponse(flow, partMap.get(flow.getPartId()), userMap.get(flow.getOperatorId())))
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

    private Map<Long, SysUserEntity> loadUsersMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(new ArrayList<>(userIds)).stream()
                .collect(Collectors.toMap(SysUserEntity::getId, u -> u));
    }

    private List<Long> findPartIdsByFilters(Long storeId, String partCode, String partName, String source) {
        if (!StringUtils.hasText(partCode) && !StringUtils.hasText(partName) && !StringUtils.hasText(source)) {
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
        if (StringUtils.hasText(source)) {
            pw.eq("source", source);
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
        if (command.getPartId() == null
                && !StringUtils.hasText(command.getBarcode())
                && !StringUtils.hasText(command.getCode())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "partId或条码不能为空");
        }
    }

    private void resolveInboundPart(InventoryInboundCommand command) {
        if (command.getPartId() != null) {
            return;
        }
        String code = firstText(command.getBarcode(), command.getCode());
        PartEntity part = partService.findVisiblePartByCode(command.getStoreId(), code);
        if (part == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND, "未找到对应配件，请先新增配件");
        }
        if (!CommonStatus.ENABLED.getCode().equals(part.getStatus())) {
            throw new BusinessException(ErrorCode.PART_DISABLED, "该配件已停用，请先在管理端启用后再入库");
        }
        command.setPartId(part.getId());
    }

    private String firstText(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : null;
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
                                                              PartEntity part,
                                                              long inventoryFlowCount,
                                                              long workOrderReferenceCount) {
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
            response.setPartStatus(part.getStatus());
        }
        boolean hasHistoryReference = inventoryFlowCount > 0 || workOrderReferenceCount > 0;
        boolean archived = part != null
                && CommonStatus.DISABLED.getCode().equals(part.getStatus())
                && stock.getActualQty() == 0
                && stock.getAvailableQty() == 0
                && stock.getReservedQty() == 0
                && hasHistoryReference;
        response.setHasHistoryReference(hasHistoryReference);
        response.setArchived(archived);
        response.setCanUseForNewBusiness(part != null
                && CommonStatus.ENABLED.getCode().equals(part.getStatus()));
        if (stock.getReservedQty() != null && stock.getReservedQty() > 0) {
            response.setInventoryStateCode("HAS_RESERVED");
            response.setInventoryStateTag("有预占");
        } else if (part != null
                && CommonStatus.DISABLED.getCode().equals(part.getStatus())
                && (stock.getActualQty() > 0 || stock.getAvailableQty() > 0 || stock.getReservedQty() > 0)) {
            response.setInventoryStateCode("DISABLED_WITH_STOCK");
            response.setInventoryStateTag("已停用仍有库存");
        } else if (archived) {
            response.setInventoryStateCode("ARCHIVED");
            response.setInventoryStateTag("历史/归档");
        } else if (stock.getActualQty() == 0 && stock.getAvailableQty() == 0 && stock.getReservedQty() == 0) {
            response.setInventoryStateCode("ZERO_STOCK");
            response.setInventoryStateTag("零库存");
        } else {
            response.setInventoryStateCode("NORMAL");
            response.setInventoryStateTag("正常");
        }
        return response;
    }

    private Map<Long, Long> loadInventoryFlowCountMap(Long storeId, Set<Long> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<InventoryFlowEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
                .in("part_id", partIds)
                .select("part_id");
        return inventoryFlowMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(InventoryFlowEntity::getPartId, Collectors.counting()));
    }

    private Map<Long, Long> loadWorkOrderReferenceCountMap(Set<Long> partIds) {
        if (partIds.isEmpty()) {
            return Map.of();
        }
        QueryWrapper<WorkOrderChargeItemEntity> wrapper = new QueryWrapper<>();
        wrapper.in("part_id", partIds)
                .eq("deleted", 0)
                .select("part_id");
        return chargeItemMapper.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(WorkOrderChargeItemEntity::getPartId, Collectors.counting()));
    }

    private boolean matchesInventoryView(InventoryStockQueryResponse response, InventoryViewType viewType) {
        if (response.getPartStatus() == null) {
            return false;
        }
        boolean actualPositive = response.getActualQty() != null && response.getActualQty() > 0;
        boolean availablePositive = response.getAvailableQty() != null && response.getAvailableQty() > 0;
        boolean reservedPositive = response.getReservedQty() != null && response.getReservedQty() > 0;
        boolean zeroStock = !actualPositive && !availablePositive && !reservedPositive;
        boolean enabled = CommonStatus.ENABLED.getCode().equals(response.getPartStatus());
        boolean disabled = CommonStatus.DISABLED.getCode().equals(response.getPartStatus());

        return switch (viewType) {
            case DEFAULT -> actualPositive || availablePositive || reservedPositive;
            case ALL -> true;
            case HAS_STOCK -> actualPositive;
            case HAS_RESERVED -> reservedPositive;
            case ZERO_STOCK -> enabled && zeroStock;
            case DISABLED_WITH_STOCK -> disabled && (actualPositive || availablePositive || reservedPositive);
            case ARCHIVED -> disabled && zeroStock && Boolean.TRUE.equals(response.getHasHistoryReference());
        };
    }

    private InventoryFlowQueryResponse toFlowQueryResponse(InventoryFlowEntity flow,
                                                            PartEntity part,
                                                            SysUserEntity operator) {
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
        response.setOperatorName(operatorName(operator));
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

    private String operatorName(SysUserEntity operator) {
        if (operator == null) {
            return null;
        }
        if (StringUtils.hasText(operator.getRealName())) {
            return operator.getRealName();
        }
        return operator.getUsername();
    }
}
