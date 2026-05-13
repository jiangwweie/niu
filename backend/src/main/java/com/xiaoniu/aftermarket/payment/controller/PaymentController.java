package com.xiaoniu.aftermarket.payment.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.payment.application.RecordPaymentApplicationService;
import com.xiaoniu.aftermarket.payment.controller.dto.RecordPaymentRequest;
import com.xiaoniu.aftermarket.payment.dto.PaymentQueryRequest;
import com.xiaoniu.aftermarket.payment.dto.PaymentQueryResponse;
import com.xiaoniu.aftermarket.payment.dto.PaymentRecordResponse;
import com.xiaoniu.aftermarket.payment.dto.PaymentSummaryResponse;
import com.xiaoniu.aftermarket.payment.dto.RecordPaymentCommand;
import com.xiaoniu.aftermarket.payment.service.PaymentService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private final RecordPaymentApplicationService recordPaymentService;

    public PaymentController(PaymentService paymentService,
                             RecordPaymentApplicationService recordPaymentService) {
        this.paymentService = paymentService;
        this.recordPaymentService = recordPaymentService;
    }

    // ========== Work-order scoped ==========

    @GetMapping("/api/admin/work-orders/{workOrderId}/payments")
    public ApiResponse<java.util.List<PaymentRecordResponse>> listPaymentsByWorkOrder(
            @PathVariable Long workOrderId) {
        requireCurrentUser();
        return ApiResponse.success(paymentService.listByWorkOrderId(workOrderId));
    }

    @PostMapping("/api/admin/work-orders/{workOrderId}/payments")
    public ApiResponse<Long> recordPayment(
            @PathVariable Long workOrderId,
            @Valid @RequestBody RecordPaymentRequest request) {
        CurrentUser user = requireCurrentUser();
        RecordPaymentCommand command = new RecordPaymentCommand();
        command.setStoreId(user.storeId());
        command.setWorkOrderId(workOrderId);
        command.setOperatorId(user.userId());
        command.setAmount(request.getAmount());
        command.setPaymentMethod(request.getPaymentMethod());
        command.setPaidAt(request.getPaidAt());
        command.setReceiverId(request.getReceiverId());
        command.setRemark(request.getRemark());
        Long paymentId = recordPaymentService.execute(command);
        return ApiResponse.success(paymentId);
    }

    @GetMapping("/api/admin/work-orders/{workOrderId}/payment-summary")
    public ApiResponse<PaymentSummaryResponse> getPaymentSummary(
            @PathVariable Long workOrderId) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(paymentService.getPaymentSummary(user.storeId(), workOrderId));
    }

    // ========== Global page query ==========

    @GetMapping("/api/admin/payments")
    public ApiResponse<PageResponse<PaymentQueryResponse>> listPayments(
            @RequestParam(required = false) String workOrderNo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        CurrentUser user = requireCurrentUser();
        PaymentQueryRequest request = new PaymentQueryRequest();
        request.setStoreId(user.storeId());
        request.setWorkOrderNo(workOrderNo);
        request.setCustomerName(customerName);
        request.setPaymentMethod(paymentMethod);
        request.setStartTime(startTime);
        request.setEndTime(endTime);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(paymentService.pageQuery(request));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
