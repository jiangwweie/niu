package com.xiaoniu.aftermarket.payment.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.payment.application.RecordRefundApplicationService;
import com.xiaoniu.aftermarket.payment.controller.dto.RecordRefundRequest;
import com.xiaoniu.aftermarket.payment.dto.RecordRefundCommand;
import com.xiaoniu.aftermarket.payment.dto.RefundQueryRequest;
import com.xiaoniu.aftermarket.payment.dto.RefundQueryResponse;
import com.xiaoniu.aftermarket.payment.dto.RefundRecordResponse;
import com.xiaoniu.aftermarket.payment.service.RefundService;
import com.xiaoniu.aftermarket.workorder.entity.WorkOrderEntity;
import com.xiaoniu.aftermarket.workorder.mapper.WorkOrderMapper;
import com.xiaoniu.aftermarket.common.util.DateParamParser;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class RefundController {

    private final RefundService refundService;
    private final RecordRefundApplicationService recordRefundService;
    private final WorkOrderMapper workOrderMapper;

    public RefundController(RefundService refundService,
                            RecordRefundApplicationService recordRefundService,
                            WorkOrderMapper workOrderMapper) {
        this.refundService = refundService;
        this.recordRefundService = recordRefundService;
        this.workOrderMapper = workOrderMapper;
    }

    // ========== Work-order scoped ==========

    @PreAuthorize("hasAnyAuthority('FINANCE_VIEW','REFUND_RECORD')")
    @GetMapping("/api/admin/work-orders/{workOrderId}/refunds")
    public ApiResponse<java.util.List<RefundRecordResponse>> listRefundsByWorkOrder(
            @PathVariable Long workOrderId) {
        CurrentUser user = requireCurrentUser();
        requireWorkOrderInStore(workOrderId, user.storeId());
        return ApiResponse.success(refundService.listByWorkOrderId(workOrderId));
    }

    @PostMapping("/api/admin/work-orders/{workOrderId}/refunds")
    @PreAuthorize("hasAuthority('REFUND_RECORD')")
    public ApiResponse<Long> recordRefund(
            @PathVariable Long workOrderId,
            @Valid @RequestBody RecordRefundRequest request) {
        CurrentUser user = requireCurrentUser();
        WorkOrderEntity workOrder = requireWorkOrderInStore(workOrderId, user.storeId());
        boolean delivered = "DELIVERED".equals(workOrder.getStatus());
        if (delivered && !user.permissions().contains("REFUND_AFTER_DELIVERY")) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "缺少交付后退款权限");
        }
        RecordRefundCommand command = new RecordRefundCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setAmount(request.getAmount());
        command.setRefundMethod(request.getRefundMethod());
        command.setRefundedAt(request.getRefundedAt());
        command.setAllowDeliveredAfterRefund(delivered);
        command.setReason(request.getReason());
        command.setRemark(request.getRemark());
        Long refundId = recordRefundService.execute(command);
        return ApiResponse.success(refundId);
    }

    // ========== Global page query ==========

    @PreAuthorize("hasAnyAuthority('FINANCE_VIEW','REFUND_RECORD')")
    @GetMapping("/api/admin/refunds")
    public ApiResponse<PageResponse<RefundQueryResponse>> listRefunds(
            @RequestParam(required = false) String workOrderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String refundMethod,
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
        RefundQueryRequest request = new RefundQueryRequest();
        request.setStoreId(user.storeId());
        request.setWorkOrderNo(workOrderNo);
        request.setCustomerName(customerName);
        request.setRefundMethod(refundMethod);
        request.setStartTime(parsedStart);
        request.setEndTime(parsedEnd);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(refundService.pageQuery(request));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }

    private WorkOrderEntity requireWorkOrderInStore(Long workOrderId, Long storeId) {
        WorkOrderEntity workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null || !storeId.equals(workOrder.getStoreId())) {
            throw new BusinessException(ErrorCode.WORK_ORDER_NOT_FOUND, "工单不存在");
        }
        return workOrder;
    }
}
