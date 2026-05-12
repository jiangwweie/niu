package com.xiaoniu.aftermarket.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_permission")
public class SysPermissionEntity extends SoftDeleteEntity {

    private String permissionCode;
    private String permissionName;
    private String moduleCode;
    private String status;
    private Integer sortOrder;
    private String remark;
}
