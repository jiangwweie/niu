package com.xiaoniu.aftermarket.workorder.service.impl;

import com.xiaoniu.aftermarket.workorder.dto.AdjustChargeItemsCommand;
import com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SettleWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class WorkOrderServiceImpl implements WorkOrderService {

    @Override
    public Long createDraft(CreateDraftWorkOrderCommand command) {
        throw new UnsupportedOperationException("TODO: implement draft creation in Phase 3");
    }

    @Override
    public WorkOrderEntity updateDraft(Long workOrderId, CreateDraftWorkOrderCommand command) {
        throw new UnsupportedOperationException("TODO: implement draft update in Phase 3");
    }

    @Override
    public void submit(SubmitWorkOrderCommand command) {
        throw new UnsupportedOperationException("TODO: implement submit flow in Phase 3");
    }

    @Override
    public void cancel(CancelWorkOrderCommand command) {
        throw new UnsupportedOperationException("TODO: implement cancel flow in Phase 3");
    }

    @Override
    public void settle(SettleWorkOrderCommand command) {
        throw new UnsupportedOperationException("TODO: implement settlement flow in Phase 4");
    }

    @Override
    public void adjustChargeItems(AdjustChargeItemsCommand command) {
        throw new UnsupportedOperationException("TODO: implement charge item adjustment in Phase 3/4");
    }

    @Override
    public void moveToAccepted(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement accepted transition in a later phase");
    }

    @Override
    public void markPartOrdered(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement part-ordered transition in a later phase");
    }

    @Override
    public void markPartArrived(Long workOrderId, Long operatorId) {
        throw new UnsupportedOperationException("TODO: implement part-arrived transition in a later phase");
    }
}
