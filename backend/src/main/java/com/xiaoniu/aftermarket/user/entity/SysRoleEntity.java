package com.xiaoniu.aftermarket.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_role")
public class SysRoleEntity extends SoftDeleteEntity {

    private Long storeId;
    private String roleCode;
    private String roleName;
    private String status;
    private Integer sortOrder;
    private String remark;
}
