package com.greathiit.bwc.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.system.entity.SysPlatformUser;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import com.greathiit.bwc.module.system.mapper.SysPlatformUserMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantUserMapper;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * JWT 认证过滤器：解析 token 后以数据库账号状态为准重建登录上下文。
 * <p>
 * 仅依赖 JWT 里的 roleCode 会导致改角色/停用账号后旧 Token 继续生效，因此每次请求
 * 都校验账号状态、租户状态与最新角色，保证权限撤销即时生效。
 * system 超管可通过 X-Tenant-Id 下钻租户（只读）。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC = List.of(
            "/api/auth/login", "/api/auth/wx-login", "/api/auth/wx-bind", "/api/health",
            "/api/portal/**", "/api/wechat/callback", "/api/public/**",
            "/uploads/**", "/error"
    );

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final SysPlatformUserMapper platformUserMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysTenantMapper tenantMapper;

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
            LoginUser loginUser = jwtUtil.parse(token);
            LoginUser current = loginUser.isPlatform()
                    ? loadPlatformUser(loginUser)
                    : loadTenantUser(loginUser);
            AuthContext.set(current);
            if (current.isPlatform()) {
                Long headerTenant = parseLong(request.getHeader("X-Tenant-Id"));
                if (headerTenant != null && headerTenant > 0) {
                    TenantContext.set(headerTenant, false, true, true);
                } else {
                    TenantContext.set(0L, true, true, false);
                }
            } else {
                TenantContext.set(current.getTenantId(), false, false, false);
            }
            chain.doFilter(request, response);
        } catch (BizException e) {
            writeUnauthorized(response, e.getMessage());
        } catch (Exception e) {
            writeUnauthorized(response, "登录已失效，请重新登录");
        } finally {
            AuthContext.clear();
            TenantContext.clear();
        }
    }

    private LoginUser loadPlatformUser(LoginUser tokenUser) {
        SysPlatformUser user = platformUserMapper.selectById(tokenUser.getUserId());
        if (user == null || !"NORMAL".equals(user.getStatus())) {
            throw new BizException(401, "账号不存在或已停用");
        }
        String roleCode = user.getRoleCode() == null || user.getRoleCode().isBlank()
                ? "PLATFORM_OPERATOR" : user.getRoleCode();
        return new LoginUser(user.getId(), 0L, "PLATFORM", user.getUsername(),
                user.getRealName(), roleCode, "PLATFORM", true);
    }

    private LoginUser loadTenantUser(LoginUser tokenUser) {
        SysTenantUser user = tenantUserMapper.selectByIdIgnoreTenant(tokenUser.getUserId());
        if (user == null || !"NORMAL".equals(user.getStatus())) {
            throw new BizException(401, "账号不存在或已停用");
        }
        if (tokenUser.getTenantId() == null || !tokenUser.getTenantId().equals(user.getTenantId())) {
            throw new BizException(401, "登录租户已变更，请重新登录");
        }
        SysTenant tenant = tenantMapper.selectById(user.getTenantId());
        if (tenant == null) {
            throw new BizException(401, "租户不存在");
        }
        if (!"NORMAL".equals(tenant.getStatus()) && !"TRIAL".equals(tenant.getStatus())) {
            throw new BizException(401, "租户已停用，请联系平台管理员");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(401, "租户已到期，请联系平台管理员续期");
        }
        String roleCode = user.getRoleCode() == null || user.getRoleCode().isBlank()
                ? "STAFF" : user.getRoleCode();
        return new LoginUser(user.getId(), user.getTenantId(), tenant.getCode(),
                user.getUsername(), user.getRealName(), roleCode, user.getUserType(), false);
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
