package com.xiaoniu.aftermarket.part.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.PartSource;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.inventory.entity.InventoryStockEntity;
import com.xiaoniu.aftermarket.inventory.mapper.InventoryStockMapper;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.dto.PartLookupResponse;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.dto.UpdatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartBarcodeEntity;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartBarcodeMapper;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.part.service.PartService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PartServiceImpl implements PartService {

    private final PartMapper partMapper;
    private final PartBarcodeMapper partBarcodeMapper;
    private final SequenceService sequenceService;
    private final InventoryStockMapper inventoryStockMapper;

    public PartServiceImpl(PartMapper partMapper, PartBarcodeMapper partBarcodeMapper,
                           SequenceService sequenceService,
                           InventoryStockMapper inventoryStockMapper) {
        this.partMapper = partMapper;
        this.partBarcodeMapper = partBarcodeMapper;
        this.sequenceService = sequenceService;
        this.inventoryStockMapper = inventoryStockMapper;
    }

    @Override
    public PartEntity getById(Long id) {
        return partMapper.selectById(id);
    }

    @Override
    public PartEntity getByBarcode(Long storeId, String barcode) {
        PartBarcodeEntity barcodeEntity = partBarcodeMapper.selectByStoreIdAndBarcode(storeId, barcode);
        if (barcodeEntity == null) {
            return null;
        }
        PartEntity part = partMapper.selectById(barcodeEntity.getPartId());
        return part == null || (part.getDeleted() != null && part.getDeleted() == 1) ? null : part;
    }

    @Override
    public PartEntity getByPartCode(Long storeId, String partCode) {
        QueryWrapper<PartEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("store_id", storeId)
               .eq("part_code", partCode)
               .eq("deleted", 0)
               .last("LIMIT 1");
        return partMapper.selectOne(wrapper);
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
        part.setLocationRemark(command.getLocationRemark());
        part.setCreateSource(resolveCreateSource(command));
        part.setStatus(CommonStatus.ENABLED.getCode());
        part.setRemark(command.getRemark());
        part.setCreatedBy(command.getOperatorId());

        checkPartCodeUnique(command.getStoreId(), part.getPartCode());

        partMapper.insert(part);
        if (StringUtils.hasText(command.getDefaultBarcode())) {
            syncDefaultBarcode(part, command.getDefaultBarcode(), command.getOperatorId());
            partMapper.updateById(part);
        }
        return part;
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
        part.setLocationRemark(command.getLocationRemark());
        part.setCreateSource(resolveCreateSource(command));
        part.setStatus(CommonStatus.ENABLED.getCode());
        part.setRemark(command.getRemark());
        part.setCreatedBy(command.getOperatorId());

        partMapper.insert(part);
        if (StringUtils.hasText(command.getDefaultBarcode())) {
            syncDefaultBarcode(part, command.getDefaultBarcode(), command.getOperatorId());
            partMapper.updateById(part);
        }
        return part;
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
        if (command.getLocationRemark() != null) {
            existing.setLocationRemark(command.getLocationRemark());
        }
        if (command.getRemark() != null) {
            existing.setRemark(command.getRemark());
        }

        if (command.getDefaultBarcode() != null) {
            syncDefaultBarcode(existing, command.getDefaultBarcode(), command.getOperatorId());
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
        InventoryStockEntity stock = inventoryStockMapper.selectByStoreIdAndPartId(storeId, partId);
        if (stock != null && (positive(stock.getActualQty())
                || positive(stock.getAvailableQty())
                || positive(stock.getReservedQty()))) {
            throw new BusinessException(ErrorCode.PART_HAS_STOCK);
        }
        existing.setDeleted(1);
        existing.setUpdatedBy(operatorId);
        existing.setUpdatedAt(LocalDateTime.now());
        partMapper.updateById(existing);
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
        entity.setBarcodeType("MANUAL");
        entity.setPrimaryBarcode(!hasPrimary);
        entity.setStatus(CommonStatus.ENABLED.getCode());
        entity.setCreatedBy(operatorId);
        insertBarcode(entity);

        if (!hasPrimary) {
            part.setDefaultBarcode(normalized);
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
            return;
        }

        boolean hasPrimary = hasPrimaryBarcode(partId);
        PartBarcodeEntity entity = new PartBarcodeEntity();
        entity.setStoreId(storeId);
        entity.setPartId(partId);
        entity.setBarcode(normalized);
        entity.setBarcodeType("MANUAL");
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
            created.setBarcodeType("MANUAL");
            created.setPrimaryBarcode(true);
            created.setStatus(CommonStatus.ENABLED.getCode());
            insertBarcode(created);
        }

        part.setDefaultBarcode(normalized);
        partMapper.updateById(part);
    }

    @Override
    public PartEntity lookupEnabledPartByCode(Long storeId, String code) {
        String normalized = normalizeBarcode(code);
        if (storeId == null || normalized == null) {
            return null;
        }

        PartEntity byBarcode = getByBarcode(storeId, normalized);
        if (isLookupVisible(byBarcode, storeId)) {
            return byBarcode;
        }

        PartEntity byPartCode = getByPartCode(storeId, normalized);
        if (isLookupVisible(byPartCode, storeId)) {
            return byPartCode;
        }

        PartEntity byOfficialPartNo = selectEnabledByColumn(storeId, "official_part_no", normalized);
        if (byOfficialPartNo != null) {
            return byOfficialPartNo;
        }

        return selectEnabledByColumn(storeId, "default_barcode", normalized);
    }

    @Override
    public PartLookupResponse lookup(Long storeId, String code) {
        PartEntity part = lookupEnabledPartByCode(storeId, code);
        if (part == null) {
            return PartLookupResponse.notMatched();
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
            records = entities.stream().map(this::toQueryResponse).toList();
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
            newBarcode.setUpdatedBy(operatorId);
            newBarcode.setUpdatedAt(LocalDateTime.now());
            partBarcodeMapper.updateById(newBarcode);
        } else {
            PartBarcodeEntity created = new PartBarcodeEntity();
            created.setStoreId(part.getStoreId());
            created.setPartId(part.getId());
            created.setBarcode(normalized);
            created.setBarcodeType("MANUAL");
            created.setPrimaryBarcode(true);
            created.setStatus(CommonStatus.ENABLED.getCode());
            created.setCreatedBy(operatorId);
            insertBarcode(created);
        }

        part.setDefaultBarcode(normalized);
        part.setUpdatedBy(operatorId);
        part.setUpdatedAt(LocalDateTime.now());
    }

    private String resolveCreateSource(CreatePartCommand command) {
        return StringUtils.hasText(command.getCreateSource()) ? command.getCreateSource() : "NORMAL";
    }

    private String normalizeBarcode(String barcode) {
        return StringUtils.hasText(barcode) ? barcode.trim() : null;
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

    private void insertBarcode(PartBarcodeEntity entity) {
        try {
            partBarcodeMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "条码已存在");
        }
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

        if (StringUtils.hasText(request.getPartCode())) {
            wrapper.eq("part_code", request.getPartCode());
        }
        if (StringUtils.hasText(request.getPartName())) {
            wrapper.like("part_name", request.getPartName());
        }
        if (StringUtils.hasText(request.getOfficialPartNo())) {
            wrapper.eq("official_part_no", request.getOfficialPartNo());
        }
        if (StringUtils.hasText(request.getModel())) {
            wrapper.like("model", request.getModel());
        }
        if (StringUtils.hasText(request.getCategoryCode())) {
            wrapper.eq("category_code", request.getCategoryCode());
        }
        if (StringUtils.hasText(request.getSource())) {
            wrapper.eq("source", request.getSource());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq("status", request.getStatus());
        }
        return wrapper;
    }

    private PartQueryResponse toQueryResponse(PartEntity entity) {
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
        response.setDefaultBarcode(entity.getDefaultBarcode());
        response.setLocationRemark(entity.getLocationRemark());
        response.setCreateSource(entity.getCreateSource());
        response.setStatus(entity.getStatus());
        response.setRemark(entity.getRemark());
        return response;
    }
}
