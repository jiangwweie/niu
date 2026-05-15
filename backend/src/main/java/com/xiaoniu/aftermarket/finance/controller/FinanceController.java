package com.xiaoniu.aftermarket.finance.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.finance.dto.FinanceReportResponse;
import com.xiaoniu.aftermarket.finance.service.FinanceService;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/finance")
public class FinanceController {

    private final FinanceService financeService;

    public FinanceController(FinanceService financeService) {
        this.financeService = financeService;
    }

    @GetMapping("/daily")
    public ApiResponse<FinanceReportResponse> queryDaily(
            @RequestParam(required = false) String date) {
        CurrentUser user = requireCurrentUser();
        LocalDate queryDate = (date != null && !date.isBlank()) ? LocalDate.parse(date) : LocalDate.now();
        return ApiResponse.success(financeService.queryDaily(user.storeId(), queryDate));
    }

    @GetMapping("/monthly")
    public ApiResponse<FinanceReportResponse> queryMonthly(
            @RequestParam int year,
            @RequestParam int month) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(financeService.queryMonthly(user.storeId(), year, month));
    }

    @GetMapping("/range")
    public ApiResponse<FinanceReportResponse> queryRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        CurrentUser user = requireCurrentUser();
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        if (start.isAfter(end)) {
            return ApiResponse.failure(ErrorCode.COMMON_BAD_REQUEST, "开始日期不能晚于结束日期");
        }
        return ApiResponse.success(financeService.queryRange(user.storeId(), start, end));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
