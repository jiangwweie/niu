package com.xiaoniu.aftermarket.dashboard.service;

import com.xiaoniu.aftermarket.dashboard.dto.DashboardSummaryResponse;

public interface DashboardService {

    DashboardSummaryResponse getSummary(Long storeId);
}
