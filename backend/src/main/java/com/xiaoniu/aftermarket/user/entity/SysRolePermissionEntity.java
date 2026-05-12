package com.xiaoniu.aftermarket.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.CreatedEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_role_permission")
public class SysRolePermissionEntity extends CreatedEntity {

    private Long roleId;
    private Long permissionId;
}
