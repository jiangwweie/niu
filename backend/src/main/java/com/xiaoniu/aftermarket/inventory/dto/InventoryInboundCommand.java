package com.xiaoniu.aftermarket.inventory.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class InventoryInboundCommand {

    private Long storeId;
    private Long partId;
    private Integer quantity;
    private BigDecimal unitCost;
    private String barcode;
    private String code;
    private String locationRemark;
    private Long operatorId;
    private String reason;
    private String remark;
}
