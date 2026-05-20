package com.xiaoniu.aftermarket.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("sys_user")
public class SysUserEntity extends SoftDeleteEntity {

    private Long storeId;
    private String username;
    private String passwordHash;
    private String realName;
    private String phone;
    private String wechatOpenid;
    private String wechatUnionid;
    private String accountType;
    private String status;
    private Boolean passwordMustChange;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastLoginAt;
    private String remark;
}
