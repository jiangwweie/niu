package com.xiaoniu.aftermarket.workorder.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class AddNonInventoryChargeCommand {
    private Long storeId;
    private Long workOrderId;
    private Long operatorId;
    private String chargeType;
    private String itemName;
    private Integer quantity;
    private String unit;
    private BigDecimal unitPrice;
    private String reason;
    private String remark;
}
