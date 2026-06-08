package com.xiaoniu.aftermarket.workorder.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.util.DateParamParser;
import com.xiaoniu.aftermarket.workorder.application.CancelWorkOrderApplicationService;
import com.xiaoniu.aftermarket.workorder.application.DeliverWorkOrderApplicationService;
import com.xiaoniu.aftermarket.workorder.application.MarkRepairDoneWorkOrderApplicationService;
import com.xiaoniu.aftermarket.workorder.application.SettleWorkOrderApplicationService;
import com.xiaoniu.aftermarket.workorder.application.SubmitWorkOrderApplicationService;
import com.xiaoniu.aftermarket.workorder.dto.AddWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.AddNonInventoryChargeCommand;
import com.xiaoniu.aftermarket.workorder.dto.CancelWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.CreateDraftWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.DeliverWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.MarkRepairDoneWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SettleWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.SubmitWorkOrderCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderChargeItemCommand;
import com.xiaoniu.aftermarket.workorder.dto.UpdateWorkOrderDraftCommand;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderDetailResponse;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryRequest;
import com.xiaoniu.aftermarket.workorder.dto.WorkOrderQueryResponse;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderDraftReferenceResolver;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/work-orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderDraftReferenceResolver draftReferenceResolver;
    private final SubmitWorkOrderApplicationService submitService;
    private final CancelWorkOrderApplicationService cancelService;
    private final SettleWorkOrderApplicationService settleService;
    private final MarkRepairDoneWorkOrderApplicationService markRepairDoneService;
    private final DeliverWorkOrderApplicationService deliverService;

    public WorkOrderController(WorkOrderService workOrderService,
                               WorkOrderDraftReferenceResolver draftReferenceResolver,
                               SubmitWorkOrderApplicationService submitService,
                               CancelWorkOrderApplicationService cancelService,
                               SettleWorkOrderApplicationService settleService,
                               MarkRepairDoneWorkOrderApplicationService markRepairDoneService,
                               DeliverWorkOrderApplicationService deliverService) {
        this.workOrderService = workOrderService;
        this.draftReferenceResolver = draftReferenceResolver;
        this.submitService = submitService;
        this.cancelService = cancelService;
        this.settleService = settleService;
        this.markRepairDoneService = markRepairDoneService;
        this.deliverService = deliverService;
    }

    @GetMapping
    public ApiResponse<PageResponse<WorkOrderQueryResponse>> listWorkOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String workOrderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String vehicleFrameNo,
            @RequestParam(required = false) String scooterModel,
            @RequestParam(required = false) Long partId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean officialOnly,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        CurrentUser user = requireCurrentUser();
        LocalDateTime parsedStart;
        LocalDateTime parsedEnd;
        try {
            parsedStart = DateParamParser.parseStartDateTime(startTime);
            parsedEnd = DateParamParser.parseEndDateTime(endTime);
        } catch (IllegalArgumentException e) {
            return ApiResponse.failure(ErrorCode.COMMON_BAD_REQUEST, e.getMessage());
        }
        WorkOrderQueryRequest request = new WorkOrderQueryRequest();
        request.setStoreId(user.storeId());
        request.setKeyword(keyword);
        request.setWorkOrderNo(workOrderNo);
        request.setCustomerName(customerName);
        request.setCustomerPhone(customerPhone);
        request.setVehicleFrameNo(vehicleFrameNo);
        request.setScooterModel(scooterModel);
        request.setPartId(partId);
        request.setStatus(status);
        request.setOfficialOnly(officialOnly);
        request.setStartTime(parsedStart);
        request.setEndTime(parsedEnd);
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

    @PreAuthorize("hasAuthority('WORK_ORDER_CREATE')")
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
        draftReferenceResolver.resolve(command);
        Long workOrderId = workOrderService.createDraft(command);
        return ApiResponse.success(workOrderId);
    }

    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
    @PutMapping("/{workOrderId}/draft")
    public ApiResponse<Void> updateDraft(@PathVariable Long workOrderId,
                                         @Valid @RequestBody UpdateDraftRequest request) {
        CurrentUser user = requireCurrentUser();
        UpdateWorkOrderDraftCommand command = new UpdateWorkOrderDraftCommand();
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
        draftReferenceResolver.resolve(command);
        workOrderService.updateDraft(workOrderId, command);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
    @DeleteMapping("/{workOrderId}")
    public ApiResponse<Void> deleteDraft(@PathVariable Long workOrderId) {
        CurrentUser user = requireCurrentUser();
        workOrderService.deleteDraft(user.storeId(), workOrderId, user.userId());
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
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
        command.setBarcode(request.barcode());
        command.setCode(request.code());
        command.setQuantity(request.quantity());
        command.setUnit(request.unit());
        command.setUnitPrice(request.unitPrice());
        command.setRemark(request.remark());
        Long chargeItemId = workOrderService.addChargeItem(workOrderId, command);
        return ApiResponse.success(new ChargeItemIdResponse(chargeItemId));
    }

    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
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

    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
    @DeleteMapping("/{workOrderId}/charge-items/{chargeItemId}")
    public ApiResponse<Void> deleteChargeItem(@PathVariable Long workOrderId,
                                               @PathVariable Long chargeItemId) {
        CurrentUser user = requireCurrentUser();
        workOrderService.removeChargeItem(user.storeId(), workOrderId, chargeItemId);
        return ApiResponse.success(null);
    }

    // 工单状态流转必须通过独立动作接口（submit/cancel/settle），不能直接修改 status 字段
    // 这样确保每次状态变更都有完整的业务校验和副作用（如库存操作）

    @PostMapping("/{workOrderId}/submit")
    @PreAuthorize("hasAuthority('WORK_ORDER_SUBMIT')")
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
    @PreAuthorize("hasAuthority('WORK_ORDER_CANCEL')")
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

    @PostMapping("/{workOrderId}/mark-repair-done")
    @PreAuthorize("hasAuthority('WORK_ORDER_SETTLE')")
    public ApiResponse<Void> markRepairDone(@PathVariable Long workOrderId,
                                            @Valid @RequestBody MarkRepairDoneRequest request) {
        CurrentUser user = requireCurrentUser();
        MarkRepairDoneWorkOrderCommand command = new MarkRepairDoneWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setNoChargeReason(request.noChargeReason());
        command.setNoChargeRemark(request.noChargeRemark());
        command.setRemark(request.remark());
        markRepairDoneService.execute(command);
        return ApiResponse.success(null);
    }

    @PostMapping("/{workOrderId}/deliver")
    @PreAuthorize("hasAuthority('WORK_ORDER_SETTLE')")
    public ApiResponse<Void> deliver(@PathVariable Long workOrderId,
                                     @Valid @RequestBody DeliverRequest request) {
        CurrentUser user = requireCurrentUser();
        DeliverWorkOrderCommand command = new DeliverWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setNoChargeReason(request.noChargeReason());
        command.setNoChargeRemark(request.noChargeRemark());
        command.setRemark(request.remark());
        deliverService.execute(command);
        return ApiResponse.success(null);
    }

    @PostMapping("/{workOrderId}/non-inventory-charges")
    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
    public ApiResponse<ChargeItemIdResponse> addNonInventoryCharge(
            @PathVariable Long workOrderId,
            @Valid @RequestBody AddNonInventoryChargeRequest request) {
        CurrentUser user = requireCurrentUser();
        AddNonInventoryChargeCommand command = new AddNonInventoryChargeCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setChargeType(request.chargeType());
        command.setItemName(request.itemName());
        command.setQuantity(request.quantity());
        command.setUnit(request.unit());
        command.setUnitPrice(request.unitPrice());
        command.setReason(request.reason());
        command.setRemark(request.remark());
        return ApiResponse.success(new ChargeItemIdResponse(workOrderService.addNonInventoryCharge(command)));
    }

    @PostMapping("/{workOrderId}/settle")
    @PreAuthorize("hasAuthority('WORK_ORDER_SETTLE')")
    public ApiResponse<Void> settle(@PathVariable Long workOrderId,
                                    @Valid @RequestBody SettleRequest request) {
        CurrentUser user = requireCurrentUser();
        SettleWorkOrderCommand command = new SettleWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setRemark(request.remark());
        throw new BusinessException(ErrorCode.WORK_ORDER_LEGACY_SETTLE_DISABLED);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
