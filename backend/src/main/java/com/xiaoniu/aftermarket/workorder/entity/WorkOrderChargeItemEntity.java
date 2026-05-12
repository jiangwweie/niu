package com.xiaoniu.aftermarket.workorder.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaoniu.aftermarket.common.persistence.entity.SoftDeleteEntity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName("work_order_charge_item")
public class WorkOrderChargeItemEntity extends SoftDeleteEntity {

    private Long storeId;
    private Long workOrderId;
    private String chargeType;
    private String itemName;
    private Long partId;
    private String partCodeSnapshot;
    private String partNameSnapshot;
    private String partSourceSnapshot;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private BigDecimal costPriceSnapshot;
    private BigDecimal lineCostAmount;

    private Boolean inventoryAffecting;

    @TableField("is_temp_part")
    private Boolean tempPart;

    private String status;
    private String remark;
}
