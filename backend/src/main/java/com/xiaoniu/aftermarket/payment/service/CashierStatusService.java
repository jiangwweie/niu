package com.xiaoniu.aftermarket.payment.service;

import com.xiaoniu.aftermarket.payment.dto.CashierSummary;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;

public interface CashierStatusService {

    CashierSummary summarize(WorkOrderEntity workOrder);
}
