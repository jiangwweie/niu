package com.xiaoniu.aftermarket.part.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.enums.PartSource;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.service.SequenceService;
import com.xiaoniu.aftermarket.part.dto.CreatePartCommand;
import com.xiaoniu.aftermarket.part.dto.PartQueryRequest;
import com.xiaoniu.aftermarket.part.dto.PartQueryResponse;
import com.xiaoniu.aftermarket.part.dto.UpdatePartCommand;
import com.xiaoniu.aftermarket.part.entity.PartBarcodeEntity;
import com.xiaoniu.aftermarket.part.entity.PartEntity;
import com.xiaoniu.aftermarket.part.mapper.PartBarcodeMapper;
import com.xiaoniu.aftermarket.part.mapper.PartMapper;
import com.xiaoniu.aftermarket.part.service.PartService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class PartServiceImpl implements PartService {

    private final PartMapper partMapper;
    private final PartBarcodeMapper partBarcodeMapper;
    private final SequenceService sequenceService;

    public PartServiceImpl(PartMapper partMapper, PartBarcodeMapper partBarcodeMapper,
                           SequenceService sequenceService) {
        this.partMapper = partMapper;
        this.partBarcodeMapper = partBarcodeMapper;
        this.sequenceService = sequenceService;
    }

    @Override
    public PartEntity getById(Long id) {
        return partMapper.selectById(id);
    }

    @Override
    public PartEntity getByBarcode(Long storeId, String barcode) {
        PartBarcodeEntity barcodeEntity = partBarcodeMapper.selectByStoreIdAndBarcode(storeId, barcode);
        return barcodeEntity == null ? null : partMapper.selectById(barcodeEntity.getPartId());
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
        part.setDefaultBarcode(command.getDefaultBarcode());
        part.setLocationRemark(command.getLocationRemark());
        part.setCreateSource("NORMAL");
        part.setStatus(CommonStatus.ENABLED.getCode());
        part.setRemark(command.getRemark());
        part.setCreatedBy(command.getOperatorId());

        checkPartCodeUnique(command.getStoreId(), part.getPartCode());

        partMapper.insert(part);

        if (StringUtils.hasText(command.getDefaultBarcode())) {
            insertBarcode(command.getStoreId(), part.getId(), command.getDefaultBarcode());
        }

        return part;
    }

    @Override
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
        part.setDefaultBarcode(command.getDefaultBarcode());
        part.setLocationRemark(command.getLocationRemark());
        part.setCreateSource("NORMAL");
        part.setStatus(CommonStatus.ENABLED.getCode());
        part.setRemark(command.getRemark());
        part.setCreatedBy(command.getOperatorId());

        partMapper.insert(part);

        if (StringUtils.hasText(command.getDefaultBarcode())) {
            insertBarcode(command.getStoreId(), part.getId(), command.getDefaultBarcode());
        }

        return part;
    }

    @Override
    public void updatePart(UpdatePartCommand command) {
        PartEntity existing = partMapper.selectById(command.getPartId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
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
        if (command.getDefaultBarcode() != null) {
            existing.setDefaultBarcode(command.getDefaultBarcode());
        }
        if (command.getLocationRemark() != null) {
            existing.setLocationRemark(command.getLocationRemark());
        }
        if (command.getRemark() != null) {
            existing.setRemark(command.getRemark());
        }

        partMapper.updateById(existing);
    }

    @Override
    public void enablePart(Long partId) {
        PartEntity existing = partMapper.selectById(partId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        existing.setStatus(CommonStatus.ENABLED.getCode());
        partMapper.updateById(existing);
    }

    @Override
    public void disablePart(Long partId) {
        PartEntity existing = partMapper.selectById(partId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.PART_NOT_FOUND);
        }
        existing.setStatus(CommonStatus.DISABLED.getCode());
        partMapper.updateById(existing);
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
        if (StringUtils.hasText(request.getSource())) {
            wrapper.eq("source", request.getSource());
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq("status", request.getStatus());
        }
        return wrapper;
    }

    private void validatePartName(String partName) {
        if (!StringUtils.hasText(partName)) {
            throw new BusinessException(ErrorCode.PART_NAME_REQUIRED);
        }
    }

    private void checkPartCodeUnique(Long storeId, String partCode) {
        PartEntity existing = getByPartCode(storeId, partCode);
        if (existing != null) {
            throw new BusinessException(ErrorCode.PART_CODE_DUPLICATE);
        }
    }

    private void insertBarcode(Long storeId, Long partId, String barcode) {
        PartBarcodeEntity barcodeEntity = new PartBarcodeEntity();
        barcodeEntity.setStoreId(storeId);
        barcodeEntity.setPartId(partId);
        barcodeEntity.setBarcode(barcode);
        barcodeEntity.setBarcodeType("MANUAL");
        barcodeEntity.setPrimaryBarcode(true);
        barcodeEntity.setStatus(CommonStatus.ENABLED.getCode());
        partBarcodeMapper.insert(barcodeEntity);
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
