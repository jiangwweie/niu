package com.xiaoniu.aftermarket.workorder.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.workorder.application.CancelWorkOrderApplicationService;
import com.xiaoniu.aftermarket.workorder.application.SettleWorkOrderApplicationService;
import com.xiaoniu.aftermarket.workorder.application.SubmitWorkOrderApplicationService;
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
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final SubmitWorkOrderApplicationService submitService;
    private final CancelWorkOrderApplicationService cancelService;
    private final SettleWorkOrderApplicationService settleService;

    public WorkOrderController(WorkOrderService workOrderService,
                               SubmitWorkOrderApplicationService submitService,
                               CancelWorkOrderApplicationService cancelService,
                               SettleWorkOrderApplicationService settleService) {
        this.workOrderService = workOrderService;
        this.submitService = submitService;
        this.cancelService = cancelService;
        this.settleService = settleService;
    }

    @GetMapping
    public ApiResponse<PageResponse<WorkOrderQueryResponse>> listWorkOrders(
            @RequestParam(required = false) String workOrderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String vehicleFrameNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean officialOnly,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
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
        request.setOfficialOnly(officialOnly);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(workOrderService.pageQuery(request));
    }

    @GetMapping("/{workOrderId}")
    public ApiResponse<WorkOrderDetailResponse> getWorkOrder(@PathVariable Long workOrderId) {
        CurrentUser user = requireCurrentUser();
        WorkOrderDetailResponse detail = workOrderService.getById(workOrderId);
        if (!user.storeId().equals(detail.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND, "工单不存在");
        }
        return ApiResponse.success(detail);
    }

    @PostMapping("/drafts")
    public ApiResponse<Long> createDraft(@Valid @RequestBody CreateDraftRequest request) {
        CurrentUser user = requireCurrentUser();
        CreateDraftWorkOrderCommand command = new CreateDraftWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setCustomerId(request.customerId());
        command.setVehicleId(request.vehicleId());
        command.setCustomerNameSnapshot(request.customerNameSnapshot());
        command.setCustomerPhoneSnapshot(request.customerPhoneSnapshot());
        command.setVehicleModelSnapshot(request.vehicleModelSnapshot());
        command.setFrameNoSnapshot(request.frameNoSnapshot());
        command.setBatteryNoSnapshot(request.batteryNoSnapshot());
        command.setRepairItem(request.repairItem());
        command.setRemark(request.remark());
        command.setChargeItems(request.chargeItems());
        Long workOrderId = workOrderService.createDraft(command);
        return ApiResponse.success(workOrderId);
    }

    @PutMapping("/{workOrderId}/draft")
    public ApiResponse<Void> updateDraft(@PathVariable Long workOrderId,
                                         @Valid @RequestBody UpdateDraftRequest request) {
        CurrentUser user = requireCurrentUser();
        UpdateWorkOrderDraftCommand command = new UpdateWorkOrderDraftCommand();
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setCustomerNameSnapshot(request.customerNameSnapshot());
        command.setCustomerPhoneSnapshot(request.customerPhoneSnapshot());
        command.setVehicleModelSnapshot(request.vehicleModelSnapshot());
        command.setFrameNoSnapshot(request.frameNoSnapshot());
        command.setBatteryNoSnapshot(request.batteryNoSnapshot());
        command.setRepairItem(request.repairItem());
        command.setRemark(request.remark());
        workOrderService.updateDraft(workOrderId, command);
        return ApiResponse.success(null);
    }

    @PostMapping("/{workOrderId}/charge-items")
    public ApiResponse<ChargeItemIdResponse> addChargeItem(
            @PathVariable Long workOrderId,
            @Valid @RequestBody AddChargeItemRequest request) {
        CurrentUser user = requireCurrentUser();
        AddWorkOrderChargeItemCommand command = new AddWorkOrderChargeItemCommand();
        command.setStoreId(user.storeId());
        command.setChargeType(request.chargeType());
        command.setItemName(request.itemName());
        command.setPartId(request.partId());
        command.setQuantity(request.quantity());
        command.setUnit(request.unit());
        command.setUnitPrice(request.unitPrice());
        command.setRemark(request.remark());
        Long chargeItemId = workOrderService.addChargeItem(workOrderId, command);
        return ApiResponse.success(new ChargeItemIdResponse(chargeItemId));
    }

    @PutMapping("/{workOrderId}/charge-items/{chargeItemId}")
    public ApiResponse<Void> updateChargeItem(
            @PathVariable Long workOrderId,
            @PathVariable Long chargeItemId,
            @Valid @RequestBody UpdateChargeItemRequest request) {
        CurrentUser user = requireCurrentUser();
        UpdateWorkOrderChargeItemCommand command = new UpdateWorkOrderChargeItemCommand();
        command.setStoreId(user.storeId());
        command.setItemName(request.itemName());
        command.setQuantity(request.quantity());
        command.setUnit(request.unit());
        command.setUnitPrice(request.unitPrice());
        command.setRemark(request.remark());
        workOrderService.updateChargeItem(workOrderId, chargeItemId, command);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{workOrderId}/charge-items/{chargeItemId}")
    public ApiResponse<Void> deleteChargeItem(@PathVariable Long workOrderId,
                                               @PathVariable Long chargeItemId) {
        CurrentUser user = requireCurrentUser();
        workOrderService.removeChargeItem(user.storeId(), workOrderId, chargeItemId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{workOrderId}/submit")
    public ApiResponse<Void> submit(@PathVariable Long workOrderId,
                                    @Valid @RequestBody SubmitRequest request) {
        CurrentUser user = requireCurrentUser();
        SubmitWorkOrderCommand command = new SubmitWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setRemark(request.remark());
        submitService.execute(command);
        return ApiResponse.success(null);
    }

    @PostMapping("/{workOrderId}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long workOrderId,
                                    @Valid @RequestBody CancelRequest request) {
        CurrentUser user = requireCurrentUser();
        CancelWorkOrderCommand command = new CancelWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setReason(request.reason());
        cancelService.execute(command);
        return ApiResponse.success(null);
    }

    @PostMapping("/{workOrderId}/settle")
    public ApiResponse<Void> settle(@PathVariable Long workOrderId,
                                    @Valid @RequestBody SettleRequest request) {
        CurrentUser user = requireCurrentUser();
        SettleWorkOrderCommand command = new SettleWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setRemark(request.remark());
        settleService.execute(command);
        return ApiResponse.success(null);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
