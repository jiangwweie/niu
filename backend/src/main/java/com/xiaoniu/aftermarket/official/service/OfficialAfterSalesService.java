package com.xiaoniu.aftermarket.official.service;

import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.official.dto.MarkNoSettlementRequiredCommand;
import com.xiaoniu.aftermarket.official.dto.MarkOfficialSettledCommand;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesQueryRequest;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesQueryResponse;
import com.xiaoniu.aftermarket.official.dto.OfficialAfterSalesResponse;
import com.xiaoniu.aftermarket.official.dto.SaveOfficialOrderInfoCommand;

public interface OfficialAfterSalesService {

    Long saveOfficialOrderInfo(SaveOfficialOrderInfoCommand command);

    void markOfficialSettled(MarkOfficialSettledCommand command);

    void markNoSettlementRequired(MarkNoSettlementRequiredCommand command);

    OfficialAfterSalesResponse getByWorkOrderId(Long storeId, Long workOrderId);

    PageResponse<OfficialAfterSalesQueryResponse> pageQuery(OfficialAfterSalesQueryRequest request);
}
