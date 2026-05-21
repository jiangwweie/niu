package com.xiaoniu.aftermarket.finance.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.finance.dto.CashierReportResponse;
import com.xiaoniu.aftermarket.finance.dto.FinanceReportResponse;
import com.xiaoniu.aftermarket.finance.service.FinanceService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    /**
     * 财务报表需按门店隔离，所有查询基于 user.storeId()
     * 权限控制：FINANCE_VIEW
     */
    @GetMapping("/daily")
    @PreAuthorize("hasAuthority('FINANCE_VIEW')")
    public ApiResponse<FinanceReportResponse> queryDaily(
            @RequestParam(required = false) String date) {
        CurrentUser user = requireCurrentUser();
        LocalDate queryDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.success(financeService.queryDaily(user.storeId(), queryDate));
    }

    @GetMapping("/monthly")
    @PreAuthorize("hasAuthority('FINANCE_VIEW')")
    public ApiResponse<FinanceReportResponse> queryMonthly(
            @RequestParam int year,
            @RequestParam int month) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(financeService.queryMonthly(user.storeId(), year, month));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAuthority('FINANCE_VIEW')")
    public ApiResponse<FinanceReportResponse> queryRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        CurrentUser user = requireCurrentUser();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        // 日期范围校验：开始日期不能晚于结束日期
        if (start.isAfter(end)) {
            return ApiResponse.failure(ErrorCode.COMMON_BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        return ApiResponse.success(financeService.queryRange(user.storeId(), start, end));
    }

    @GetMapping("/cashier-report")
    @PreAuthorize("hasAuthority('FINANCE_VIEW')")
    public ApiResponse<CashierReportResponse> cashierReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long storeId = CurrentUserContext.requireStoreId();
        return ApiResponse.success(financeService.getCashierReport(storeId, date));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
