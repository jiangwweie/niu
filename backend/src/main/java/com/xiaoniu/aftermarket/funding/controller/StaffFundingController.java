package com.xiaoniu.aftermarket.funding.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.ApplicationResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.AttachmentResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingDetailResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.FundingPaymentResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.LedgerResponse;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.RecordFundingPaymentRequest;
import com.xiaoniu.aftermarket.funding.dto.FundingDtos.SaveApplicationRequest;
import com.xiaoniu.aftermarket.funding.service.FundingService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/staff/funding")
public class StaffFundingController {

    private final FundingService fundingService;

    public StaffFundingController(FundingService fundingService) {
        this.fundingService = fundingService;
    }

    @GetMapping("/applications")
    @PreAuthorize("hasAuthority('FUNDING_APPLICATION_VIEW')")
    public ApiResponse<PageResponse<ApplicationResponse>> listApplications(@RequestParam(required = false) String keyword,
                                                                           @RequestParam(required = false) String status,
                                                                           @RequestParam(defaultValue = "1") int pageNo,
                                                                           @RequestParam(defaultValue = "10") int pageSize) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.listApplications(user.storeId(), keyword, status, pageNo, pageSize));
    }

    @PostMapping("/applications")
    @PreAuthorize("hasAuthority('FUNDING_APPLICATION_MANAGE')")
    public ApiResponse<ApplicationResponse> createApplication(@Valid @RequestBody SaveApplicationRequest request) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.createApplication(user.storeId(), user.userId(), request));
    }

    @PutMapping("/applications/{id}")
    @PreAuthorize("hasAuthority('FUNDING_APPLICATION_MANAGE')")
    public ApiResponse<ApplicationResponse> updateApplication(@PathVariable Long id,
                                                              @Valid @RequestBody SaveApplicationRequest request) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.updateApplication(user.storeId(), user.userId(), id, request));
    }

    @PostMapping("/applications/{id}/submit")
    @PreAuthorize("hasAuthority('FUNDING_APPLICATION_MANAGE')")
    public ApiResponse<ApplicationResponse> submitApplication(@PathVariable Long id) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.submitApplication(user.storeId(), user.userId(), id));
    }

    @GetMapping("/applications/{id}")
    @PreAuthorize("hasAuthority('FUNDING_APPLICATION_VIEW')")
    public ApiResponse<FundingDetailResponse> getApplication(@PathVariable Long id) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.getApplicationDetail(user.storeId(), id));
    }

    @GetMapping("/ledgers")
    @PreAuthorize("hasAuthority('FUNDING_LEDGER_VIEW')")
    public ApiResponse<PageResponse<LedgerResponse>> listLedgers(@RequestParam(required = false) String keyword,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(defaultValue = "1") int pageNo,
                                                                 @RequestParam(defaultValue = "10") int pageSize) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.listLedgers(user.storeId(), keyword, status, pageNo, pageSize));
    }

    @GetMapping("/ledgers/{id}")
    @PreAuthorize("hasAuthority('FUNDING_LEDGER_VIEW')")
    public ApiResponse<FundingDetailResponse> getLedger(@PathVariable Long id) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.getLedgerDetail(user.storeId(), id));
    }

    @PostMapping("/ledgers/{id}/payments")
    @PreAuthorize("hasAuthority('FUNDING_PAYMENT_RECORD')")
    public ApiResponse<FundingPaymentResponse> recordPayment(@PathVariable Long id,
                                                             @Valid @RequestBody RecordFundingPaymentRequest request) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.recordPayment(user.storeId(), user.userId(), id, request));
    }

    @PostMapping("/attachments")
    @PreAuthorize("hasAnyAuthority('FUNDING_APPLICATION_MANAGE', 'FUNDING_PAYMENT_RECORD')")
    public ApiResponse<AttachmentResponse> uploadAttachment(@RequestParam String ownerType,
                                                            @RequestParam Long ownerId,
                                                            @RequestParam String attachmentType,
                                                            @RequestParam(required = false) String remark,
                                                            @RequestParam MultipartFile file) throws IOException {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(fundingService.uploadAttachment(user.storeId(), user.userId(), ownerType, ownerId, attachmentType, file, remark));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .filter(u -> u.storeId() != null)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLATFORM_STORE_CONTEXT_REQUIRED));
    }
}
