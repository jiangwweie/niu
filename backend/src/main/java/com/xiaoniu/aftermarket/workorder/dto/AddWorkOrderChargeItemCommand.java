package com.xiaoniu.aftermarket.workorder.dto;

import lombok.Data;

@Data
public class AddWorkOrderChargeItemCommand {

    private Long storeId;
    private String chargeType;
    private String itemName;
    private Long partId;
    private Integer quantity;
    private String unit;
    private java.math.BigDecimal unitPrice;
    private String remark;
}
