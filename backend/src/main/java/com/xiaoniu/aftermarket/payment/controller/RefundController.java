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
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
public class RefundController {

    private final RefundService refundService;
    private final RecordRefundApplicationService recordRefundService;

    public RefundController(RefundService refundService,
                            RecordRefundApplicationService recordRefundService) {
        this.refundService = refundService;
        this.recordRefundService = recordRefundService;
    }

    // ========== Work-order scoped ==========

    @GetMapping("/api/admin/work-orders/{workOrderId}/refunds")
    public ApiResponse<java.util.List<RefundRecordResponse>> listRefundsByWorkOrder(
            @PathVariable Long workOrderId) {
        requireCurrentUser();
        return ApiResponse.success(refundService.listByWorkOrderId(workOrderId));
    }

    @PostMapping("/api/admin/work-orders/{workOrderId}/refunds")
    public ApiResponse<Long> recordRefund(
            @PathVariable Long workOrderId,
            @Valid @RequestBody RecordRefundRequest request) {
        CurrentUser user = requireCurrentUser();
        RecordRefundCommand command = new RecordRefundCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setAmount(request.getAmount());
        command.setRefundMethod(request.getRefundMethod());
        command.setRefundedAt(request.getRefundedAt());
        command.setReason(request.getReason());
        command.setRemark(request.getRemark());
        Long refundId = recordRefundService.execute(command);
        return ApiResponse.success(refundId);
    }

    // ========== Global page query ==========

    @GetMapping("/api/admin/refunds")
    public ApiResponse<PageResponse<RefundQueryResponse>> listRefunds(
            @RequestParam(required = false) String workOrderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String refundMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        CurrentUser user = requireCurrentUser();
        RefundQueryRequest request = new RefundQueryRequest();
        request.setStoreId(user.storeId());
        request.setWorkOrderNo(workOrderNo);
        request.setCustomerName(customerName);
        request.setRefundMethod(refundMethod);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(refundService.pageQuery(request));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
