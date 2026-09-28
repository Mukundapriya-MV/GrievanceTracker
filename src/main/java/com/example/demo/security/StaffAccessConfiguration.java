package com.example.demo.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaffAccessConfiguration implements WebMvcConfigurer {

    private final StaffAccessInterceptor staffAccessInterceptor;

    public StaffAccessConfiguration(StaffAccessInterceptor staffAccessInterceptor) {
        this.staffAccessInterceptor = staffAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(staffAccessInterceptor).addPathPatterns("/**");
    }
}
