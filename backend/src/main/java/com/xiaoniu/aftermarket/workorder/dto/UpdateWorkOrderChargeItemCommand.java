package com.xiaoniu.aftermarket.workorder.dto;

import lombok.Data;

@Data
public class UpdateWorkOrderChargeItemCommand {

    private Long storeId;
    private String itemName;
    private Integer quantity;
    private String unit;
    private java.math.BigDecimal unitPrice;
    private String remark;
}
