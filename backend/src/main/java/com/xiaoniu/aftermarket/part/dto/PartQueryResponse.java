package com.xiaoniu.aftermarket.part.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class PartQueryResponse {

    private Long id;
    private Long storeId;
    private String partCode;
    private String officialPartNo;
    private String partName;
    private String model;
    private String source;
    private String categoryCode;
    private BigDecimal referenceCostPrice;
    private BigDecimal defaultSalePrice;
    private String defaultBarcode;
    private String locationRemark;
    private String createSource;
    private String status;
    private String remark;
    private Boolean canDelete;
    private List<String> deleteReasons;
    private String deleteBlockReasonSummary;
    private Integer actualQty;
    private Integer availableQty;
    private Integer reservedQty;
    private Long inventoryFlowCount;
    private Long workOrderChargeItemCount;
    private Boolean archived;
    private Boolean hasHistoryReference;
}
