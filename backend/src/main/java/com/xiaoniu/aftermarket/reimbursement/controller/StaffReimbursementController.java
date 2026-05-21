package com.xiaoniu.aftermarket.reimbursement.controller;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import com.xiaoniu.aftermarket.reimbursement.controller.dto.SubmitReimbursementRequest;
import com.xiaoniu.aftermarket.reimbursement.dto.ReimbursementResponse;
import com.xiaoniu.aftermarket.reimbursement.dto.SubmitReimbursementCommand;
import com.xiaoniu.aftermarket.reimbursement.service.ReimbursementService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/staff/reimbursements")
public class StaffReimbursementController {

    private final ReimbursementService reimbursementService;

    public StaffReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REIMBURSEMENT_SUBMIT')")
    public ApiResponse<ReimbursementResponse> submit(
            @Valid @RequestBody SubmitReimbursementRequest request) {
        CurrentUser user = requireCurrentUser();

        SubmitReimbursementCommand command = new SubmitReimbursementCommand();
        command.setStoreId(user.storeId());
        command.setApplicantId(user.userId());
        command.setOperatorId(user.userId());
        command.setPurpose(request.purpose());
        command.setAmount(request.amount());
        command.setRemark(request.remark());

        Long reimbursementId = reimbursementService.submit(command);
        // 员工提交报销仅进入待审批队列，不直接入财务成本；需管理员 CONFIRM 后才计入
        return ApiResponse.success(reimbursementService.getById(user.storeId(), reimbursementId));
    }

    private CurrentUser requireCurrentUser() {
        return CurrentUserContext.get()
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMON_BAD_REQUEST, "缺少用户上下文"));
    }
}
