package com.xiaoniu.aftermarket.common.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CreatedEntity extends IdEntity {

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
