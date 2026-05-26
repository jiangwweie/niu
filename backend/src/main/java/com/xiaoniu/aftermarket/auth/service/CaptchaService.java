package com.xiaoniu.aftermarket.auth.service;

import com.xiaoniu.aftermarket.auth.dto.CaptchaResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import com.xiaoniu.aftermarket.common.exception.BusinessException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CaptchaService {

    private static final int EXPIRES_IN_SECONDS = 300;
    private static final int MAX_ENTRIES = 10_000;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> entries = new ConcurrentHashMap<>();
    private final boolean exposeTextForTests;

    public CaptchaService(Environment environment) {
        this.exposeTextForTests = Arrays.asList(environment.getActiveProfiles()).contains("test");
    }

    public CaptchaResponse create() {
        ensureCapacity();
        int left = random.nextInt(9) + 1;
        int right = random.nextInt(9) + 1;
        String text = left + " + " + right + " = ?";
        String captchaId = UUID.randomUUID().toString();
        entries.put(captchaId, new CaptchaEntry(String.valueOf(left + right),
                Instant.now().plusSeconds(EXPIRES_IN_SECONDS)));
        return new CaptchaResponse(captchaId, exposeTextForTests ? text : null, renderImage(text), EXPIRES_IN_SECONDS);
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

    private void ensureCapacity() {
        cleanupExpired();
        if (entries.size() >= MAX_ENTRIES) {
            cleanupExpired();
        }
        if (entries.size() >= MAX_ENTRIES) {
            throw new BusinessException(ErrorCode.CAPTCHA_LIMIT_EXCEEDED);
        }
    }

    private String renderImage(String text) {
        try {
            BufferedImage image = new BufferedImage(132, 40, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(246, 248, 250));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(new Color(64, 158, 255));
            for (int i = 0; i < 5; i++) {
                graphics.drawLine(random.nextInt(image.getWidth()), random.nextInt(image.getHeight()),
                        random.nextInt(image.getWidth()), random.nextInt(image.getHeight()));
            }
            graphics.setColor(new Color(48, 49, 51));
            graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            graphics.drawString(text, 18, 27);
            graphics.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.COMMON_INTERNAL_ERROR, "验证码生成失败");
        }
    }

    private record CaptchaEntry(String answer, Instant expiresAt) {
    }
}
