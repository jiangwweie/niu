package com.xiaoniu.aftermarket.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.AuditableEntity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("inventory_flow")
public class InventoryFlowEntity extends AuditableEntity {

    private Long storeId;
    private Long inventoryStockId;
    private Long partId;
    private String flowType;
    private Integer quantityDelta;
    private Integer actualBefore;
    private Integer actualAfter;
    private Integer availableBefore;
    private Integer availableAfter;
    private Integer reservedBefore;
    private Integer reservedAfter;
    private String businessType;
    private Long businessId;
    private Long workOrderId;
    private Long workOrderChargeItemId;
    private Long operatorId;
    private LocalDateTime operatedAt;
    private String reason;
    private String remark;
    private BigDecimal unitCost;
}
