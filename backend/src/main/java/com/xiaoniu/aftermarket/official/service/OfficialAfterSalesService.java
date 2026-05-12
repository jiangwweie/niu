package com.xiaoniu.aftermarket.official.service;

import com.xiaoniu.aftermarket.official.entity.OfficialAfterSalesEntity;
import java.math.BigDecimal;

public interface OfficialAfterSalesService {

    OfficialAfterSalesEntity upsertOfficialAfterSales(OfficialAfterSalesEntity officialAfterSales);

    void markOfficialSettled(Long workOrderId, BigDecimal settlementAmount, Long operatorId, String remark);
}
