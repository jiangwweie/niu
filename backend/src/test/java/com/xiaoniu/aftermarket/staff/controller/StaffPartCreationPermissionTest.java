package com.xiaoniu.aftermarket.staff.controller;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * M18G: 验证扫码新增配件入库权限收口。
 * <p>
 * 使用 data.sql 中的测试用户（权限从 DB 加载）：
 * - user 1 (admin01): INVENTORY_INBOUND + PART_MANAGE（ADMIN 角色）
 * - user 50 (inv_staff): 仅 INVENTORY_INBOUND（INVENTORY_STAFF 角色）
 * - user 51 (part_creator): 仅 PART_CREATE（PART_CREATOR 角色）
 * <p>
 * - 独立创建配件接口必须要求 PART_CREATE 或 PART_MANAGE
 * - create-part-and-inbound 必须同时要求 INVENTORY_INBOUND + (PART_CREATE | PART_MANAGE)
 * - 仅 INVENTORY_INBOUND 不允许创建配件
 */
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class StaffPartCreationPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String OFFICIAL_BODY = """
            {"partName":"权限测试官方件","officialPartNo":"PERM-OFF-001"}
            """;
    private static final String THIRD_PARTY_BODY = """
            {"partName":"权限测试第三方件"}
            """;
    private static final String CREATE_AND_INBOUND_BODY = """
            {"source":"THIRD_PARTY","partName":"权限测试原子件","inboundQuantity":1}
            """;

    // ── P0-1: 独立创建配件接口必须要求 PART_CREATE 或 PART_MANAGE ──

    @Test
    void onlyInboundCannotCreateOfficialPart() throws Exception {
        // user 50: INVENTORY_INBOUND only → 403
        mockMvc.perform(post("/api/staff/parts/official")
                        .header("X-User-Id", "50")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OFFICIAL_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void onlyInboundCannotCreateThirdPartyPart() throws Exception {
        // user 50: INVENTORY_INBOUND only → 403
        mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "50")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(THIRD_PARTY_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void partCreateCanCreateOfficialPart() throws Exception {
        // user 51: PART_CREATE only → can create parts
        mockMvc.perform(post("/api/staff/parts/official")
                        .header("X-User-Id", "51")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OFFICIAL_BODY))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(403, s, "PART_CREATE 应可创建官方配件");
                });
    }

    @Test
    void partManageCanCreateThirdPartyPart() throws Exception {
        // user 1: PART_MANAGE → can create parts
        mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(THIRD_PARTY_BODY))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(403, s, "PART_MANAGE 应可创建第三方配件");
                });
    }

    // ── P0-2: create-part-and-inbound 必须同时要求 INVENTORY_INBOUND + (PART_CREATE | PART_MANAGE) ──

    @Test
    void onlyInboundCannotCreatePartAndInbound() throws Exception {
        // user 50: INVENTORY_INBOUND only → passes @PreAuthorize but fails runtime check
        mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "50")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_AND_INBOUND_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("创建配件需要 PART_CREATE 或 PART_MANAGE 权限"));
    }

    @Test
    void onlyPartCreateCannotCreatePartAndInbound() throws Exception {
        // user 51: PART_CREATE only → no INVENTORY_INBOUND → 403
        mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "51")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_AND_INBOUND_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void inboundPlusPartCreateCanCreatePartAndInbound() throws Exception {
        // user 1: INVENTORY_INBOUND + PART_MANAGE → both checks pass
        mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_AND_INBOUND_BODY))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(403, s, "INVENTORY_INBOUND + PART_MANAGE 应可执行原子接口");
                    assertNotEquals(400, s, "INVENTORY_INBOUND + PART_MANAGE 应可执行原子接口, got: " + s);
                });
    }

    @Test
    void partManageAloneCannotCreatePartAndInbound() throws Exception {
        // user 1 has both INVENTORY_INBOUND and PART_MANAGE, so this test verifies
        // that a user WITHOUT INVENTORY_INBOUND gets 403.
        // user 51 has PART_CREATE but no INVENTORY_INBOUND → 403
        mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "51")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_AND_INBOUND_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    // ── P0-3: source 字段一致性 ──

    @Test
    void thirdPartyEndpointRejectsSourceOfficial() throws Exception {
        String body = """
                {"source":"OFFICIAL","partName":"source不一致测试"}
                """;
        mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("source 与接口语义不一致，应为 THIRD_PARTY"));
    }

    @Test
    void officialEndpointRejectsSourceThirdParty() throws Exception {
        String body = """
                {"source":"THIRD_PARTY","partName":"source不一致测试","officialPartNo":"PERM-SRC-001"}
                """;
        mockMvc.perform(post("/api/staff/parts/official")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("source 与接口语义不一致，应为 OFFICIAL"));
    }

    @Test
    void thirdPartyEndpointAcceptsNoSource() throws Exception {
        String body = """
                {"partName":"无source测试"}
                """;
        mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(403, s, "不传 source 应正常创建");
                    assertNotEquals(400, s, "不传 source 不应报错, got: " + s);
                });
    }

    @Test
    void thirdPartyEndpointAcceptsMatchingSource() throws Exception {
        String body = """
                {"source":"THIRD_PARTY","partName":"source一致测试"}
                """;
        mockMvc.perform(post("/api/staff/parts/third-party")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(403, s, "source 一致应正常创建");
                    assertNotEquals(400, s, "source 一致不应报错, got: " + s);
                });
    }

    @Test
    void createPartAndInboundSourceStillWorks() throws Exception {
        String body = """
                {"source":"OFFICIAL","partName":"原子接口source测试","officialPartNo":"PERM-ATOMIC-001","inboundQuantity":1}
                """;
        mockMvc.perform(post("/api/staff/inventory/inbound/create-part-and-inbound")
                        .header("X-User-Id", "1")
                        .header("X-Store-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(result -> {
                    int s = result.getResponse().getStatus();
                    assertNotEquals(403, s, "原子接口 source=OFFICIAL 应正常");
                    assertNotEquals(400, s, "原子接口 source=OFFICIAL 应正常, got: " + s);
                });
    }
}
