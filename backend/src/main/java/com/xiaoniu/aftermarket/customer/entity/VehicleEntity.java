package com.xiaoniu.aftermarket.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("vehicle")
public class VehicleEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long customerId;
    private String model;
    private String frameNo;
    private String batteryNo;
    private String remark;
}
