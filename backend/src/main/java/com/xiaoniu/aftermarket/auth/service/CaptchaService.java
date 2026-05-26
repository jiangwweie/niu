package com.xiaoniu.aftermarket.auth.service;

import com.xiaoniu.aftermarket.auth.dto.CaptchaResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CaptchaService {

    private static final int EXPIRES_IN_SECONDS = 300;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> entries = new ConcurrentHashMap<>();

    public CaptchaResponse create() {
        cleanupExpired();
        int left = random.nextInt(9) + 1;
        int right = random.nextInt(9) + 1;
        String captchaId = UUID.randomUUID().toString();
        entries.put(captchaId, new CaptchaEntry(String.valueOf(left + right),
                Instant.now().plusSeconds(EXPIRES_IN_SECONDS)));
        return new CaptchaResponse(captchaId, left + " + " + right + " = ?", EXPIRES_IN_SECONDS);
    }

    public void validateAndConsume(String captchaId, String captchaCode) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(captchaCode)) {
            throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED);
        }
        CaptchaEntry entry = entries.remove(captchaId);
        if (entry == null) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            throw new BusinessException(ErrorCode.CAPTCHA_EXPIRED);
        }
        if (!entry.answer().equals(captchaCode.trim())) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        entries.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private record CaptchaEntry(String answer, Instant expiresAt) {
    }
}
