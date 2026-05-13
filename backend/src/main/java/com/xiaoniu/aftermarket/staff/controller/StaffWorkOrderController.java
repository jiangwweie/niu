package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderChargeItemResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderDetailResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryRequest;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff/work-orders")
public class StaffWorkOrderController {

    private final WorkOrderService workOrderService;

    public StaffWorkOrderController(WorkOrderService workOrderService) {
        this.workOrderService = workOrderService;
    }

    @GetMapping
    public ApiResponse<PageResponse<StaffWorkOrderListItem>> listWorkOrders(
            @RequestParam(required = false) String workOrderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String vehicleFrameNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        WorkOrderQueryRequest request = new WorkOrderQueryRequest();
        request.setStoreId(user.storeId());
        request.setWorkOrderNo(workOrderNo);
        request.setCustomerName(customerName);
        request.setCustomerPhone(customerPhone);
        request.setVehicleFrameNo(vehicleFrameNo);
        request.setStatus(status);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        PageResponse<WorkOrderQueryResponse> result = workOrderService.pageQuery(request);
        return ApiResponse.success(result.map(StaffWorkOrderListItem::from));
    }

    @GetMapping("/{workOrderId}")
    public ApiResponse<StaffWorkOrderDetail> getWorkOrder(@PathVariable Long workOrderId) {
        CurrentUser user = requireCurrentUser();
        WorkOrderDetailResponse detail = workOrderService.getById(workOrderId);
        if (!user.storeId().equals(detail.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND, "工单不存在");
        }
        return ApiResponse.success(StaffWorkOrderDetail.from(detail));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
