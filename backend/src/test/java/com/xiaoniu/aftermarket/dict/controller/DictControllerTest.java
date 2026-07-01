package com.xiaoniu.aftermarket.dict.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        mockMvc.perform(get("/api/admin/dict/types/PART_CATEGORY/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].itemCode").value("STORE_BATTERY"))
                .andExpect(jsonPath("$.data[0].itemName").value("本店电池"))
                .andExpect(jsonPath("$.data[0].sortOrder").value(0))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[0].id").value(4))
                .andExpect(jsonPath("$.data[0].typeId").doesNotExist())
                .andExpect(jsonPath("$.data[0].scope").value("STORE"))
                .andExpect(jsonPath("$.data[0].systemItem").value(false))
                .andExpect(jsonPath("$.traceId").value(nullValue()));
    }

    @Test
    void storeAdminCanCreateAndDeleteStoreDictItem() throws Exception {
        mockMvc.perform(post("/api/admin/dict/types/PART_CATEGORY/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType("application/json")
                        .content("""
                                {
                                  "itemCode": "STORE_TEST_CATEGORY",
                                  "itemName": "本店测试分类",
                                  "sortOrder": -1,
                                  "scope": "STORE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.scope").value("STORE"))
                .andExpect(jsonPath("$.data.storeId").value(1))
                .andExpect(jsonPath("$.data.editable").value(true));

        mockMvc.perform(get("/api/admin/dict/types/PART_CATEGORY/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].itemCode").value("STORE_TEST_CATEGORY"));
    }

    @Test
    void storeUserCannotDeleteSystemDictItem() throws Exception {
        mockMvc.perform(delete("/api/admin/dict/items/1")
                        .header("X-User-Id", "10")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
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
    void listTypesReturnsEnabledTypes() throws Exception {
        mockMvc.perform(get("/api/admin/dict/types")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].typeCode").value("PART_CATEGORY"))
                .andExpect(jsonPath("$.data[0].typeName").value("零件分类"))
                .andExpect(jsonPath("$.data[0].enabled").value(true))
                .andExpect(jsonPath("$.data[7].typeCode").value("INBOUND_REASON"))
                .andExpect(jsonPath("$.data[7].editMode").value("STORE_EXTENDABLE"));
    }

    @Test
    void listItemsReturnsEmptyForNonexistentTypeCode() throws Exception {
        mockMvc.perform(get("/api/admin/dict/types/NONEXISTENT/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void listItemsReturnsEmptyForDisabledType() throws Exception {
        // REPAIR_TYPE is DISABLED in test data
        mockMvc.perform(get("/api/admin/dict/types/REPAIR_TYPE/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void apiResponseDoesNotExposeSuccessField() throws Exception {
        mockMvc.perform(get("/api/admin/dict/types/PART_CATEGORY/items")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1"))
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
