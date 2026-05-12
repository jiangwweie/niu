package com.xiaoniu.aftermarket.dict.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_dict_item")
public class SysDictItemEntity extends SoftDeleteEntity {

    private Long typeId;
    private String itemCode;
    private String itemName;
    private Integer sortOrder;
    private String status;

    @TableField("is_system")
    private Boolean system;

    private String remark;
}
