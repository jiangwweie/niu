package com.xiaoniu.aftermarket.auth.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.WxMaUserService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.chanjar.weixin.common.error.WxError;
import me.chanjar.weixin.common.error.WxErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class WechatAuthTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private WxMaService wxMaService;
    @MockBean private WxMaUserService wxMaUserService;

    @BeforeEach
    void setupMock() throws Exception {
        when(wxMaService.getUserService()).thenReturn(wxMaUserService);
    }

    private WxMaJscode2SessionResult mockSessionResult(String openid) {
        WxMaJscode2SessionResult result = new WxMaJscode2SessionResult();
        result.setOpenid(openid);
        result.setSessionKey("test_session_key");
        return result;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    // --- WeChat Login Tests ---

    @Test
    void loginWithBoundOpenidReturnsJwt() throws Exception {
        // tech01 (id=2) has wechat_openid='test_bound_openid_001' in test data
        when(wxMaUserService.getSessionInfo("valid_code_tech01"))
                .thenReturn(mockSessionResult("test_bound_openid_001"));

        mockMvc.perform(post("/api/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"valid_code_tech01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.userId").value(2))
                .andExpect(jsonPath("$.data.user.storeId").value(1))
                .andExpect(jsonPath("$.data.user.accountType").value("STORE"));
    }

    @Test
    void loginWithUnboundOpenidReturnsNotBound() throws Exception {
        when(wxMaUserService.getSessionInfo("code_unbound"))
                .thenReturn(mockSessionResult("openid_nobody_999"));

        mockMvc.perform(post("/api/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code_unbound\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("WECHAT_NOT_BOUND"));
    }

    @Test
    void loginWechatApiErrorReturns401() throws Exception {
        WxError wxError = WxError.builder().errorCode(40029).errorMsg("invalid code").build();
        when(wxMaUserService.getSessionInfo("bad_code"))
                .thenThrow(new WxErrorException(wxError));

        mockMvc.perform(post("/api/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bad_code\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("WECHAT_LOGIN_FAILED"));
    }

    @Test
    void loginPasswordMustChangeUserGetsFlag() throws Exception {
        // tech01 has passwordMustChange=false
        when(wxMaUserService.getSessionInfo("code_tech01"))
                .thenReturn(mockSessionResult("test_bound_openid_001"));

        mockMvc.perform(post("/api/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code_tech01\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.passwordMustChange").value(false));
    }

    // --- WeChat Bind Tests ---

    @Test
    void bindWechatPlatformUserReturnsError() throws Exception {
        String token = loginAndGetToken("platform_admin", "dev123");

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"any_code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PLATFORM_CANNOT_BIND_WECHAT"));
    }

    @Test
    void bindWechatAlreadyBoundReturnsError() throws Exception {
        // tech01 already has wechat_openid='test_bound_openid_001'
        String token = loginAndGetToken("tech01", "dev123");

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"any_code\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WECHAT_ALREADY_BOUND"));
    }

    @Test
    void bindWechatOpenidAlreadyBoundByOtherReturnsError() throws Exception {
        // test02 (id=11, storeId=2) tries to bind with the openid already bound to tech01
        String token = loginAndGetToken("test02", "dev123");

        when(wxMaUserService.getSessionInfo("code_dup"))
                .thenReturn(mockSessionResult("test_bound_openid_001"));

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code_dup\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("WECHAT_OPENID_ALREADY_BOUND"));
    }

    @Test
    void bindWechatWithoutTokenReturns401() throws Exception {
        mockMvc.perform(post("/api/auth/wechat/bind")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"any_code\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Admin Unbind Tests ---

    @Test
    void adminUnbindWechatSuccess() throws Exception {
        // First bind store_admin01 (id=10, storeId=2) so we can unbind without affecting tech01
        String storeAdminToken = loginAndGetToken("store_admin01", "dev123");
        when(wxMaUserService.getSessionInfo("bind_for_unbind"))
                .thenReturn(mockSessionResult("openid_store_admin_temp"));

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storeAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"bind_for_unbind\"}"))
                .andExpect(status().isOk());

        // Now unbind store_admin01 via platform admin — but platform admin can't access /api/admin/**
        // Use store_admin01 itself as the admin (it has USER_MANAGE via role 5)
        // Actually store_admin01 (storeId=2) unbinding their own user (id=10)
        mockMvc.perform(post("/api/admin/users/10/wechat/unbind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + storeAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        // Verify: login with old openid should now fail
        when(wxMaUserService.getSessionInfo("verify_code"))
                .thenReturn(mockSessionResult("openid_store_admin_temp"));

        mockMvc.perform(post("/api/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"verify_code\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("WECHAT_NOT_BOUND"));
    }

    @Test
    void adminCannotUnbindCrossStoreUser() throws Exception {
        // store_admin01 (id=10, storeId=2) tries to unbind tech01 (id=2, storeId=1)
        String token = loginAndGetToken("store_admin01", "dev123");

        mockMvc.perform(post("/api/admin/users/2/wechat/unbind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void userListShowsWechatBoundStatus() throws Exception {
        String token = loginAndGetToken("admin01", "dev123");

        // tech01 should show wechatBound=true (bound in test data)
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("username", "tech01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].wechatBound").value(true))
                .andExpect(jsonPath("$.data.records[0].wechatBoundAt").isNotEmpty());

        // disabled01 (id=3) has no wechat binding
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("username", "disabled01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].wechatBound").value(false));
    }

    @Test
    void loginDisabledUserWithBoundOpenidReturnsFailed() throws Exception {
        // disabled01 (id=3) has no wechat binding in test data → WECHAT_NOT_BOUND
        when(wxMaUserService.getSessionInfo("code_disabled"))
                .thenReturn(mockSessionResult("openid_disabled_only"));

        mockMvc.perform(post("/api/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code_disabled\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("WECHAT_NOT_BOUND"));
    }

    @Test
    void loginDeletedUserWithBoundOpenidReturnsFailed() throws Exception {
        // deleted01 (id=4) has no wechat binding in test data → WECHAT_NOT_BOUND
        when(wxMaUserService.getSessionInfo("code_deleted"))
                .thenReturn(mockSessionResult("openid_deleted_only"));

        mockMvc.perform(post("/api/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code_deleted\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("WECHAT_NOT_BOUND"));
    }

    // --- wechatBound field in auth responses ---

    @Test
    void passwordLoginReturnsWechatBoundFields() throws Exception {
        // tech01 (id=2) has wechat_openid='test_bound_openid_001' → wechatBound=true
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tech01\",\"password\":\"dev123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.wechatBound").value(true))
                .andExpect(jsonPath("$.data.user.wechatBoundAt").isNotEmpty());

        // admin01 (id=1) has no wechat binding → wechatBound=false
        mockMvc.perform(post("/api/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin01\",\"password\":\"dev123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.wechatBound").value(false));
    }

    @Test
    void authMeReturnsWechatBoundFields() throws Exception {
        String token = loginAndGetToken("tech01", "dev123");

        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wechatBound").value(true))
                .andExpect(jsonPath("$.data.wechatBoundAt").isNotEmpty());

        String adminToken = loginAndGetToken("admin01", "dev123");
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wechatBound").value(false));
    }

    @Test
    void wechatLoginReturnsWechatBoundFields() throws Exception {
        when(wxMaUserService.getSessionInfo("code_bound_fields"))
                .thenReturn(mockSessionResult("test_bound_openid_001"));

        mockMvc.perform(post("/api/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code_bound_fields\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.wechatBound").value(true))
                .andExpect(jsonPath("$.data.user.wechatBoundAt").isNotEmpty());
    }

    @Test
    void bindWechatThenAuthMeShowsWechatBoundTrue() throws Exception {
        // bindtest01 (id=30, storeId=1) dedicated for this test, no wechat binding
        String token = loginAndGetToken("bindtest01", "dev123");

        // Verify wechatBound=false before binding
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wechatBound").value(false));

        // Bind wechat
        when(wxMaUserService.getSessionInfo("code_bind_me"))
                .thenReturn(mockSessionResult("openid_bindtest01_fresh"));

        mockMvc.perform(post("/api/auth/wechat/bind")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"code_bind_me\"}"))
                .andExpect(status().isOk());

        // Verify wechatBound=true after binding
        mockMvc.perform(get("/api/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wechatBound").value(true))
                .andExpect(jsonPath("$.data.wechatBoundAt").isNotEmpty());
    }
}
