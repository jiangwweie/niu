package com.xiaoniu.aftermarket.common.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class SoftDeleteEntity extends AuditableEntity {

    private Integer deleted;
}
