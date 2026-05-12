package com.xiaoniu.aftermarket.common.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("store")
public class StoreEntity extends SoftDeleteEntity {

    private String storeCode;
    private String storeName;
    private String contactPhone;
    private String address;
    private String status;
    private String remark;
}
