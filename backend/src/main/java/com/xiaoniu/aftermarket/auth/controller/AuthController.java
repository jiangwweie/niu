package com.xiaoniu.aftermarket.auth.controller;

import com.xiaoniu.aftermarket.auth.dto.AuthUserResponse;
import com.xiaoniu.aftermarket.auth.dto.LoginResponse;
import com.xiaoniu.aftermarket.auth.dto.PasswordLoginRequest;
import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.service.AuthService;
import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.api.ErrorCode;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/password")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithPassword(
            @Valid @RequestBody PasswordLoginRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success(authService.loginWithPassword(request)));
        } catch (BadCredentialsException | DisabledException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure(ErrorCode.UNAUTHORIZED));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> me() {
        AuthenticatedUser currentUser = currentUser();
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure(ErrorCode.UNAUTHORIZED));
        }
        return ResponseEntity.ok(ApiResponse.success(authService.toResponse(currentUser)));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success(null);
    }

    private AuthenticatedUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        return principal instanceof AuthenticatedUser user ? user : null;
    }
}
