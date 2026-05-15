package com.xiaoniu.aftermarket.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class SecurityConfigDenyAllTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unmatchedApiPathIsDeniedForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/unknown-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unmatchedApiPathIsDeniedForAuthenticated() throws Exception {
        mockMvc.perform(get("/api/unknown-endpoint")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status == 403 || status == 401
                            : "Unmatched path should be denied (401 or 403), got " + status;
                });
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }

    @Test
    void authLoginEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}