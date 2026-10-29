package com.greathiit.bwc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greathiit.bwc.common.R;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：解析 token 并写入 AuthContext / TenantContext。
 * system 超管可通过 X-Tenant-Id 下钻租户（只读）。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC = List.of(
            "/api/auth/login", "/api/auth/wx-login", "/api/health",
            "/api/portal/**", "/api/wechat/callback", "/api/public/**",
            "/uploads/**", "/error"
    );

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return PUBLIC.stream().anyMatch(p -> MATCHER.match(p, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        String token = auth != null && auth.startsWith("Bearer ") ? auth.substring(7) : null;
        if (token == null) {
            writeUnauthorized(response, "未登录或登录已失效");
            return;
        }
        try {
            LoginUser user = jwtUtil.parse(token);
            AuthContext.set(user);
            if (user.isPlatform()) {
                Long headerTenant = parseLong(request.getHeader("X-Tenant-Id"));
                if (headerTenant != null && headerTenant > 0) {
                    TenantContext.set(headerTenant, false, true, true);
                } else {
                    TenantContext.set(0L, true, true, false);
                }
            } else {
                TenantContext.set(user.getTenantId(), false, false, false);
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            writeUnauthorized(response, "登录已失效，请重新登录");
        } finally {
            AuthContext.clear();
            TenantContext.clear();
        }
    }

    private Long parseLong(String value) {
        try {
            return value == null ? null : Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(401, message)));
    }
}
