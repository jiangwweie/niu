package com.xiaoniu.aftermarket.official.service.impl;

import com.xiaoniu.aftermarket.official.entity.OfficialAfterSalesEntity;
import com.xiaoniu.aftermarket.official.service.OfficialAfterSalesService;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class OfficialAfterSalesServiceImpl implements OfficialAfterSalesService {

    @Override
    public OfficialAfterSalesEntity upsertOfficialAfterSales(OfficialAfterSalesEntity officialAfterSales) {
        throw new UnsupportedOperationException("TODO: implement official after-sales upsert in Phase 5");
    }

    @Override
    public void markOfficialSettled(Long workOrderId, BigDecimal settlementAmount, Long operatorId, String remark) {
        throw new UnsupportedOperationException("TODO: implement official settlement in Phase 5");
    }
}
