package com.xiaoniu.aftermarket.common.config;

import com.xiaoniu.aftermarket.common.web.DevCurrentUserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

    private final DevCurrentUserInterceptor devCurrentUserInterceptor;

    public AdminWebMvcConfig(DevCurrentUserInterceptor devCurrentUserInterceptor) {
        this.devCurrentUserInterceptor = devCurrentUserInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000", "http://localhost:8081")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-User-Id", "X-Store-Id")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Dev-only bootstrap for local Admin API integration. This is not authentication
        // and must be replaced by the formal token-based CurrentUserContext setup.
        registry.addInterceptor(devCurrentUserInterceptor)
                .addPathPatterns("/api/admin/**", "/api/staff/**");
    }
}
