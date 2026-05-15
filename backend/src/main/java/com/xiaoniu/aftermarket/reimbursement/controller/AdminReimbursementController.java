package com.xiaoniu.aftermarket.reimbursement.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.common.pagination.PageResponse;
import com.xiaoniu.aftermarket.common.util.DateParamParser;
import com.xiaoniu.aftermarket.reimbursement.application.ConfirmReimbursementApplicationService;
import com.xiaoniu.aftermarket.reimbursement.controller.dto.ConfirmReimbursementRequest;
import com.xiaoniu.aftermarket.reimbursement.controller.dto.RejectReimbursementRequest;
import com.xiaoniu.aftermarket.reimbursement.dto.ConfirmReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.dto.ReimbursementQueryRequest;
import com.xiaoniu.aftermarket.reimbursement.dto.ReimbursementResponse;
import com.xiaoniu.aftermarket.reimbursement.dto.RejectReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.service.ReimbursementService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reimbursements")
public class AdminReimbursementController {

    private final ReimbursementService reimbursementService;
    private final ConfirmReimbursementApplicationService confirmService;

    public AdminReimbursementController(ReimbursementService reimbursementService,
                                        ConfirmReimbursementApplicationService confirmService) {
        this.reimbursementService = reimbursementService;
        this.confirmService = confirmService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReimbursementResponse>> list(
            @RequestParam(required = false) String reimbursementNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        CurrentUser user = requireCurrentUser();
        LocalDateTime parsedFrom;
        LocalDateTime parsedTo;
        try {
            parsedFrom = DateParamParser.parseStartDateTime(dateFrom);
            parsedTo = DateParamParser.parseEndDateTime(dateTo);
        } catch (IllegalArgumentException e) {
            return ApiResponse.failure(ErrorCode.COMMON_BAD_REQUEST, e.getMessage());
        }

        ReimbursementQueryRequest request = new ReimbursementQueryRequest();
        request.setStoreId(user.storeId());
        request.setReimbursementNo(reimbursementNo);
        request.setStatus(status);
        request.setApplicantId(applicantId);
        request.setDateFrom(parsedFrom);
        request.setDateTo(parsedTo);
        request.setPageNo(pageNo);
        request.setPageSize(pageSize);
        return ApiResponse.success(reimbursementService.pageQuery(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<ReimbursementResponse> get(@PathVariable Long id) {
        CurrentUser user = requireCurrentUser();
        return ApiResponse.success(reimbursementService.getById(user.storeId(), id));
    }

    @PostMapping("/{id}/confirm")
    public ApiResponse<ReimbursementResponse> confirm(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmReimbursementRequest request) {
        CurrentUser user = requireCurrentUser();

        ConfirmReimbursementCommand command = new ConfirmReimbursementCommand();
        command.setStoreId(user.storeId());
        command.setReimbursementId(id);
        command.setOperatorId(user.userId());
        command.setConfirmedAmount(request.confirmedAmount());
        command.setRemark(request.remark());
        confirmService.execute(command);
        return ApiResponse.success(reimbursementService.getById(user.storeId(), id));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ReimbursementResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectReimbursementRequest request) {
        CurrentUser user = requireCurrentUser();

        RejectReimbursementCommand command = new RejectReimbursementCommand();
        command.setStoreId(user.storeId());
        command.setReimbursementId(id);
        command.setOperatorId(user.userId());
        command.setRejectReason(request.rejectReason());
        reimbursementService.reject(command);
        return ApiResponse.success(reimbursementService.getById(user.storeId(), id));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
