package com.xiaoniu.aftermarket.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public final class TestAuthHelper {

    private TestAuthHelper() {
    }

    public static String loginBody(MockMvc mockMvc, ObjectMapper objectMapper,
                                   String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/auth/captcha"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode captcha = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        return """
                {"username":"%s","password":"%s","captchaId":"%s","captchaCode":"%s"}
                """.formatted(username, password, captcha.path("captchaId").asText(), answer(captcha.path("captchaText").asText()));
    }

    private static String answer(String captchaText) {
        String[] parts = captchaText.replace("= ?", "").split("\\+");
        return String.valueOf(Integer.parseInt(parts[0].trim()) + Integer.parseInt(parts[1].trim()));
    }
}
