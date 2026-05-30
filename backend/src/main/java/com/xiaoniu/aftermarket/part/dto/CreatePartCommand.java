package com.xiaoniu.aftermarket.part.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class CreatePartCommand {

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
    private String createSource;
    private String remark;
}
