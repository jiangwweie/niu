package com.xiaoniu.aftermarket.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("customer")
public class CustomerEntity extends SoftDeleteEntity {

    private Long storeId;
    private String customerName;
    private String phone;
    private String remark;
}
