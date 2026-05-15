package com.xiaoniu.aftermarket.auth.security;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Set;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class JwtSecretValidator {

    private static final Set<String> UNSAFE_PROFILES = Set.of("dev", "test", "default");
    private static final List<String> UNSAFE_KEYWORDS = List.of("dev", "change-me", "please", "placeholder");
    private static final int MIN_SECRET_LENGTH = 32;

    private final JwtProperties jwtProperties;
    private final Environment environment;

    public JwtSecretValidator(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @PostConstruct
    void validateSecret() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isUnsafeProfile = activeProfiles.length == 0
                || List.of(activeProfiles).stream().anyMatch(UNSAFE_PROFILES::contains);

        if (isUnsafeProfile) {
            return;
        }

        String secret = jwtProperties.secret();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least " + MIN_SECRET_LENGTH + " characters in production");
        }
        String lower = secret.toLowerCase();
        for (String keyword : UNSAFE_KEYWORDS) {
            if (lower.contains(keyword)) {
                throw new IllegalStateException(
                        "JWT_SECRET contains unsafe keyword '" + keyword + "' — set a proper secret via JWT_SECRET env var");
            }
        }
    }
}
