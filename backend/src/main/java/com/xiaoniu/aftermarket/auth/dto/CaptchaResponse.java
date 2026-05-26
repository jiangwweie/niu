package com.xiaoniu.aftermarket.auth.dto;

public record CaptchaResponse(
        String captchaId,
        String captchaText,
        int expiresInSeconds
) {
}
