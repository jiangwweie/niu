package com.xiaoniu.aftermarket.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record WechatBindRequest(@NotBlank String code) {
}
