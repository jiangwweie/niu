package com.xiaoniu.aftermarket.trialdata.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.trialdata.dto.ClearTrialDataRequest;
import com.xiaoniu.aftermarket.trialdata.dto.ClearTrialDataResponse;
import com.xiaoniu.aftermarket.trialdata.dto.TrialDataSummaryResponse;
import com.xiaoniu.aftermarket.trialdata.service.TrialDataService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trial-data")
public class TrialDataController {

    private static final String CONFIRM_TEXT = "CONFIRM_CLEAR_TRIAL_DATA";

    private final TrialDataService trialDataService;

    public TrialDataController(TrialDataService trialDataService) {
        this.trialDataService = trialDataService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<TrialDataSummaryResponse> summary() {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(trialDataService.getSummary(user.storeId()));
    }

    @PostMapping("/clear")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<ClearTrialDataResponse> clear(@RequestBody(required = false) ClearTrialDataRequest request) {
        CurrentUser user = requireCurrentUser();

        if (request == null || !CONFIRM_TEXT.equals(request.getConfirmText())) {
            return ApiResponse.failure(ErrorCode.COMMON_BAD_REQUEST,
                    "确认文本不正确，请输入 " + CONFIRM_TEXT);
        }

        ClearTrialDataResponse response = trialDataService.clearTrialData(user.storeId());
        return ApiResponse.success(response);
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
