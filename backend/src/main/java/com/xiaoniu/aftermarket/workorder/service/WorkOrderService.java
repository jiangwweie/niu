package com.xiaoniu.aftermarket.workorder.service;

import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.workorder.dto.AdjustChargeItemsCommand;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SettleWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderDraftCommand;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderDetailResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryRequest;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;

public interface WorkOrderService {

    Long createDraft(CreateDraftWorkOrderCommand command);

    WorkOrderEntity updateDraft(Long workOrderId, UpdateWorkOrderDraftCommand command);

    WorkOrderDetailResponse getById(Long workOrderId);

    WorkOrderEntity getByWorkOrderNo(String workOrderNo);

    PageResponse<WorkOrderQueryResponse> pageQuery(WorkOrderQueryRequest request);

    Long addChargeItem(Long workOrderId, AddWorkOrderChargeItemCommand command);

    void updateChargeItem(Long workOrderId, Long chargeItemId,
                          UpdateWorkOrderChargeItemCommand command);

    void removeChargeItem(Long workOrderId, Long chargeItemId);

    void recalculateReceivableAmount(Long workOrderId);

    void submit(SubmitWorkOrderCommand command);

    void cancel(CancelWorkOrderCommand command);

    void settle(SettleWorkOrderCommand command);

    void adjustChargeItems(AdjustChargeItemsCommand command);

    void moveToAccepted(Long workOrderId, Long operatorId);

    void markPartOrdered(Long workOrderId, Long operatorId);

    void markPartArrived(Long workOrderId, Long operatorId);
}
