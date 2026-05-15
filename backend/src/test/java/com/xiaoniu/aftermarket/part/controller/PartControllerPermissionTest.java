package com.xiaoniu.aftermarket.part.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class PartControllerPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    private String tokenWithPartManage() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(1L, 1L, "admin01", "张三",
                        Set.of("ADMIN"), Set.of("PART_MANAGE")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    private String tokenWithoutPartManage() {
        return jwtProvider.generateAccessToken(
                new AuthenticatedUser(2L, 1L, "tech01", "赵维修",
                        Set.of("TECHNICIAN_FRONT_DESK"), Set.of("WORK_ORDER_SUBMIT")),
                Instant.now(), Instant.now().plusSeconds(3600)
        ).token();
    }

    @Test
    void unauthenticatedWriteReturns401() throws Exception {
        mockMvc.perform(post("/api/admin/parts/official")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/admin/parts/third-party")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(put("/api/admin/parts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/admin/parts/1/enable"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/admin/parts/1/disable"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void withoutPartManageReturns403() throws Exception {
        String token = tokenWithoutPartManage();

        // createOfficialPart: @Valid requires partName + officialPartNo
        mockMvc.perform(post("/api/admin/parts/official")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partName\":\"test\",\"officialPartNo\":\"P001\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // createThirdPartyPart: @Valid requires partName
        mockMvc.perform(post("/api/admin/parts/third-party")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partName\":\"test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        // updatePart: no @NotBlank fields, {} passes @Valid
        mockMvc.perform(put("/api/admin/parts/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/admin/parts/1/enable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(post("/api/admin/parts/1/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void withPartManageCanAccessWriteEndpoints() throws Exception {
        String token = tokenWithPartManage();

        // Verify PART_MANAGE permission bypasses @PreAuthorize — response is NOT 403
        mockMvc.perform(post("/api/admin/parts/official")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partName\":\"perm-test-official\",\"officialPartNo\":\"PT001\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "PART_MANAGE user should not get 403 on createOfficialPart";
                });

        mockMvc.perform(post("/api/admin/parts/third-party")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partName\":\"perm-test-third-party\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "PART_MANAGE user should not get 403 on createThirdPartyPart";
                });

        mockMvc.perform(put("/api/admin/parts/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "PART_MANAGE user should not get 403 on updatePart";
                });

        mockMvc.perform(post("/api/admin/parts/999/enable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "PART_MANAGE user should not get 403 on enablePart";
                });

        mockMvc.perform(post("/api/admin/parts/999/disable")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "PART_MANAGE user should not get 403 on disablePart";
                });
    }

    @Test
    void queryEndpointsRequireOnlyAuthentication() throws Exception {
        String token = tokenWithoutPartManage();

        mockMvc.perform(get("/api/admin/parts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "Query endpoints should not require PART_MANAGE";
                });

        mockMvc.perform(get("/api/admin/parts/1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assert status != 403 : "Query endpoints should not require PART_MANAGE";
                });
    }
}
