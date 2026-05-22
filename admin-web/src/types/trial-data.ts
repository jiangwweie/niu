export interface TrialDataSummaryResponse {
  storeId: number;
  workOrderCount: number;
  workOrderChargeItemCount: number;
  workOrderStatusLogCount: number;
  paymentRecordCount: number;
  refundRecordCount: number;
  officialAfterSalesCount: number;
  reimbursementCount: number;
  inventoryFlowCount: number;
  inventoryStockCount: number;
  vehicleCount: number;
  customerCount: number;
  legacyWorkOrderStatusCount: number;
}

export interface ClearTrialDataResponse {
  workOrdersDeleted: number;
  chargeItemsDeleted: number;
  statusLogsDeleted: number;
  paymentsDeleted: number;
  refundsDeleted: number;
  officialAfterSalesDeleted: number;
  reimbursementsDeleted: number;
  inventoryFlowsDeleted: number;
  inventoryStocksReset: number;
  vehiclesDeleted: number;
  customersDeleted: number;
}
