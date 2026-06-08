package com.xiaoniu.aftermarket.part.dto;

import lombok.Data;

@Data
public class PartQueryRequest {

    private Long storeId;
    private String partCode;
    private String partName;
    private String officialPartNo;
    private String model;
    private String categoryCode;
    private String source;
    private String keyword;
    private String barcode;
    private String status;
    private Integer pageNo;
    private Integer pageSize;
}
