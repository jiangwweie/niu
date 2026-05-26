package com.xiaoniu.aftermarket.auth.dto;

public record CaptchaResponse(
        String captchaId,
        String captchaText,
        String imageBase64,
        int expiresInSeconds
) {
}
