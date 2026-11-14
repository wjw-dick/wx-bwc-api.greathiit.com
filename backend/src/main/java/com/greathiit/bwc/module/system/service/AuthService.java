package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.module.system.dto.ChangePasswordRequest;
import com.greathiit.bwc.module.system.dto.LoginRequest;
import com.greathiit.bwc.module.system.entity.SysPlatformUser;
import com.greathiit.bwc.module.system.entity.SysRole;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import com.greathiit.bwc.module.system.mapper.SysPlatformUserMapper;
import com.greathiit.bwc.module.system.mapper.SysRoleMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantUserMapper;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.JwtUtil;
import com.greathiit.bwc.security.LoginUser;
import com.greathiit.bwc.security.PermissionService;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录、微信登录绑定、修改密码。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysTenantUserMapper tenantUserMapper;
    private final SysPlatformUserMapper platformUserMapper;
    private final SysTenantMapper tenantMapper;
    private final SysRoleMapper roleMapper;
    private final TenantService tenantService;
    private final WeChatService weChatService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final PermissionService permissionService;

    public Map<String, Object> login(LoginRequest request) {
        if ("platform".equalsIgnoreCase(request.getLoginType())) {
            return platformLogin(request);
        }
        return tenantLogin(request);
    }

    private Map<String, Object> platformLogin(LoginRequest request) {
        SysPlatformUser user = platformUserMapper.selectOne(
                new LambdaQueryWrapper<SysPlatformUser>().eq(SysPlatformUser::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!"NORMAL".equals(user.getStatus())) {
            throw BizException.forbidden("账号已停用");
        }
        user.setLastLoginTime(LocalDateTime.now());
        platformUserMapper.updateById(user);
        String roleCode = user.getRoleCode() == null || user.getRoleCode().isBlank()
                ? "PLATFORM_OPERATOR" : user.getRoleCode();
        LoginUser loginUser = new LoginUser(user.getId(), 0L, "PLATFORM", user.getUsername(),
                user.getRealName(), roleCode, "PLATFORM", true);
        auditService.record("auth", "LOGIN", "platform", "平台登录");
        return buildResult(loginUser, null);
    }

    private Map<String, Object> tenantLogin(LoginRequest request) {
        List<SysTenantUser> users = tenantUserMapper.selectByUsernameIgnoreTenant(request.getUsername());
        SysTenantUser user = users.stream()
                .filter(u -> matchTenant(u, request.getTenantCode()))
                .findFirst()
                .orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!"NORMAL".equals(user.getStatus())) {
            throw BizException.forbidden("账号已停用");
        }
        SysTenant tenant = tenantService.requireTenant(user.getTenantId());
        tenantService.checkTenantUsable(tenant);
        // 登录请求尚未建立租户上下文；先切到账号所属租户，避免角色查询/登录时间更新被
        // 租户插件按 tenant_id=0 过滤（历史 tenantId=0 故障根因）。
        Long prevTenant = TenantContext.getTenantId();
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        boolean prevPlatform = TenantContext.isPlatform();
        boolean prevReadOnly = TenantContext.isReadOnly();
        TenantContext.set(user.getTenantId(), false, false, false);
        try {
            String roleCode = user.getRoleCode();
            if (roleCode == null && user.getRoleId() != null) {
                SysRole role = roleMapper.selectById(user.getRoleId());
                roleCode = role == null ? "STAFF" : role.getRoleCode();
            }
            if (roleCode == null || roleCode.isBlank()) {
                roleCode = "STAFF";
            }
            user.setLastLoginTime(LocalDateTime.now());
            tenantUserMapper.updateById(user);
            LoginUser loginUser = new LoginUser(user.getId(), user.getTenantId(), tenant.getCode(),
                    user.getUsername(), user.getRealName(), roleCode,
                    user.getUserType(), false);
            auditService.record("auth", "LOGIN", "tenant", "租户登录");
            return buildResult(loginUser, tenant);
        } finally {
            TenantContext.set(prevTenant, prevIgnore, prevPlatform, prevReadOnly);
        }
    }

    private boolean matchTenant(SysTenantUser user, String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            return true;
        }
        SysTenant tenant = tenantMapper.selectById(user.getTenantId());
        return tenant != null && tenantCode.equalsIgnoreCase(tenant.getCode());
    }

    private Map<String, Object> buildResult(LoginUser loginUser, SysTenant tenant) {
        String token = jwtUtil.create(loginUser);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        Map<String, Object> user = new HashMap<>();
        user.put("id", loginUser.getUserId());
        user.put("username", loginUser.getUsername());
        user.put("realName", loginUser.getRealName());
        user.put("roleCode", loginUser.getRoleCode());
        user.put("userType", loginUser.getUserType());
        user.put("tenantId", loginUser.getTenantId());
        user.put("platform", loginUser.isPlatform());
        result.put("user", user);
        if (tenant != null) {
            Map<String, Object> t = new HashMap<>();
            t.put("id", tenant.getId());
            t.put("name", tenant.getName());
            t.put("code", tenant.getCode());
            t.put("type", tenant.getType());
            t.put("status", tenant.getStatus());
            t.put("expireTime", tenant.getExpireTime());
            result.put("tenant", t);
        }
        result.put("menus", tenantService.menuTree("admin", loginUser.isPlatform() ? 0L : loginUser.getTenantId(), loginUser));
        result.put("permissions", permissionService.effectivePermissions(loginUser));
        result.put("languages", tenantService.enabledLanguages());
        return result;
    }

    public Map<String, Object> me() {
        LoginUser current = AuthContext.require();
        Map<String, Object> result = new HashMap<>();
        result.put("user", current);
        result.put("tenant", tenantService.tenantConfig(current.getTenantId()));
        result.put("menus", tenantService.menuTree("admin",
                current.isPlatform() ? 0L : current.getTenantId(), current));
        result.put("permissions", permissionService.effectivePermissions(current));
        return result;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        LoginUser current = AuthContext.require();
        if (current.isPlatform()) {
            SysPlatformUser user = platformUserMapper.selectById(current.getUserId());
            if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BizException(400, "旧密码不正确");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            platformUserMapper.updateById(user);
        } else {
            SysTenantUser user = tenantUserMapper.selectById(current.getUserId());
            if (user == null || !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
                throw new BizException(400, "旧密码不正确");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            tenantUserMapper.updateById(user);
        }
        auditService.record("auth", "CHANGE_PASSWORD", current.getUsername(), "自助修改密码");
    }

    public Map<String, Object> wxLogin(String code, String appType) {
        Map<String, String> session = weChatService.code2Session(appType, code);
        String openid = session.get("openid");
        String unionid = session.get("unionid");
        SysTenantUser user = null;
        if (unionid != null) {
            user = tenantUserMapper.selectByUnionidIgnoreTenant(unionid);
        }
        if (user == null && openid != null) {
            user = tenantUserMapper.selectByOpenidIgnoreTenant(openid);
        }
        Map<String, Object> result = new HashMap<>();
        if (user == null) {
            result.put("bound", false);
            result.put("openid", openid);
            result.put("unionid", unionid);
            result.put("message", "微信账号尚未绑定本校档案，请使用账号密码绑定");
            return result;
        }
        if (!"NORMAL".equals(user.getStatus())) {
            throw BizException.forbidden("账号已停用");
        }
        // 未登录入口（公开路径）TenantContext 为 0，切到该用户所属租户后再做更新与租户校验
        Long prevTenant = TenantContext.getTenantId();
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        boolean prevPlatform = TenantContext.isPlatform();
        boolean prevReadOnly = TenantContext.isReadOnly();
        TenantContext.set(user.getTenantId(), false, prevPlatform, prevReadOnly);
        try {
            if (openid != null) {
                user.setOpenid(openid);
            }
            if (unionid != null) {
                user.setUnionid(unionid);
            }
            user.setLastLoginTime(LocalDateTime.now());
            tenantUserMapper.updateById(user);
            SysTenant tenant = tenantService.requireTenant(user.getTenantId());
            tenantService.checkTenantUsable(tenant);
            String roleCode = user.getRoleCode();
            if ((roleCode == null || roleCode.isBlank()) && user.getRoleId() != null) {
                SysRole role = roleMapper.selectById(user.getRoleId());
                roleCode = role == null ? "STAFF" : role.getRoleCode();
            }
            if (roleCode == null || roleCode.isBlank()) {
                roleCode = "STAFF";
            }
            LoginUser loginUser = new LoginUser(user.getId(), user.getTenantId(), tenant.getCode(),
                    user.getUsername(), user.getRealName(), roleCode, user.getUserType(), false);
            result.put("bound", true);
            result.putAll(buildResult(loginUser, tenant));
            auditService.record("auth", "WX_LOGIN", "wechat", "微信授权登录");
            return result;
        } finally {
            TenantContext.set(prevTenant, prevIgnore, prevPlatform, prevReadOnly);
        }
    }

    @Transactional
    public Map<String, Object> wxBind(String username, String password, String code, String appType,
                                      String tenantCode) {
        Map<String, String> session = weChatService.code2Session(appType, code);
        List<SysTenantUser> users = tenantUserMapper.selectByUsernameIgnoreTenant(username);
        SysTenantUser user = users.stream()
                .filter(u -> matchTenant(u, tenantCode))
                .findFirst()
                .orElse(null);
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        if (!"NORMAL".equals(user.getStatus())) {
            throw BizException.forbidden("账号已停用");
        }
        SysTenant tenant = tenantService.requireTenant(user.getTenantId());
        tenantService.checkTenantUsable(tenant);
        user.setOpenid(session.get("openid"));
        user.setUnionid(session.get("unionid"));
        Long prevTenant = TenantContext.getTenantId();
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        boolean prevPlatform = TenantContext.isPlatform();
        boolean prevReadOnly = TenantContext.isReadOnly();
        TenantContext.set(user.getTenantId(), false, prevPlatform, prevReadOnly);
        try {
            String roleCode = user.getRoleCode() == null || user.getRoleCode().isBlank()
                    ? "STAFF" : user.getRoleCode();
            user.setLastLoginTime(LocalDateTime.now());
            tenantUserMapper.updateById(user);
            LoginUser loginUser = new LoginUser(user.getId(), user.getTenantId(), tenant.getCode(),
                    user.getUsername(), user.getRealName(), roleCode, user.getUserType(), false);
            auditService.record("auth", "WX_BIND", user.getUsername(), "绑定微信 UnionID");
            return buildResult(loginUser, tenant);
        } finally {
            TenantContext.set(prevTenant, prevIgnore, prevPlatform, prevReadOnly);
        }
    }
}
