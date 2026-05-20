package com.xiaoniu.aftermarket.trialdata.service.impl;

import com.xiaoniu.aftermarket.trialdata.dto.ClearTrialDataResponse;
import com.xiaoniu.aftermarket.trialdata.dto.TrialDataSummaryResponse;
import com.xiaoniu.aftermarket.trialdata.mapper.TrialDataMapper;
import com.xiaoniu.aftermarket.trialdata.service.TrialDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrialDataServiceImpl implements TrialDataService {

    private final TrialDataMapper trialDataMapper;

    public TrialDataServiceImpl(TrialDataMapper trialDataMapper) {
        this.trialDataMapper = trialDataMapper;
    }

    @Override
    public TrialDataSummaryResponse getSummary(Long storeId) {
        TrialDataSummaryResponse response = new TrialDataSummaryResponse();
        response.setStoreId(storeId);
        response.setWorkOrderCount(trialDataMapper.countWorkOrders(storeId));
        response.setWorkOrderChargeItemCount(trialDataMapper.countWorkOrderChargeItems(storeId));
        response.setWorkOrderStatusLogCount(trialDataMapper.countWorkOrderStatusLogs(storeId));
        response.setPaymentRecordCount(trialDataMapper.countPaymentRecords(storeId));
        response.setRefundRecordCount(trialDataMapper.countRefundRecords(storeId));
        response.setOfficialAfterSalesCount(trialDataMapper.countOfficialAfterSales(storeId));
        response.setReimbursementCount(trialDataMapper.countReimbursements(storeId));
        response.setInventoryFlowCount(trialDataMapper.countInventoryFlows(storeId));
        response.setInventoryStockCount(trialDataMapper.countInventoryStocks(storeId));
        return response;
    }

    @Override
    @Transactional
    public ClearTrialDataResponse clearTrialData(Long storeId) {
        ClearTrialDataResponse response = new ClearTrialDataResponse();

        // Delete in dependency order: status logs first, then charge items, then work orders
        response.setStatusLogsDeleted(trialDataMapper.deleteWorkOrderStatusLogs(storeId));
        response.setChargeItemsDeleted(trialDataMapper.deleteWorkOrderChargeItems(storeId));
        response.setPaymentsDeleted(trialDataMapper.deletePaymentRecords(storeId));
        response.setRefundsDeleted(trialDataMapper.deleteRefundRecords(storeId));
        response.setOfficialAfterSalesDeleted(trialDataMapper.deleteOfficialAfterSales(storeId));
        response.setReimbursementsDeleted(trialDataMapper.deleteReimbursements(storeId));
        response.setWorkOrdersDeleted(trialDataMapper.deleteWorkOrders(storeId));

        // Inventory: delete flows, reset stock quantities
        response.setInventoryFlowsDeleted(trialDataMapper.deleteInventoryFlows(storeId));
        response.setInventoryStocksReset(trialDataMapper.resetInventoryStocks(storeId));

        return response;
    }
}
