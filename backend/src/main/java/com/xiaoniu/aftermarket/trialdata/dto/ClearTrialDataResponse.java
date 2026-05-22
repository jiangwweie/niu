package com.xiaoniu.aftermarket.trialdata.dto;

import lombok.Data;

@Data
public class ClearTrialDataResponse {

    private TrialDataSummaryResponse beforeSummary;
    private TrialDataSummaryResponse afterSummary;
    private Integer workOrdersDeleted;
    private Integer chargeItemsDeleted;
    private Integer statusLogsDeleted;
    private Integer paymentsDeleted;
    private Integer refundsDeleted;
    private Integer officialAfterSalesDeleted;
    private Integer reimbursementsDeleted;
    private Integer inventoryFlowsDeleted;
    private Integer inventoryStocksReset;
    private Integer vehiclesDeleted;
    private Integer customersDeleted;
}
