package com.xiaoniu.aftermarket.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.dict.dto.CreateDictItemRequest;
import com.xiaoniu.aftermarket.dict.dto.UpdateDictItemRequest;
import com.xiaoniu.aftermarket.dict.entity.SysDictItemEntity;
import com.xiaoniu.aftermarket.dict.entity.SysDictTypeEntity;
import com.xiaoniu.aftermarket.dict.mapper.SysDictItemMapper;
import com.xiaoniu.aftermarket.dict.mapper.SysDictTypeMapper;
import com.xiaoniu.aftermarket.dict.service.DictService;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class DictServiceImpl implements DictService {

    private static final String SCOPE_SYSTEM = "SYSTEM";
    private static final String SCOPE_STORE = "STORE";
    private static final String EDIT_MODE_STORE_EXTENDABLE = "STORE_EXTENDABLE";

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    public DictServiceImpl(SysDictTypeMapper dictTypeMapper, SysDictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    @Override
    public List<SysDictTypeEntity> listEnabledTypes() {
        return dictTypeMapper.selectList(
                new LambdaQueryWrapper<SysDictTypeEntity>()
                        .eq(SysDictTypeEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysDictTypeEntity::getDeleted, 0)
                        .orderByAsc(SysDictTypeEntity::getId)
        );
    }

    @Override
    public List<SysDictItemEntity> listItemsByTypeCode(String typeCode) {
        return listItemsByTypeCode(typeCode, null);
    }

    @Override
    public List<SysDictItemEntity> listItemsByTypeCode(String typeCode, Long storeId) {
        SysDictTypeEntity dictType = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictTypeEntity>()
                        .eq(SysDictTypeEntity::getTypeCode, typeCode)
                        .eq(SysDictTypeEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysDictTypeEntity::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (dictType == null) {
            return List.of();
        }
        List<SysDictItemEntity> items = dictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItemEntity>()
                        .eq(SysDictItemEntity::getTypeId, dictType.getId())
                        .eq(SysDictItemEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysDictItemEntity::getDeleted, 0)
                        .orderByAsc(SysDictItemEntity::getScope)
                        .orderByAsc(SysDictItemEntity::getSortOrder)
        );
        Map<String, SysDictItemEntity> merged = new LinkedHashMap<>();
        items.stream()
                .filter(item -> SCOPE_STORE.equals(item.getScope())
                        && storeId != null
                        && storeId.equals(item.getStoreId()))
                .forEach(item -> merged.put(item.getItemCode(), item));
        items.stream()
                .filter(item -> item.getScope() == null || SCOPE_SYSTEM.equals(item.getScope()))
                .forEach(item -> merged.putIfAbsent(item.getItemCode(), item));
        return List.copyOf(merged.values());
    }

    @Override
    public SysDictItemEntity getEnabledItem(String typeCode, String itemCode) {
        SysDictTypeEntity dictType = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictTypeEntity>()
                        .eq(SysDictTypeEntity::getTypeCode, typeCode)
                        .eq(SysDictTypeEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysDictTypeEntity::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (dictType == null) {
            return null;
        }
        return dictItemMapper.selectOne(
                new LambdaQueryWrapper<SysDictItemEntity>()
                        .eq(SysDictItemEntity::getTypeId, dictType.getId())
                        .eq(SysDictItemEntity::getItemCode, itemCode)
                        .eq(SysDictItemEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysDictItemEntity::getDeleted, 0)
                        .last("LIMIT 1")
        );
    }

    @Override
    public boolean existsEnabledItem(String typeCode, String itemCode) {
        return getEnabledItem(typeCode, itemCode) != null;
    }

    @Override
    public SysDictItemEntity createItem(String typeCode, Long storeId, Long operatorId,
                                        boolean canManageSystemItem, CreateDictItemRequest request) {
        SysDictTypeEntity dictType = requireEnabledType(typeCode);
        String scope = normalizeScope(request.scope(), canManageSystemItem);
        validateScopePermission(dictType, null, storeId, canManageSystemItem, scope);
        String itemName = request.itemName().trim();
        String itemCode = StringUtils.hasText(request.itemCode())
                ? request.itemCode().trim()
                : generateItemCode(scope, itemName);
        ensureItemCodeAvailable(dictType.getId(), scope, SCOPE_STORE.equals(scope) ? storeId : null, itemCode, null);

        SysDictItemEntity entity = new SysDictItemEntity();
        entity.setTypeId(dictType.getId());
        entity.setStoreId(SCOPE_STORE.equals(scope) ? storeId : null);
        entity.setScope(scope);
        entity.setItemCode(itemCode);
        entity.setItemName(itemName);
        entity.setSortOrder(request.sortOrder() == null ? 100 : request.sortOrder());
        entity.setStatus(CommonStatus.ENABLED.name());
        entity.setIsSystem(SCOPE_SYSTEM.equals(scope));
        entity.setRemark(trimToNull(request.remark()));
        entity.setCreatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        entity.setDeleted(0);
        dictItemMapper.insert(entity);
        return entity;
    }

    @Override
    public SysDictItemEntity updateItem(Long itemId, Long storeId, Long operatorId,
                                        boolean canManageSystemItem, UpdateDictItemRequest request) {
        SysDictItemEntity item = requireActiveItem(itemId);
        SysDictTypeEntity dictType = requireEnabledType(item.getTypeId());
        validateScopePermission(dictType, item, storeId, canManageSystemItem, item.getScope());
        item.setItemName(request.itemName().trim());
        if (request.sortOrder() != null) {
            item.setSortOrder(request.sortOrder());
        }
        if (StringUtils.hasText(request.status())) {
            String status = request.status().trim();
            if (!CommonStatus.ENABLED.name().equals(status) && !CommonStatus.DISABLED.name().equals(status)) {
                throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "字典状态无效");
            }
            item.setStatus(status);
        }
        item.setRemark(trimToNull(request.remark()));
        item.setUpdatedBy(operatorId);
        item.setUpdatedAt(LocalDateTime.now());
        dictItemMapper.updateById(item);
        return item;
    }

    @Override
    public void deleteItem(Long itemId, Long storeId, Long operatorId, boolean canManageSystemItem) {
        SysDictItemEntity item = requireActiveItem(itemId);
        SysDictTypeEntity dictType = requireEnabledType(item.getTypeId());
        validateScopePermission(dictType, item, storeId, canManageSystemItem, item.getScope());
        item.setDeleted(1);
        item.setUpdatedBy(operatorId);
        item.setUpdatedAt(LocalDateTime.now());
        dictItemMapper.updateById(item);
    }

    private SysDictTypeEntity requireEnabledType(String typeCode) {
        SysDictTypeEntity dictType = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictTypeEntity>()
                        .eq(SysDictTypeEntity::getTypeCode, typeCode)
                        .eq(SysDictTypeEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysDictTypeEntity::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (dictType == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "字典类型不存在");
        }
        return dictType;
    }

    private SysDictTypeEntity requireEnabledType(Long typeId) {
        SysDictTypeEntity dictType = dictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictTypeEntity>()
                        .eq(SysDictTypeEntity::getId, typeId)
                        .eq(SysDictTypeEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysDictTypeEntity::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (dictType == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "字典类型不存在");
        }
        return dictType;
    }

    private SysDictItemEntity requireActiveItem(Long itemId) {
        SysDictItemEntity item = dictItemMapper.selectOne(
                new LambdaQueryWrapper<SysDictItemEntity>()
                        .eq(SysDictItemEntity::getId, itemId)
                        .eq(SysDictItemEntity::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (item == null) {
            throw new BusinessException(ErrorCode.COMMON_NOT_FOUND, "字典项不存在");
        }
        return item;
    }

    private String normalizeScope(String requestedScope, boolean canManageSystemItem) {
        if (StringUtils.hasText(requestedScope)) {
            String scope = requestedScope.trim().toUpperCase();
            if (SCOPE_SYSTEM.equals(scope) || SCOPE_STORE.equals(scope)) {
                return scope;
            }
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "字典范围无效");
        }
        return canManageSystemItem ? SCOPE_SYSTEM : SCOPE_STORE;
    }

    private void validateScopePermission(SysDictTypeEntity dictType, SysDictItemEntity item,
                                         Long storeId, boolean canManageSystemItem, String scope) {
        if (SCOPE_SYSTEM.equals(scope)) {
            if (!canManageSystemItem) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "系统字典项只能由超管维护");
            }
            return;
        }
        if (!EDIT_MODE_STORE_EXTENDABLE.equals(dictType.getEditMode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该字典类型不允许门店维护");
        }
        if (storeId == null) {
            throw new BusinessException(ErrorCode.PLATFORM_STORE_CONTEXT_REQUIRED);
        }
        if (item != null && (item.getStoreId() == null || !item.getStoreId().equals(storeId)) && !canManageSystemItem) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "只能维护本门店字典项");
        }
    }

    private void ensureItemCodeAvailable(Long typeId, String scope, Long storeId, String itemCode, Long excludeId) {
        LambdaQueryWrapper<SysDictItemEntity> wrapper = new LambdaQueryWrapper<SysDictItemEntity>()
                .eq(SysDictItemEntity::getTypeId, typeId)
                .eq(SysDictItemEntity::getScope, scope)
                .eq(SysDictItemEntity::getItemCode, itemCode)
                .eq(SysDictItemEntity::getDeleted, 0);
        if (storeId == null) {
            wrapper.isNull(SysDictItemEntity::getStoreId);
        } else {
            wrapper.eq(SysDictItemEntity::getStoreId, storeId);
        }
        if (excludeId != null) {
            wrapper.ne(SysDictItemEntity::getId, excludeId);
        }
        if (dictItemMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "字典编码已存在");
        }
    }

    private String generateItemCode(String scope, String itemName) {
        String prefix = SCOPE_SYSTEM.equals(scope) ? "SYS" : "STORE";
        return prefix + "_" + Math.abs(itemName.hashCode()) + "_" + System.currentTimeMillis();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
