package com.xiaoniu.aftermarket.store.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xiaoniu.aftermarket.auth.security.AuthenticatedUser;
import com.xiaoniu.aftermarket.auth.security.JwtProvider;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class AdminStoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Test
    void storeGetPutRequireStoreManage() throws Exception {
        mockMvc.perform(get("/api/admin/store/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(Set.of("USER_MANAGE"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/store/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(Set.of("USER_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeName\":\"交付门店\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void storeGetPutWithPermissionWorks() throws Exception {
        String token = token(Set.of("STORE_MANAGE"));

        mockMvc.perform(get("/api/admin/store/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeName").value("默认门店"));

        mockMvc.perform(put("/api/admin/store/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName":"正式交付门店","contactName":"店长","contactPhone":"13900000000","address":"交付地址"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeName").value("正式交付门店"))
                .andExpect(jsonPath("$.data.contactName").value("店长"));
    }

    private String token(Set<String> permissions) {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三", Set.of("ADMIN"), permissions),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }
}
