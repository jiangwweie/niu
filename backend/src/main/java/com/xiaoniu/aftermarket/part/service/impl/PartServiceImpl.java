package com.xiaoniu.aftermarket.part.service.impl;

import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.buildContainsPattern;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.containsCondition;
import static com.xiaoniu.aftermarket.common.util.SearchKeywordUtils.normalize;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.PartSource;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.entity.InventoryFlowEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryFlowMapper;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.dto.PartDeleteCheckResponse;
import com.xiaoniu.aftermarket.part.dto.PartLookupResponse;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.dto.UpdatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartBarcodeEntity;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartBarcodeMapper;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.part.service.PartService;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderChargeItemEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderChargeItemMapper;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartServiceImpl implements PartService {

    private static final String BARCODE_TYPE_SYSTEM = "SYSTEM";
    private static final String BARCODE_TYPE_MANUAL = "MANUAL";

    private final PartMapper partMapper;
    private final PartBarcodeMapper partBarcodeMapper;
    private final SequenceService sequenceService;
    private final InventoryStockMapper inventoryStockMapper;
    private final InventoryFlowMapper inventoryFlowMapper;
    private final WorkOrderChargeItemMapper chargeItemMapper;

    public PartServiceImpl(PartMapper partMapper, PartBarcodeMapper partBarcodeMapper,
                           SequenceService sequenceService,
                           InventoryStockMapper inventoryStockMapper,
                           InventoryFlowMapper inventoryFlowMapper,
                           WorkOrderChargeItemMapper chargeItemMapper) {
        this.partMapper = partMapper;
        this.partBarcodeMapper = partBarcodeMapper;
        this.sequenceService = sequenceService;
        this.inventoryStockMapper = inventoryStockMapper;
        this.inventoryFlowMapper = inventoryFlowMapper;
        this.chargeItemMapper = chargeItemMapper;
    }

    @Override
    public PartEntity getById(Long id) {
        return partMapper.selectById(id);
    }

    @Override
    public PartEntity getByBarcode(Long storeId, String barcode) {
        String normalized = normalizeBarcode(barcode);
        if (storeId == null || normalized == null) {
            return null;
        }
        PartBarcodeEntity barcodeEntity = partBarcodeMapper.selectByStoreIdAndBarcode(storeId, normalized);
        if (barcodeEntity == null) {
            return null;
        }
        PartEntity part = partMapper.selectById(barcodeEntity.getPartId());
        if (part == null || (part.getDeleted() != null && part.getDeleted() == 1)) {
            return null;
        }
        return repairDefaultBarcodeIfNeeded(part, null);
    }

    @Override
    public PartEntity getByPartCode(Long storeId, String partCode) {
        String normalized = normalizeBarcode(partCode);
        if (storeId == null || normalized == null) {
            return null;
        }
        QueryWrapper<PartEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
               .eq("part_code", normalized)
               .eq("deleted", 0)
               .last("LIMIT 1");
        PartEntity part = partMapper.selectOne(wrapper);
        return part == null ? null : repairDefaultBarcodeIfNeeded(part, null);
    }

    @Override
    public PartEntity findVisiblePartByCode(Long storeId, String code) {
        String normalized = normalizeBarcode(code);
        if (storeId == null || normalized == null) {
            return null;
        }

        PartBarcodeEntity barcodeEntity = partBarcodeMapper.selectByStoreIdAndBarcode(storeId, normalized);
        if (barcodeEntity != null) {
            PartEntity part = partMapper.selectById(barcodeEntity.getPartId());
            if (part != null && storeId.equals(part.getStoreId())
                    && (part.getDeleted() == null || part.getDeleted() == 0)) {
                return repairDefaultBarcodeIfNeeded(part, null);
            }
        }

        PartEntity byPartCode = getByPartCode(storeId, normalized);
        if (byPartCode != null) {
            return byPartCode;
        }

        PartEntity byOfficialPartNo = selectVisibleByColumn(storeId, "official_part_no", normalized);
        if (byOfficialPartNo != null) {
            return byOfficialPartNo;
        }

        return selectVisibleByColumn(storeId, "default_barcode", normalized);
    }

    @Override
    @Transactional
    public PartEntity createOfficialPart(CreatePartCommand command) {
        validatePartName(command.getPartName());

        if (!StringUtils.hasText(command.getOfficialPartNo())) {
            throw new BusinessException(ErrorCode.PART_OFFICIAL_CODE_REQUIRED);
        }

        PartEntity part = new PartEntity();
        part.setStoreId(command.getStoreId());
        part.setPartCode(command.getOfficialPartNo());
        part.setOfficialPartNo(command.getOfficialPartNo());
        part.setPartName(command.getPartName());
        part.setModel(command.getModel());
        part.setSource(PartSource.OFFICIAL.getCode());
        part.setCategoryCode(command.getCategoryCode());
        part.setReferenceCostPrice(command.getReferenceCostPrice());
        part.setDefaultSalePrice(command.getDefaultSalePrice());
        part.setLocationRemark(command.getLocationRemark());
        part.setCreateSource(resolveCreateSource(command));
        part.setStatus(CommonStatus.ENABLED.getCode());
        part.setRemark(command.getRemark());
        part.setCreatedBy(command.getOperatorId());

        checkPartCodeUnique(command.getStoreId(), part.getPartCode());

        partMapper.insert(part);
        initializeBarcodesForNewPart(part, command);
        return repairDefaultBarcodeIfNeeded(part, command.getOperatorId());
    }

    @Override
    @Transactional
    public PartEntity createThirdPartyPart(CreatePartCommand command) {
        validatePartName(command.getPartName());

        String partCode = sequenceService.next("PART_CODE");

        PartEntity part = new PartEntity();
        part.setStoreId(command.getStoreId());
        part.setPartCode(partCode);
        part.setPartName(command.getPartName());
        part.setModel(command.getModel());
        part.setSource(PartSource.THIRD_PARTY.getCode());
        part.setCategoryCode(command.getCategoryCode());
        part.setReferenceCostPrice(command.getReferenceCostPrice());
        part.setDefaultSalePrice(command.getDefaultSalePrice());
        part.setLocationRemark(command.getLocationRemark());
        part.setCreateSource(resolveCreateSource(command));
        part.setStatus(CommonStatus.ENABLED.getCode());
        part.setRemark(command.getRemark());
        part.setCreatedBy(command.getOperatorId());

        partMapper.insert(part);
        initializeBarcodesForNewPart(part, command);
        return repairDefaultBarcodeIfNeeded(part, command.getOperatorId());
    }

    @Override
    @Transactional
    public void updatePart(UpdatePartCommand command) {
        PartEntity existing = partMapper.selectById(command.getPartId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        if (command.getStoreId() == null || !command.getStoreId().equals(existing.getStoreId())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "配件不属于当前门店");
        }

        if (StringUtils.hasText(command.getPartName())) {
            existing.setPartName(command.getPartName());
        }
        if (command.getOfficialPartNo() != null) {
            existing.setOfficialPartNo(command.getOfficialPartNo());
        }
        if (command.getModel() != null) {
            existing.setModel(command.getModel());
        }
        if (command.getCategoryCode() != null) {
            existing.setCategoryCode(command.getCategoryCode());
        }
        if (command.getReferenceCostPrice() != null) {
            existing.setReferenceCostPrice(command.getReferenceCostPrice());
        }
        if (command.getDefaultSalePrice() != null) {
            existing.setDefaultSalePrice(command.getDefaultSalePrice());
        }
        if (command.getLocationRemark() != null) {
            existing.setLocationRemark(command.getLocationRemark());
        }
        if (command.getRemark() != null) {
            existing.setRemark(command.getRemark());
        }

        if (command.getDefaultBarcode() != null) {
            syncDefaultBarcode(existing, command.getDefaultBarcode(), command.getOperatorId());
        }
        if (command.getExternalBarcode() != null) {
            syncExternalBarcode(existing, command.getExternalBarcode(), command.getOperatorId());
        }

        partMapper.updateById(existing);
    }

    @Override
    public void enablePart(Long storeId, Long partId) {
        PartEntity existing = partMapper.selectById(partId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        if (!storeId.equals(existing.getStoreId())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "配件不属于当前门店");
        }
        existing.setStatus(CommonStatus.ENABLED.getCode());
        partMapper.updateById(existing);
        updateBarcodeStatus(storeId, partId, CommonStatus.ENABLED.getCode(), false, null);
    }

    @Override
    public void disablePart(Long storeId, Long partId) {
        PartEntity existing = partMapper.selectById(partId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        if (!storeId.equals(existing.getStoreId())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "配件不属于当前门店");
        }
        existing.setStatus(CommonStatus.DISABLED.getCode());
        partMapper.updateById(existing);
        updateBarcodeStatus(storeId, partId, CommonStatus.DISABLED.getCode(), false, null);
    }

    @Override
    public boolean canDelete(Long storeId, Long partId) {
        return buildDeleteCheck(storeId, partId).reasons().isEmpty();
    }

    @Override
    public PartDeleteCheckResponse getDeleteCheck(Long storeId, Long partId) {
        DeleteCheckResult result = buildDeleteCheck(storeId, partId);
        return new PartDeleteCheckResponse(
                result.reasons().isEmpty(),
                result.reasons(),
                new PartDeleteCheckResponse.StockSummary(
                        result.actualQty(),
                        result.availableQty(),
                        result.reservedQty()),
                new PartDeleteCheckResponse.ReferenceSummary(
                        result.inventoryFlowCount(),
                        result.workOrderChargeItemCount(),
                        result.sampleWorkOrderIds())
        );
    }

    @Override
    @Transactional
    public void deletePart(Long storeId, Long partId, Long operatorId) {
        PartEntity existing = partMapper.selectById(partId);
        if (existing == null || (existing.getDeleted() != null && existing.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        if (!storeId.equals(existing.getStoreId())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "配件不属于当前门店");
        }
        DeleteCheckResult result = buildDeleteCheck(storeId, partId);
        if (!result.reasons().isEmpty()) {
            throw new BusinessException(ErrorCode.PART_HAS_STOCK,
                    String.join("；", result.reasons()));
        }
        existing.setDeleted(1);
        existing.setStatus(CommonStatus.DISABLED.getCode());
        existing.setDefaultBarcode(null);
        existing.setUpdatedBy(operatorId);
        existing.setUpdatedAt(LocalDateTime.now());
        partMapper.updateById(existing);
        updateBarcodeStatus(storeId, partId, CommonStatus.DISABLED.getCode(), true, operatorId);
    }

    @Override
    @Transactional
    public PartBarcodeEntity createBarcode(Long storeId, Long partId, String barcode, Long operatorId) {
        String normalized = normalizeBarcode(barcode);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码不能为空");
        }
        PartEntity part = partMapper.selectById(partId);
        if (part == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        if (!storeId.equals(part.getStoreId())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "配件不属于当前门店");
        }

        PartBarcodeEntity existing = partBarcodeMapper.selectByStoreIdAndBarcode(storeId, normalized);
        if (existing != null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码已存在");
        }

        boolean hasPrimary = hasPrimaryBarcode(partId);

        PartBarcodeEntity entity = new PartBarcodeEntity();
        entity.setStoreId(storeId);
        entity.setPartId(partId);
        entity.setBarcode(normalized);
        entity.setBarcodeType(BARCODE_TYPE_MANUAL);
        entity.setPrimaryBarcode(!hasPrimary);
        entity.setStatus(CommonStatus.ENABLED.getCode());
        entity.setCreatedBy(operatorId);
        insertBarcode(entity);

        if (!hasPrimary) {
            part.setDefaultBarcode(normalized);
            part.setUpdatedBy(operatorId);
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.updateById(part);
        }

        return entity;
    }

    @Override
    @Transactional
    public void ensureBarcodeForPart(Long storeId, Long partId, String barcode, Long operatorId) {
        String normalized = normalizeBarcode(barcode);
        if (normalized == null) {
            return;
        }
        PartEntity part = partMapper.selectById(partId);
        if (part == null || (part.getDeleted() != null && part.getDeleted() == 1)) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        if (!storeId.equals(part.getStoreId())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "配件不属于当前门店");
        }

        PartBarcodeEntity existing = partBarcodeMapper.selectByStoreIdAndBarcode(storeId, normalized);
        if (existing != null) {
            if (!partId.equals(existing.getPartId())) {
                throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码已被其他配件使用");
            }
            if ((existing.getDeleted() == null || existing.getDeleted() == 0)
                    && CommonStatus.DISABLED.getCode().equals(existing.getStatus())
                    && CommonStatus.ENABLED.getCode().equals(part.getStatus())) {
                existing.setStatus(CommonStatus.ENABLED.getCode());
                existing.setUpdatedBy(operatorId);
                existing.setUpdatedAt(LocalDateTime.now());
                partBarcodeMapper.updateById(existing);
            }
            return;
        }

        boolean hasPrimary = hasPrimaryBarcode(partId);
        upsertBarcode(storeId, partId, normalized, BARCODE_TYPE_MANUAL, !hasPrimary,
                CommonStatus.ENABLED.getCode(), operatorId, null);

        if (!hasPrimary) {
            part.setDefaultBarcode(normalized);
            part.setUpdatedBy(operatorId);
            part.setUpdatedAt(LocalDateTime.now());
            partMapper.updateById(part);
        }
    }

    @Override
    @Transactional
    public void updateDefaultBarcode(Long partId, String newDefaultBarcode) {
        PartEntity part = partMapper.selectById(partId);
        if (part == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }

        String oldDefault = part.getDefaultBarcode();
        if (newDefaultBarcode != null && newDefaultBarcode.equals(oldDefault)) {
            return;
        }

        String normalized = normalizeBarcode(newDefaultBarcode);
        if (normalized == null) {
            PartBarcodeEntity oldPrimary = findPrimaryBarcode(partId);
            if (oldPrimary != null) {
                oldPrimary.setPrimaryBarcode(false);
                partBarcodeMapper.updateById(oldPrimary);
            }
            UpdateWrapper<PartEntity> uw = new UpdateWrapper<>();
            uw.eq("id", partId).set("default_barcode", null);
            partMapper.update(null, uw);
            return;
        }

        PartBarcodeEntity newBarcode = partBarcodeMapper.selectByStoreIdAndBarcode(
                part.getStoreId(), normalized);

        if (newBarcode != null && !newBarcode.getPartId().equals(partId)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码已被其他配件使用");
        }

        PartBarcodeEntity oldPrimary = findPrimaryBarcode(partId);
        if (oldPrimary != null && !oldPrimary.getBarcode().equals(normalized)) {
            oldPrimary.setPrimaryBarcode(false);
            partBarcodeMapper.updateById(oldPrimary);
        }

        if (newBarcode != null) {
            newBarcode.setPrimaryBarcode(true);
            partBarcodeMapper.updateById(newBarcode);
        } else {
            PartBarcodeEntity created = new PartBarcodeEntity();
            created.setStoreId(part.getStoreId());
            created.setPartId(partId);
            created.setBarcode(normalized);
            created.setBarcodeType(BARCODE_TYPE_MANUAL);
            created.setPrimaryBarcode(true);
            created.setStatus(CommonStatus.ENABLED.getCode());
            insertBarcode(created);
        }

        part.setDefaultBarcode(normalized);
        partMapper.updateById(part);
    }

    @Override
    public PartEntity lookupEnabledPartByCode(Long storeId, String code) {
        PartEntity part = findVisiblePartByCode(storeId, code);
        return isLookupVisible(part, storeId) ? part : null;
    }

    @Override
    public PartLookupResponse lookup(Long storeId, String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码/编码不能为空");
        }
        PartEntity part = findVisiblePartByCode(storeId, code);
        if (part == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND, "未找到对应配件");
        }
        if (!CommonStatus.ENABLED.getCode().equals(part.getStatus())) {
            throw new BusinessException(ErrorCode.PART_DISABLED, "该配件已停用，请先在管理端启用后再操作");
        }
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(storeId, part.getId());
        return PartLookupResponse.matched(part, stock);
    }

    @Override
    public PageResponse<PartQueryResponse> pageQuery(PartQueryRequest request) {
        int pageNo = request.getPageNo() == null ? 1 : request.getPageNo();
        int pageSize = request.getPageSize() == null ? 20 : request.getPageSize();

        QueryWrapper<PartEntity> wrapper = buildPartQueryWrapper(request);

        long total = partMapper.selectCount(wrapper);

        List<PartQueryResponse> records;
        if (total == 0) {
            records = List.of();
        } else {
            wrapper.last("ORDER BY id DESC LIMIT " + pageSize + " OFFSET " + (long) (pageNo - 1) * pageSize);
            List<PartEntity> entities = partMapper.selectList(wrapper);
            records = entities.stream()
                    .map(entity -> repairDefaultBarcodeIfNeeded(entity, null))
                    .map(this::toQueryResponse)
                    .toList();
        }

        return new PageResponse<>(records, pageNo, pageSize, total);
    }

    private void validatePartName(String partName) {
        if (!StringUtils.hasText(partName)) {
            throw new BusinessException(ErrorCode.PART_NAME_REQUIRED);
        }
    }

    private boolean positive(Integer value) {
        return value != null && value > 0;
    }

    private void checkPartCodeUnique(Long storeId, String partCode) {
        PartEntity existing = getByPartCode(storeId, partCode);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PART_CODE_DUPLICATE);
        }
    }

    private void syncDefaultBarcode(PartEntity part, String newDefaultBarcode, Long operatorId) {
        String normalized = normalizeBarcode(newDefaultBarcode);
        String oldDefault = part.getDefaultBarcode();
        if (normalized == null) {
            PartBarcodeEntity oldPrimary = findPrimaryBarcode(part.getId());
            if (oldPrimary != null) {
                oldPrimary.setPrimaryBarcode(false);
                oldPrimary.setUpdatedBy(operatorId);
                oldPrimary.setUpdatedAt(LocalDateTime.now());
                partBarcodeMapper.updateById(oldPrimary);
            }
            part.setDefaultBarcode(null);
            part.setUpdatedBy(operatorId);
            part.setUpdatedAt(LocalDateTime.now());
            UpdateWrapper<PartEntity> uw = new UpdateWrapper<>();
            uw.eq("id", part.getId())
              .set("default_barcode", null)
              .set("updated_by", operatorId)
              .set("updated_at", part.getUpdatedAt());
            partMapper.update(null, uw);
            return;
        }
        if (normalized.equals(oldDefault)) {
            return;
        }

        PartBarcodeEntity newBarcode = partBarcodeMapper.selectByStoreIdAndBarcode(
                part.getStoreId(), normalized);

        if (newBarcode != null && !newBarcode.getPartId().equals(part.getId())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码已被其他配件使用");
        }

        PartBarcodeEntity oldPrimary = findPrimaryBarcode(part.getId());
        if (oldPrimary != null && !oldPrimary.getBarcode().equals(normalized)) {
            oldPrimary.setPrimaryBarcode(false);
            oldPrimary.setUpdatedBy(operatorId);
            oldPrimary.setUpdatedAt(LocalDateTime.now());
            partBarcodeMapper.updateById(oldPrimary);
        }

        if (newBarcode != null) {
            newBarcode.setPrimaryBarcode(true);
            newBarcode.setBarcodeType(BARCODE_TYPE_SYSTEM);
            newBarcode.setStatus(part.getStatus());
            newBarcode.setUpdatedBy(operatorId);
            newBarcode.setUpdatedAt(LocalDateTime.now());
            partBarcodeMapper.updateById(newBarcode);
        } else {
            upsertBarcode(part.getStoreId(), part.getId(), normalized, BARCODE_TYPE_SYSTEM, true,
                    part.getStatus(), operatorId, null);
        }

        part.setDefaultBarcode(normalized);
        part.setUpdatedBy(operatorId);
        part.setUpdatedAt(LocalDateTime.now());
    }

    private void syncExternalBarcode(PartEntity part, String externalBarcode, Long operatorId) {
        String normalized = normalizeBarcode(externalBarcode);
        if (normalized == null) {
            return;
        }
        if (normalized.equals(normalizeBarcode(part.getDefaultBarcode()))) {
            return;
        }
        upsertBarcode(part.getStoreId(), part.getId(), normalized, BARCODE_TYPE_MANUAL, false,
                part.getStatus(), operatorId, null);
    }

    private String resolveCreateSource(CreatePartCommand command) {
        return StringUtils.hasText(command.getCreateSource()) ? command.getCreateSource() : "NORMAL";
    }

    private String normalizeBarcode(String barcode) {
        return StringUtils.hasText(barcode) ? barcode.trim() : null;
    }

    private void initializeBarcodesForNewPart(PartEntity part, CreatePartCommand command) {
        String systemBarcode = resolveSystemDefaultBarcode(part, command.getDefaultBarcode());
        syncDefaultBarcode(part, systemBarcode, command.getOperatorId());
        syncExternalBarcode(part, command.getExternalBarcode(), command.getOperatorId());
        partMapper.updateById(part);
    }

    private String resolveSystemDefaultBarcode(PartEntity part, String requestedDefaultBarcode) {
        String normalized = normalizeBarcode(requestedDefaultBarcode);
        if (normalized != null) {
            return normalized;
        }
        String fallback = normalizeBarcode(part.getPartCode());
        if (fallback == null) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "配件编码缺失，无法生成系统条码");
        }
        return fallback;
    }

    private PartEntity repairDefaultBarcodeIfNeeded(PartEntity part, Long operatorId) {
        if (part == null || (part.getDeleted() != null && part.getDeleted() == 1)) {
            return part;
        }
        Long effectiveOperatorId = operatorId != null ? operatorId : part.getUpdatedBy();
        String desiredDefault = resolveSystemDefaultBarcode(part, part.getDefaultBarcode());
        PartBarcodeEntity currentPrimary = findPrimaryBarcode(part.getId());
        boolean defaultChanged = !desiredDefault.equals(normalizeBarcode(part.getDefaultBarcode()));
        boolean primaryMissing = currentPrimary == null || !desiredDefault.equals(normalizeBarcode(currentPrimary.getBarcode()));
        boolean statusMismatch = currentPrimary != null
                && !Objects.equals(currentPrimary.getStatus(), part.getStatus());
        if (!defaultChanged && !primaryMissing && !statusMismatch) {
            return part;
        }

        if (defaultChanged || primaryMissing) {
            syncDefaultBarcode(part, desiredDefault, effectiveOperatorId);
        }
        updateBarcodeStatus(part.getStoreId(), part.getId(), part.getStatus(), false, effectiveOperatorId);
        if (defaultChanged || primaryMissing) {
            partMapper.updateById(part);
        }
        return partMapper.selectById(part.getId());
    }

    private boolean isLookupVisible(PartEntity part, Long storeId) {
        return part != null
                && storeId.equals(part.getStoreId())
                && (part.getDeleted() == null || part.getDeleted() == 0)
                && CommonStatus.ENABLED.getCode().equals(part.getStatus());
    }

    private PartEntity selectEnabledByColumn(Long storeId, String column, String code) {
        QueryWrapper<PartEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
               .eq(column, code)
               .eq("deleted", 0)
               .eq("status", CommonStatus.ENABLED.getCode())
               .last("LIMIT 1");
        return partMapper.selectOne(wrapper);
    }

    private PartEntity selectVisibleByColumn(Long storeId, String column, String code) {
        QueryWrapper<PartEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
                .eq(column, code)
                .eq("deleted", 0)
                .last("LIMIT 1");
        return partMapper.selectOne(wrapper);
    }

    private void insertBarcode(PartBarcodeEntity entity) {
        try {
            partBarcodeMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码已存在");
        }
    }

    private PartBarcodeEntity selectAnyBarcode(Long storeId, String barcode) {
        QueryWrapper<PartBarcodeEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
                .eq("barcode", barcode)
                .last("LIMIT 1");
        return partBarcodeMapper.selectOne(wrapper);
    }

    private PartBarcodeEntity upsertBarcode(Long storeId, Long partId, String barcode, String barcodeType,
                                            boolean primary, String status, Long operatorId, String remark) {
        PartBarcodeEntity existing = selectAnyBarcode(storeId, barcode);
        if (existing != null) {
            if (!partId.equals(existing.getPartId())) {
                throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码已被其他配件使用");
            }
            existing.setBarcodeType(barcodeType);
            existing.setPrimaryBarcode(primary);
            existing.setStatus(status);
            existing.setRemark(remark);
            existing.setDeleted(0);
            existing.setUpdatedBy(operatorId);
            existing.setUpdatedAt(LocalDateTime.now());
            partBarcodeMapper.updateById(existing);
            return existing;
        }
        PartBarcodeEntity created = new PartBarcodeEntity();
        created.setStoreId(storeId);
        created.setPartId(partId);
        created.setBarcode(barcode);
        created.setBarcodeType(barcodeType);
        created.setPrimaryBarcode(primary);
        created.setStatus(status);
        created.setRemark(remark);
        created.setCreatedBy(operatorId);
        insertBarcode(created);
        return created;
    }

    private boolean hasPrimaryBarcode(Long partId) {
        QueryWrapper<PartBarcodeEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("part_id", partId)
               .eq("is_primary", 1)
               .eq("deleted", 0)
               .last("LIMIT 1");
        return partBarcodeMapper.selectOne(wrapper) != null;
    }

    private PartBarcodeEntity findPrimaryBarcode(Long partId) {
        QueryWrapper<PartBarcodeEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("part_id", partId)
               .eq("is_primary", 1)
               .eq("deleted", 0)
               .last("LIMIT 1");
        return partBarcodeMapper.selectOne(wrapper);
    }

    private QueryWrapper<PartEntity> buildPartQueryWrapper(PartQueryRequest request) {
        QueryWrapper<PartEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", request.getStoreId())
               .eq("deleted", 0);

        String keyword = normalize(request.getKeyword());
        if (keyword != null) {
            String pattern = buildContainsPattern(keyword);
            wrapper.and(g -> g
                    .apply(containsCondition("part_code"), pattern)
                    .or().apply(containsCondition("part_name"), pattern)
                    .or().apply(containsCondition("official_part_no"), pattern)
                    .or().apply(containsCondition("default_barcode"), pattern)
                    .or().apply(containsCondition("model"), pattern)
                    .or().apply(containsCondition("location_remark"), pattern));
        }
        String partCode = normalize(request.getPartCode());
        if (partCode != null) {
            wrapper.apply(containsCondition("part_code"), buildContainsPattern(partCode));
        }
        String partName = normalize(request.getPartName());
        if (partName != null) {
            wrapper.apply(containsCondition("part_name"), buildContainsPattern(partName));
        }
        String officialPartNo = normalize(request.getOfficialPartNo());
        if (officialPartNo != null) {
            wrapper.apply(containsCondition("official_part_no"), buildContainsPattern(officialPartNo));
        }
        String barcode = normalize(request.getBarcode());
        if (barcode != null) {
            wrapper.apply(containsCondition("default_barcode"), buildContainsPattern(barcode));
        }
        String model = normalize(request.getModel());
        if (model != null) {
            wrapper.apply(containsCondition("model"), buildContainsPattern(model));
        }
        String categoryCode = normalize(request.getCategoryCode());
        if (categoryCode != null) {
            wrapper.eq("category_code", categoryCode);
        }
        String source = normalize(request.getSource());
        if (source != null) {
            wrapper.eq("source", source);
        }
        String status = normalize(request.getStatus());
        if (status != null) {
            wrapper.eq("status", status);
        }
        return wrapper;
    }

    private PartQueryResponse toQueryResponse(PartEntity entity) {
        DeleteCheckResult deleteCheck = buildDeleteCheck(entity.getStoreId(), entity.getId());
        PartQueryResponse response = new PartQueryResponse();
        response.setId(entity.getId());
        response.setStoreId(entity.getStoreId());
        response.setPartCode(entity.getPartCode());
        response.setOfficialPartNo(entity.getOfficialPartNo());
        response.setPartName(entity.getPartName());
        response.setModel(entity.getModel());
        response.setSource(entity.getSource());
        response.setCategoryCode(entity.getCategoryCode());
        response.setReferenceCostPrice(entity.getReferenceCostPrice());
        response.setDefaultSalePrice(entity.getDefaultSalePrice());
        response.setDefaultBarcode(entity.getDefaultBarcode());
        response.setLocationRemark(entity.getLocationRemark());
        response.setCreateSource(entity.getCreateSource());
        response.setStatus(entity.getStatus());
        response.setRemark(entity.getRemark());
        response.setCanDelete(deleteCheck.reasons().isEmpty());
        response.setDeleteReasons(deleteCheck.reasons());
        response.setDeleteBlockReasonSummary(deleteCheck.reasons().isEmpty()
                ? null
                : String.join("；", deleteCheck.reasons()));
        response.setActualQty(deleteCheck.actualQty());
        response.setAvailableQty(deleteCheck.availableQty());
        response.setReservedQty(deleteCheck.reservedQty());
        response.setInventoryFlowCount(deleteCheck.inventoryFlowCount());
        response.setWorkOrderChargeItemCount(deleteCheck.workOrderChargeItemCount());
        response.setArchived(deleteCheck.archived());
        response.setHasHistoryReference(deleteCheck.hasHistoryReference());
        return response;
    }

    private boolean hasStockQuantity(Long storeId, Long partId) {
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(storeId, partId);
        return stock != null && (positive(stock.getActualQty())
                || positive(stock.getAvailableQty())
                || positive(stock.getReservedQty()));
    }

    private boolean hasInventoryFlows(Long storeId, Long partId) {
        QueryWrapper<InventoryFlowEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
                .eq("part_id", partId);
        return inventoryFlowMapper.selectCount(wrapper) > 0;
    }

    private boolean hasWorkOrderReferences(Long partId) {
        QueryWrapper<WorkOrderChargeItemEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("part_id", partId)
                .eq("deleted", 0);
        return chargeItemMapper.selectCount(wrapper) > 0;
    }

    private DeleteCheckResult buildDeleteCheck(Long storeId, Long partId) {
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(storeId, partId);
        int actualQty = stock != null && stock.getActualQty() != null ? stock.getActualQty() : 0;
        int availableQty = stock != null && stock.getAvailableQty() != null ? stock.getAvailableQty() : 0;
        int reservedQty = stock != null && stock.getReservedQty() != null ? stock.getReservedQty() : 0;

        QueryWrapper<InventoryFlowEntity> flowWrapper = new QueryWrapper<>();
        flowWrapper.eq("store_id", storeId)
                .eq("part_id", partId);
        long inventoryFlowCount = inventoryFlowMapper.selectCount(flowWrapper);

        QueryWrapper<WorkOrderChargeItemEntity> referenceWrapper = new QueryWrapper<>();
        referenceWrapper.eq("part_id", partId)
                .eq("deleted", 0);
        long workOrderChargeItemCount = chargeItemMapper.selectCount(referenceWrapper);
        referenceWrapper.select("DISTINCT work_order_id");
        referenceWrapper.last("LIMIT 5");
        List<Long> sampleWorkOrderIds = chargeItemMapper.selectList(referenceWrapper).stream()
                .map(WorkOrderChargeItemEntity::getWorkOrderId)
                .filter(Objects::nonNull)
                .toList();

        List<String> reasons = new ArrayList<>();
        if (reservedQty > 0) {
            reasons.add("该配件存在未结算工单预占库存，不能删除。请先处理相关工单。");
        }
        if (actualQty > 0 || availableQty > 0) {
            reasons.add("该配件当前仍有库存，不能删除。请先通过库存调整处理库存后再删除。");
        }
        if (inventoryFlowCount > 0) {
            reasons.add("该配件已有库存流水，不能删除。可以停用，停用后不会再被新工单选择。");
        }
        if (workOrderChargeItemCount > 0) {
            reasons.add("该配件已有工单记录，不能删除。可以停用，历史工单仍会保留。");
        }
        boolean hasHistoryReference = inventoryFlowCount > 0 || workOrderChargeItemCount > 0;
        boolean archived = actualQty == 0
                && availableQty == 0
                && reservedQty == 0
                && hasHistoryReference;
        return new DeleteCheckResult(
                reasons,
                actualQty,
                availableQty,
                reservedQty,
                inventoryFlowCount,
                workOrderChargeItemCount,
                sampleWorkOrderIds,
                archived,
                hasHistoryReference
        );
    }

    private void updateBarcodeStatus(Long storeId, Long partId, String status, boolean deleted, Long operatorId) {
        UpdateWrapper<PartBarcodeEntity> wrapper = new UpdateWrapper<>();
        wrapper.eq("store_id", storeId)
                .eq("part_id", partId)
                .set("deleted", deleted ? 1 : 0)
                .set("status", status)
                .set("updated_by", operatorId)
                .set("updated_at", LocalDateTime.now());
        if (deleted) {
            wrapper.set("is_primary", false);
        }
        partBarcodeMapper.update(null, wrapper);
    }

    private record DeleteCheckResult(
            List<String> reasons,
            int actualQty,
            int availableQty,
            int reservedQty,
            long inventoryFlowCount,
            long workOrderChargeItemCount,
            List<Long> sampleWorkOrderIds,
            boolean archived,
            boolean hasHistoryReference
    ) {
    }
}
