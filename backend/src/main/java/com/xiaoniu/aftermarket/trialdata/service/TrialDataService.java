package com.xiaoniu.aftermarket.trialdata.service;

import com.xiaoniu.aftermarket.trialdata.dto.ClearTrialDataResponse;
import com.xiaoniu.aftermarket.trialdata.dto.TrialDataSummaryResponse;

public interface TrialDataService {

    TrialDataSummaryResponse getSummary(Long storeId);

    ClearTrialDataResponse clearTrialData(Long storeId);
}
