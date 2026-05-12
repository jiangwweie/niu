package com.xiaoniu.aftermarket.workorder.service;

import com.xiaoniu.aftermarket.workorder.dto.AdjustChargeItemsCommand;
import com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SettleWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;

public interface WorkOrderService {

    Long createDraft(CreateDraftWorkOrderCommand command);

    WorkOrderEntity updateDraft(Long workOrderId, CreateDraftWorkOrderCommand command);

    void submit(SubmitWorkOrderCommand command);

    void cancel(CancelWorkOrderCommand command);

    void settle(SettleWorkOrderCommand command);

    void adjustChargeItems(AdjustChargeItemsCommand command);

    void moveToAccepted(Long workOrderId, Long operatorId);

    void markPartOrdered(Long workOrderId, Long operatorId);

    void markPartArrived(Long workOrderId, Long operatorId);
}
