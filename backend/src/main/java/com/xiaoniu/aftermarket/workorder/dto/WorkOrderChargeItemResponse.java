package com.xiaoniu.aftermarket.workorder.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class WorkOrderChargeItemResponse {

    private Long id;
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
    private Boolean tempPart;
    private String status;
    private String remark;
}
