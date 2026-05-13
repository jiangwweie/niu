package com.xiaoniu.aftermarket.dict.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.xiaoniu.aftermarket.common.api.ApiResponse;
import com.xiaoniu.aftermarket.common.context.CurrentUser;
import com.xiaoniu.aftermarket.common.context.CurrentUserContext;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class DictControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listItemsReturnsVoWithUnifiedApiResponse() throws Exception {
        mockMvc.perform(get("/api/admin/dict/types/PART_CATEGORY/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].itemCode").value("BATTERY"))
                .andExpect(jsonPath("$.data[0].itemName").value("电池"))
                .andExpect(jsonPath("$.data[0].sortOrder").value(1))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].typeId").doesNotExist())
                .andExpect(jsonPath("$.traceId").value(nullValue()));
    }

    @Test
    void devHeadersPopulateCurrentUserContextForAdminRequest() throws Exception {
        mockMvc.perform(get("/api/admin/test/current-user")
                        .header("X-User-Id", "99")
                        .header("X-Store-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.userId").value(99))
                .andExpect(jsonPath("$.data.storeId").value(7));
    }

    @Test
    void missingDevHeadersLeaveCurrentUserContextEmpty() throws Exception {
        mockMvc.perform(get("/api/admin/test/current-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void currentUserContextIsClearedAfterRequestCompletion() throws Exception {
        mockMvc.perform(get("/api/admin/test/current-user")
                        .header("X-User-Id", "99")
                        .header("X-Store-Id", "7"))
                .andExpect(status().isOk());

        assertTrue(CurrentUserContext.get().isEmpty());
    }

    @Test
    void apiResponseDoesNotExposeSuccessField() throws Exception {
        mockMvc.perform(get("/api/admin/dict/types/PART_CATEGORY/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").doesNotExist())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.traceId").value(nullValue()));
    }

    @TestConfiguration
    static class CurrentUserProbeConfiguration {

        @Bean
        CurrentUserProbeController currentUserProbeController() {
            return new CurrentUserProbeController();
        }
    }

    @RestController
    static class CurrentUserProbeController {

        @GetMapping("/api/admin/test/current-user")
        ApiResponse<Map<String, Long>> currentUser() {
            CurrentUser currentUser = CurrentUserContext.get().orElse(null);
            if (currentUser == null) {
                return ApiResponse.success(Map.of());
            }
            return ApiResponse.success(Map.of(
                    "userId", currentUser.userId(),
                    "storeId", currentUser.storeId()
            ));
        }
    }
}
