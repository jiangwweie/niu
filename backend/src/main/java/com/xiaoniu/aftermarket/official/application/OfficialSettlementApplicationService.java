package com.xiaoniu.aftermarket.official.application;

import com.xiaoniu.aftermarket.official.dto.MarkOfficialSettledCommand;
import com.xiaoniu.aftermarket.official.service.OfficialAfterSalesService;
import org.springframework.stereotype.Service;

@Service
public class OfficialSettlementApplicationService {

    private final OfficialAfterSalesService officialAfterSalesService;

    public OfficialSettlementApplicationService(OfficialAfterSalesService officialAfterSalesService) {
        this.officialAfterSalesService = officialAfterSalesService;
    }

    public void execute(MarkOfficialSettledCommand command) {
        officialAfterSalesService.markOfficialSettled(command);
    }
}
