package com.xiaoniu.aftermarket.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.CreatedEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_user_role")
public class SysUserRoleEntity extends CreatedEntity {

    private Long userId;
    private Long roleId;
}
