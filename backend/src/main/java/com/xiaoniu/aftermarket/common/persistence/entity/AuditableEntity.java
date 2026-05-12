package com.xiaoniu.aftermarket.common.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AuditableEntity extends CreatedEntity {

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
