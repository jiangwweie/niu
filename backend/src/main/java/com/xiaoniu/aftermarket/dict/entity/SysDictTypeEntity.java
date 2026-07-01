package com.xiaoniu.aftermarket.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_dict_type")
public class SysDictTypeEntity extends SoftDeleteEntity {

    private String typeCode;
    private String typeName;
    private String status;
    private String editMode;
    private String remark;
}
