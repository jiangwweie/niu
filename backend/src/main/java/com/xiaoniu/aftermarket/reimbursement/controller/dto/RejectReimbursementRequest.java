package com.xiaoniu.aftermarket.reimbursement.controller.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectReimbursementRequest(
        @NotBlank(message = "驳回原因不能为空")
        String rejectReason
) {
}
