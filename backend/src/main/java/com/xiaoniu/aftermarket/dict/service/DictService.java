package com.xiaoniu.aftermarket.dict.service;

import com.xiaoniu.aftermarket.dict.entity.SysDictItemEntity;
import com.xiaoniu.aftermarket.dict.entity.SysDictTypeEntity;
import com.xiaoniu.aftermarket.dict.dto.CreateDictItemRequest;
import com.xiaoniu.aftermarket.dict.dto.UpdateDictItemRequest;
import java.util.List;

public interface DictService {

    List<SysDictTypeEntity> listEnabledTypes();

    List<SysDictItemEntity> listItemsByTypeCode(String typeCode);

    List<SysDictItemEntity> listItemsByTypeCode(String typeCode, Long storeId);

    SysDictItemEntity getEnabledItem(String typeCode, String itemCode);

    boolean existsEnabledItem(String typeCode, String itemCode);

    SysDictItemEntity createItem(String typeCode, Long storeId, Long operatorId,
                                 boolean canManageSystemItem, CreateDictItemRequest request);

    SysDictItemEntity updateItem(Long itemId, Long storeId, Long operatorId,
                                 boolean canManageSystemItem, UpdateDictItemRequest request);

    void deleteItem(Long itemId, Long storeId, Long operatorId, boolean canManageSystemItem);
}
