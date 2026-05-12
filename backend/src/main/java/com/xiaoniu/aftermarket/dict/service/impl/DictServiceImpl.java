package com.xiaoniu.aftermarket.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xiaoniu.aftermarket.common.enums.CommonStatus;
import com.xiaoniu.aftermarket.dict.entity.SysDictItemEntity;
import com.xiaoniu.aftermarket.dict.entity.SysDictTypeEntity;
import com.xiaoniu.aftermarket.dict.mapper.SysDictItemMapper;
import com.xiaoniu.aftermarket.dict.mapper.SysDictTypeMapper;
import com.xiaoniu.aftermarket.dict.service.DictService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DictServiceImpl implements DictService {

    private final SysDictTypeMapper dictTypeMapper;
    private final SysDictItemMapper dictItemMapper;

    public DictServiceImpl(SysDictTypeMapper dictTypeMapper, SysDictItemMapper dictItemMapper) {
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    @Override
    public List<SysDictItemEntity> listItemsByTypeCode(String typeCode) {
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
        return dictItemMapper.selectList(
                new LambdaQueryWrapper<SysDictItemEntity>()
                        .eq(SysDictItemEntity::getTypeId, dictType.getId())
                        .eq(SysDictItemEntity::getStatus, CommonStatus.ENABLED.name())
                        .eq(SysDictItemEntity::getDeleted, 0)
                        .orderByAsc(SysDictItemEntity::getSortOrder)
        );
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
}
