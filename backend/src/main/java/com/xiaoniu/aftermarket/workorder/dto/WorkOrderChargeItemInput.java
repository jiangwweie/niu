package com.xiaoniu.aftermarket.workorder.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WorkOrderChargeItemInput {

    private String chargeType;
    private String itemName;
    private Long partId;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private BigDecimal lineAmount;
    private BigDecimal costPriceSnapshot;
    private BigDecimal lineCostAmount;
    private Boolean inventoryAffecting;
    private Boolean tempPart;
    private String remark;
}
