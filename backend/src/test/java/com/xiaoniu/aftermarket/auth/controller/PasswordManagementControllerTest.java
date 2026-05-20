package com.xiaoniu.aftermarket.auth.controller;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import com.xiaoniu.aftermarket.user.entity.SysUserEntity;
import com.xiaoniu.aftermarket.user.mapper.SysUserMapper;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class PasswordManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void meReturnsPasswordMustChange() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.passwordMustChange").value(false));
    }

    @Test
    void changePasswordRejectsWrongOldPassword() throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"wrong\",\"newPassword\":\"Niu12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OLD_PASSWORD_INCORRECT"));
    }

    @Test
    void changePasswordSuccessClearsMustChange() throws Exception {
        SysUserEntity user = userMapper.selectById(2L);
        user.setPasswordMustChange(true);
        userMapper.updateById(user);

        mockMvc.perform(post("/api/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"dev123\",\"newPassword\":\"Niu12345\"}"))
                .andExpect(status().isOk());

        SysUserEntity updated = userMapper.selectById(2L);
        assertFalse(Boolean.TRUE.equals(updated.getPasswordMustChange()));
        assertFalse(updated.getPasswordHash().contains("{noop}"));
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Niu12345", updated.getPasswordHash()));
    }

    private String token(Long userId) {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(userId, 1L, "user" + userId, "测试用户", null, Set.of("ADMIN"), Set.of()),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }
}
