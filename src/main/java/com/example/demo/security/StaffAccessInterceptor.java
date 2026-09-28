package com.example.demo.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class StaffAccessInterceptor implements HandlerInterceptor {

    private final String staffApiKey;

    public StaffAccessInterceptor(@Value("${STAFF_API_KEY:}") String staffApiKey) {
        this.staffApiKey = staffApiKey;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!requiresStaffAccess(request)) {
            return true;
        }

        if (staffApiKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"Staff access is not configured on the server\"}");
            return false;
        }

        String suppliedKey = request.getHeader("X-Staff-Key");
        boolean authorized = suppliedKey != null && MessageDigest.isEqual(
                staffApiKey.getBytes(StandardCharsets.UTF_8), suppliedKey.getBytes(StandardCharsets.UTF_8));
        if (!authorized) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"A valid staff access key is required\"}");
            return false;
        }
        return true;
    }

    private boolean requiresStaffAccess(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean managementWrite = (path.startsWith("/departments") || path.startsWith("/categories"))
                && ("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method));
        boolean statusUpdate = path.matches("/grievances/\\d+/status") && "PUT".equals(method);
        return managementWrite || statusUpdate;
    }
}
