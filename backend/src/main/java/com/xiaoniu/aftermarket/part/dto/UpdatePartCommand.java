package com.xiaoniu.aftermarket.part.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class UpdatePartCommand {

    private Long partId;
    private Long storeId;
    private Long operatorId;
    private String partName;
    private String officialPartNo;
    private String model;
    private String categoryCode;
    private BigDecimal referenceCostPrice;
    private BigDecimal defaultSalePrice;
    private String defaultBarcode;
    private String locationRemark;
    private String remark;
}
