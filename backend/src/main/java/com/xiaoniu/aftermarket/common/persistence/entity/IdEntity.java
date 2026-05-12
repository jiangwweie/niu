package com.xiaoniu.aftermarket.common.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class IdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
}
