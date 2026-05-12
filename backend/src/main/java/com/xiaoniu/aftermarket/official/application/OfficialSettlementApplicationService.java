package com.xiaoniu.aftermarket.official.application;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class OfficialSettlementApplicationService {

    public void execute(Long workOrderId, BigDecimal settlementAmount, Long operatorId, String remark) {
        throw new UnsupportedOperationException("TODO: orchestrate official settlement in Phase 5");
    }
}
