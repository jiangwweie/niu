package com.xiaoniu.aftermarket.dict.service;

import com.xiaoniu.aftermarket.dict.entity.SysDictItemEntity;
import com.xiaoniu.aftermarket.dict.entity.SysDictTypeEntity;
import java.util.List;

public interface DictService {

    List<SysDictTypeEntity> listEnabledTypes();

    List<SysDictItemEntity> listItemsByTypeCode(String typeCode);

    SysDictItemEntity getEnabledItem(String typeCode, String itemCode);

    boolean existsEnabledItem(String typeCode, String itemCode);
}
