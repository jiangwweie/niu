package com.xiaoniu.aftermarket.auth.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

// JWT 设计：仅作为身份凭证（userId + storeId），不缓存 accountType/权限等易变数据
// 每次请求在 JwtAuthenticationFilter 中从 DB 重新加载，确保权限变更即时生效
@Component
public class JwtProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtProvider(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public JwtToken generateAccessToken(AuthenticatedUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(Duration.ofSeconds(properties.accessTokenTtlSeconds()));
        return generateAccessToken(user, issuedAt, expiresAt);
    }

    public JwtToken generateAccessToken(AuthenticatedUser user, Instant issuedAt, Instant expiresAt) {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", String.valueOf(user.userId()));
        claims.put("userId", user.userId());
        claims.put("storeId", user.storeId());
        claims.put("username", user.username());
        claims.put("realName", user.realName());
        claims.put("roleCodes", sorted(user.roleCodes()));
        claims.put("permissionCodes", sorted(user.permissionCodes()));
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = encodeJson(header) + "." + encodeJson(claims);
        String signature = sign(unsignedToken);
        return new JwtToken(unsignedToken + "." + signature, expiresAt);
    }

    public AuthenticatedUser parseAndValidate(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtAuthenticationException("Invalid JWT format");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        String expectedSignature = sign(unsignedToken);
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            throw new JwtAuthenticationException("Invalid JWT signature");
        }

        Map<String, Object> claims = decodeJson(parts[1]);
        Instant expiresAt = Instant.ofEpochSecond(asLong(claims.get("exp"), "exp"));
        if (!expiresAt.isAfter(Instant.now())) {
            throw new JwtAuthenticationException("JWT expired");
        }

        return new AuthenticatedUser(
                asLong(claims.get("userId"), "userId"),
                asNullableLong(claims.get("storeId")),
                asString(claims.get("username")),
                asString(claims.get("realName")),
                null, // accountType 不存入 JWT，每次请求从 DB 重建，避免角色变更后 JWT 仍持有旧值
                asStringSet(claims.get("roleCodes")),
                asStringSet(claims.get("permissionCodes")),
                null, // wechatBound 不存入 JWT，每次请求从 DB 重建
                null  // wechatBoundAt 不存入 JWT，每次请求从 DB 重建
        );
    }

    private List<String> sorted(Set<String> values) {
        return new TreeSet<>(values == null ? Set.of() : values).stream().toList();
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to encode JWT JSON", exception);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(URL_DECODER.decode(value), MAP_TYPE);
        } catch (Exception exception) {
            throw new JwtAuthenticationException("Invalid JWT payload", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        if (expectedBytes.length != actualBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expectedBytes.length; i++) {
            result |= expectedBytes[i] ^ actualBytes[i];
        }
        return result == 0;
    }

    private Long asLong(Object value, String claimName) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        throw new JwtAuthenticationException("Missing JWT claim: " + claimName);
    }

    private Long asNullableLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Long.valueOf(text);
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Set<String> asStringSet(Object value) {
        if (!(value instanceof List<?> values)) {
            return Set.of();
        }
        return values.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public record JwtToken(String token, Instant expiresAt) {
    }
}
