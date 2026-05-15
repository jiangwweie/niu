package com.xiaoniu.aftermarket.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@ActiveProfiles("prod-like")
@AutoConfigureMockMvc
@SpringBootTest
class ProdLikeSecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void devHeadersDoNotAuthenticateInProdLikeProfile() throws Exception {
        mockMvc.perform(get("/api/admin/dict/types")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/staff/dict/types/PART_CATEGORY/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
