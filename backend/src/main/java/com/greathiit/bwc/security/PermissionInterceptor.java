package com.greathiit.bwc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greathiit.bwc.common.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 权限拦截器：校验 @RequirePermission；403 越权返回并落审计。
 */
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final com.greathiit.bwc.module.system.service.AuditService auditService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        if (AuthContext.get() == null) {
            return reject(response, 401, "未登录或登录已失效", null);
        }
        String path = request.getRequestURI();
        if (path.startsWith("/api/platform/") && !AuthContext.isPlatform()) {
            return reject(response, 403, "仅平台超级管理员可操作", "platform");
        }

        RequirePermission required = method.getMethodAnnotation(RequirePermission.class);
        if (required == null) {
            required = method.getBeanType().getAnnotation(RequirePermission.class);
        }
        if (required != null && !permissionService.has(required.value())) {
            auditService.record("security", "DENY", path, "权限不足：" + required.value());
            return reject(response, 403, "越权访问：" + required.value(), null);
        }
        return true;
    }

    private boolean reject(HttpServletResponse response, int code, String message, String biz) throws Exception {
        response.setStatus(code);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(code, message)));
        return false;
    }
}
