package com.xiaoniu.aftermarket.workorder.controller;

import jakarta.validation.constraints.NotBlank;

public record CancelRequest(@NotBlank(message = "取消原因不能为空") String reason) {
}
