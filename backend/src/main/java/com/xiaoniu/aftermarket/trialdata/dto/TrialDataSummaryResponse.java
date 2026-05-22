package com.xiaoniu.aftermarket.trialdata.dto;

import lombok.Data;

@Data
public class TrialDataSummaryResponse {

    private Long storeId;
    private Integer workOrderCount;
    private Integer workOrderChargeItemCount;
    private Integer workOrderStatusLogCount;
    private Integer paymentRecordCount;
    private Integer refundRecordCount;
    private Integer officialAfterSalesCount;
    private Integer reimbursementCount;
    private Integer inventoryFlowCount;
    private Integer inventoryStockCount;
    private Integer vehicleCount;
    private Integer customerCount;
    private Integer legacyWorkOrderStatusCount;
}
