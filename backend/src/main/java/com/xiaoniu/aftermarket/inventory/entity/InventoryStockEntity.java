package com.xiaoniu.aftermarket.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("inventory_stock")
public class InventoryStockEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long partId;
    private Integer actualQty;
    private Integer availableQty;
    private Integer reservedQty;
    private Long lastFlowId;
    private LocalDateTime lastChangedAt;
    private String remark;
}
