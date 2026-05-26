package com.xiaoniu.aftermarket.staff.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.payment.application.RecordPaymentApplicationService;
import com.xiaoniu.aftermarket.payment.application.RecordRefundApplicationService;
import com.xiaoniu.aftermarket.payment.dto.PaymentRecordResponse;
import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.dto.RefundRecordResponse;
import com.xiaoniu.aftermarket.payment.service.PaymentService;
import com.xiaoniu.aftermarket.payment.service.RefundService;
import com.xiaoniu.aftermarket.staff.dto.StaffAddChargeItemRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffCancelWorkOrderRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffAddNonInventoryChargeRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffChargeItemIdResponse;
import com.xiaoniu.aftermarket.staff.dto.StaffCreateDraftWorkOrderRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffDeliverWorkOrderRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffMarkRepairDoneRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffRecordPaymentRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffRecordRefundRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffSettleWorkOrderRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffSubmitWorkOrderRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffUpdateChargeItemRequest;
import com.xiaoniu.aftermarket.staff.dto.StaffUpdateDraftWorkOrderRequest;
import com.xiaoniu.aftermarket.workorder.application.SettleWorkOrderApplicationService;
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
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderDraftReferenceResolver;
import com.xiaoniu.aftermarket.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/work-orders")
public class StaffWorkOrderController {

    private final WorkOrderService workOrderService;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderDraftReferenceResolver draftReferenceResolver;
    private final RecordPaymentApplicationService recordPaymentService;
    private final PaymentService paymentService;
    private final RecordRefundApplicationService recordRefundService;
    private final RefundService refundService;
    private final SettleWorkOrderApplicationService settleService;

    public StaffWorkOrderController(WorkOrderService workOrderService,
                                    WorkOrderMapper workOrderMapper,
                                    WorkOrderDraftReferenceResolver draftReferenceResolver,
                                    RecordPaymentApplicationService recordPaymentService,
                                    PaymentService paymentService,
                                    RecordRefundApplicationService recordRefundService,
                                    RefundService refundService,
                                    SettleWorkOrderApplicationService settleService) {
        this.workOrderService = workOrderService;
        this.workOrderMapper = workOrderMapper;
        this.draftReferenceResolver = draftReferenceResolver;
        this.recordPaymentService = recordPaymentService;
        this.paymentService = paymentService;
        this.recordRefundService = recordRefundService;
        this.refundService = refundService;
        this.settleService = settleService;
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
        requireWorkOrderInStore(workOrderId, user.storeId());
        WorkOrderDetailResponse detail = workOrderService.getById(workOrderId);
        return ApiResponse.success(StaffWorkOrderDetail.from(detail));
    }

    @PreAuthorize("hasAuthority('WORK_ORDER_CREATE')")
    @PostMapping("/drafts")
    public ApiResponse<StaffWorkOrderDetail> createDraft(
            @Valid @RequestBody StaffCreateDraftWorkOrderRequest request) {
        CurrentUser user = requireCurrentUser();

        CreateDraftWorkOrderCommand command = new CreateDraftWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setOperatorId(user.userId());
        command.setCustomerNameSnapshot(request.customerNameSnapshot());
        command.setCustomerPhoneSnapshot(request.customerPhoneSnapshot());
        command.setVehicleModelSnapshot(request.vehicleModelSnapshot());
        command.setFrameNoSnapshot(request.frameNoSnapshot());
        command.setBatteryNoSnapshot(request.batteryNoSnapshot());
        command.setRepairItem(request.repairItem());
        command.setRemark(request.remark());

        command.setCustomerId(request.customerId());
        command.setVehicleId(request.vehicleId());
        draftReferenceResolver.resolve(command);
        if (!StringUtils.hasText(command.getCustomerNameSnapshot())) {
            throw new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "customerNameSnapshot不能为空");
        }

