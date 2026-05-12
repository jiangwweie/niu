package com.xiaoniu.aftermarket.part.dto;

import lombok.Data;

@Data
public class PartQueryRequest {

    private Long storeId;
    private String partCode;
    private String partName;
    private String source;
    private String status;
    private Integer pageNo;
    private Integer pageSize;
}