        Long workOrderId = workOrderService.createDraft(command);
        WorkOrderDetailResponse detail = workOrderService.getById(workOrderId);
        return ApiResponse.success(StaffWorkOrderDetail.from(detail));
    }

    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
    @PutMapping("/{workOrderId}/draft")
    public ApiResponse<StaffWorkOrderDetail> updateDraft(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffUpdateDraftWorkOrderRequest request) {
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
        WorkOrderDetailResponse detail = workOrderService.getById(workOrderId);
        return ApiResponse.success(StaffWorkOrderDetail.from(detail));
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
    public ApiResponse<StaffChargeItemIdResponse> addChargeItem(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffAddChargeItemRequest request) {
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
        return ApiResponse.success(new StaffChargeItemIdResponse(chargeItemId));
    }

    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
    @PutMapping("/{workOrderId}/charge-items/{chargeItemId}")
    public ApiResponse<Void> updateChargeItem(
            @PathVariable Long workOrderId,
            @PathVariable Long chargeItemId,
            @Valid @RequestBody StaffUpdateChargeItemRequest request) {
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
    public ApiResponse<Void> deleteChargeItem(
            @PathVariable Long workOrderId,
            @PathVariable Long chargeItemId) {
        CurrentUser user = requireCurrentUser();
        workOrderService.removeChargeItem(user.storeId(), workOrderId, chargeItemId);
        return ApiResponse.success(null);
    }

    // 状态变更走独立接口，确保每次变更都有完整的业务校验（如库存预占/释放、金额校验）

    @PostMapping("/{workOrderId}/submit")
    @PreAuthorize("hasAuthority('WORK_ORDER_SUBMIT')")
    public ApiResponse<StaffWorkOrderDetail> submit(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffSubmitWorkOrderRequest request) {
        CurrentUser user = requireCurrentUser();

        SubmitWorkOrderCommand command = new SubmitWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setRemark(request.remark());

        workOrderService.submit(command);
        WorkOrderDetailResponse detail = workOrderService.getById(workOrderId);
        return ApiResponse.success(StaffWorkOrderDetail.from(detail));
    }

    @PostMapping("/{workOrderId}/cancel")
    @PreAuthorize("hasAuthority('WORK_ORDER_CANCEL')")
    public ApiResponse<StaffWorkOrderDetail> cancel(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffCancelWorkOrderRequest request) {
        CurrentUser user = requireCurrentUser();

        CancelWorkOrderCommand command = new CancelWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setReason(request.reason());

        workOrderService.cancel(command);
        WorkOrderDetailResponse detail = workOrderService.getById(workOrderId);
        return ApiResponse.success(StaffWorkOrderDetail.from(detail));
    }

    @PostMapping("/{workOrderId}/mark-repair-done")
    @PreAuthorize("hasAuthority('WORK_ORDER_SETTLE')")
    public ApiResponse<StaffWorkOrderDetail> markRepairDone(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffMarkRepairDoneRequest request) {
        CurrentUser user = requireCurrentUser();
        MarkRepairDoneWorkOrderCommand command = new MarkRepairDoneWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setNoChargeReason(request.noChargeReason());
        command.setNoChargeRemark(request.noChargeRemark());
        command.setRemark(request.remark());
        workOrderService.markRepairDone(command);
        return ApiResponse.success(StaffWorkOrderDetail.from(workOrderService.getById(workOrderId)));
    }

    @PostMapping("/{workOrderId}/deliver")
    @PreAuthorize("hasAuthority('WORK_ORDER_SETTLE')")
    public ApiResponse<StaffWorkOrderDetail> deliver(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffDeliverWorkOrderRequest request) {
        CurrentUser user = requireCurrentUser();
        DeliverWorkOrderCommand command = new DeliverWorkOrderCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setNoChargeReason(request.noChargeReason());
        command.setNoChargeRemark(request.noChargeRemark());
        command.setRemark(request.remark());
        workOrderService.deliver(command);
        return ApiResponse.success(StaffWorkOrderDetail.from(workOrderService.getById(workOrderId)));
    }

    @PostMapping("/{workOrderId}/non-inventory-charges")
    @PreAuthorize("hasAuthority('WORK_ORDER_UPDATE')")
    public ApiResponse<StaffChargeItemIdResponse> addNonInventoryCharge(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffAddNonInventoryChargeRequest request) {
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
        return ApiResponse.success(new StaffChargeItemIdResponse(workOrderService.addNonInventoryCharge(command)));
    }

    @PostMapping("/{workOrderId}/payments")
    @PreAuthorize("hasAuthority('PAYMENT_RECORD')")
    public ApiResponse<StaffPaymentRecordResponse> recordPayment(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffRecordPaymentRequest request) {
        CurrentUser user = requireCurrentUser();
        requireWorkOrderInStore(workOrderId, user.storeId());

        RecordPaymentCommand command = new RecordPaymentCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setReceiverId(user.userId());
        command.setAmount(request.amount());
        command.setPaymentMethod(request.paymentMethod());
        command.setPaidAt(request.paidAt());
        command.setRemark(request.remark());

        Long paymentId = recordPaymentService.execute(command);
        PaymentRecordResponse response = paymentService.listByWorkOrderId(workOrderId).stream()
                .filter(record -> paymentId.equals(record.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "支付记录不存在"));
        return ApiResponse.success(StaffPaymentRecordResponse.from(response));
    }

    @PostMapping("/{workOrderId}/refunds")
    @PreAuthorize("hasAuthority('REFUND_RECORD')")
    public ApiResponse<StaffRefundRecordResponse> recordRefund(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffRecordRefundRequest request) {
        CurrentUser user = requireCurrentUser();
        requireWorkOrderInStore(workOrderId, user.storeId());

        RecordRefundCommand command = new RecordRefundCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setAmount(request.amount());
        command.setRefundMethod(request.refundMethod());
        command.setRefundedAt(request.refundedAt());
        command.setReason(request.reason());
        command.setRemark(request.remark());

        Long refundId = recordRefundService.execute(command);
        RefundRecordResponse response = refundService.listByWorkOrderId(workOrderId).stream()
                .filter(record -> refundId.equals(record.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "退款记录不存在"));
        return ApiResponse.success(StaffRefundRecordResponse.from(response));
    }

    @PostMapping("/{workOrderId}/settle")
    @PreAuthorize("hasAuthority('WORK_ORDER_SETTLE')")
    public ApiResponse<StaffSettledWorkOrderResponse> settle(
            @PathVariable Long workOrderId,
            @Valid @RequestBody StaffSettleWorkOrderRequest request) {
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

    private void requireWorkOrderInStore(Long workOrderId, Long storeId) {
        WorkOrderEntity entity = workOrderMapper.selectById(workOrderId);
        if (entity == null || entity.getDeleted() != null && entity.getDeleted() == 1
                || !storeId.equals(entity.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND, "工单不存在");
        }
    }
}
