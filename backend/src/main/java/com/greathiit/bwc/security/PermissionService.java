package com.greathiit.bwc.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.module.system.entity.SysMenu;
import com.greathiit.bwc.module.system.entity.SysRole;
import com.greathiit.bwc.module.system.entity.SysRoleMenu;
import com.greathiit.bwc.module.system.mapper.SysMenuMapper;
import com.greathiit.bwc.module.system.mapper.SysRoleMapper;
import com.greathiit.bwc.module.system.mapper.SysRoleMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 两级 RBAC 权限判定（白名单 + 默认拒绝）。
 * 权限来源优先取 sys_role_menu → sys_menu.permission；角色未配置时回退内置矩阵。
 * 平台：SYSTEM / PLATFORM_OPERATOR；租户：TENANT_ADMIN / CAPTAIN / GUARD / STAFF / STUDENT / LEADER / VISITEE。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    /** 业务模块（管理端可见） */
    private static final Set<String> BUSINESS = Set.of(
            "dashboard", "visitor", "vehicle", "plate", "event", "fire", "hazard", "patrol",
            "lost", "education", "emergency", "device", "logistics", "feedback", "report");

    /** 租户系统管理模块 */
    private static final Set<String> TENANT_SYSTEM = Set.of("system", "dict", "audit");

    /** 平台级模块（仅平台账号） */
    private static final Set<String> PLATFORM_MODULES = Set.of(
            "platform", "tenant", "menu", "param", "i18n", "storage", "wechat");

    /** 保安/队长可写的业务模块 */
    private static final Set<String> GUARD_WRITE = Set.of(
            "visitor", "vehicle", "plate", "fire", "hazard", "event", "patrol", "lost",
            "device", "emergency", "logistics", "feedback", "report");

    /** C 端（教职工/学生/被访人）可写的业务模块 */
    private static final Set<String> SELF_WRITE = Set.of(
            "visitor", "vehicle", "lost", "education", "report", "device", "feedback", "emergency");

    /** C 端可读的业务模块 */
    private static final Set<String> SELF_READ = Set.of(
            "dashboard", "visitor", "vehicle", "lost", "education", "report", "device", "feedback", "emergency");

    /** 平台可写的模块 */
    private static final Set<String> PLATFORM_WRITE = Set.of(
            "platform", "tenant", "menu", "param", "i18n", "storage", "wechat", "audit",
            "dict", "system");

    /** 平台运营只读权限：不得管理租户、平台账号、参数、菜单、存储与微信配置 */
    private static final Set<String> OPERATOR_READ = Set.of(
            "platform:read", "tenant:read", "audit:read", "dashboard:read");

    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;

    /** 角色权限缓存：tenantId:roleCode -> permissions */
    private final Map<String, Set<String>> cache = new ConcurrentHashMap<>();

    public boolean has(String permission) {
        return has(AuthContext.get(), permission);
    }

    /** 指定用户的权限判定（登录态写上下文前也可用，例如返回登录菜单树） */
    public boolean has(LoginUser user, String permission) {
        if (user == null || permission == null || permission.isBlank()) {
            return false;
        }
        String[] parts = permission.split(":", 2);
        String module = normalize(parts[0]);
        String action = parts.length > 1 ? parts[1] : "read";
        boolean write = !"read".equals(action);

        if (user.isPlatform()) {
            if (isSystemRole(user)) {
                if (!write) {
                    return true;
                }
                // 下钻租户或只读模式下，平台超管同样禁止写操作
                if (TenantContext.isReadOnly() || TenantContext.getTenantId() != 0L) {
                    return false;
                }
                return PLATFORM_MODULES.contains(module) || PLATFORM_WRITE.contains(module);
            }
            // 平台运营：只读，且只能访问租户/看板/审计等获授权范围
            return !write && OPERATOR_READ.contains(module + ":" + action);
        }

        if (PLATFORM_MODULES.contains(module)) {
            return false;
        }
        Set<String> granted = permissionsOf(user);
        if (granted.isEmpty()) {
            return false;
        }
        if (granted.contains(permission)) {
            return true;
        }
        // user/role 的写权限等价于 system:write（菜单表与接口注解的映射差异）
        if (write && "system".equals(module)
                && (granted.contains("user:write") || granted.contains("role:write"))) {
            return true;
        }
        // 拥有 module:write 自动具备 module:read
        return !write && granted.contains(module + ":write");
    }

    /** 当前用户可见菜单判定：权限为空（目录）先放行，由父节点按子节点裁剪 */
    public boolean canSeeMenu(LoginUser user, String menuPermission) {
        if (user == null) {
            return false;
        }
        if (menuPermission == null || menuPermission.isBlank()) {
            return true;
        }
        return has(user, menuPermission);
    }

    /** 平台超管返回通配符，平台运营返回只读白名单；租户角色返回真实授权集合 */
    public Set<String> effectivePermissions(LoginUser user) {
        if (user == null) {
            return Set.of();
        }
        if (user.isPlatform()) {
            return isSystemRole(user) ? Set.of("*") : OPERATOR_READ;
        }
        return permissionsOf(user);
    }

    public boolean isSystemRole(LoginUser user) {
        return user != null && user.isPlatform()
                && "SYSTEM".equalsIgnoreCase(user.getRoleCode());
    }

    public Set<String> permissionsOf(LoginUser user) {
        if (user == null) {
            return Set.of();
        }
        if (user.isPlatform()) {
            return Set.of();
        }
        String key = user.getTenantId() + ":" + user.getRoleCode();
        Set<String> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Set<String> loaded = loadFromDb(user);
        if (loaded.isEmpty()) {
            // 仅当全局尚未配置任何角色菜单（如未执行迁移）时才回退内置矩阵；
            // 已配置过角色菜单则空授权=默认拒绝，避免“清空权限后反而放大”。
            Long total = roleMenuMapper.selectCount(null);
            if (total == null || total == 0) {
                loaded = fallback(user.getRoleCode());
            }
        }
        cache.put(key, loaded);
        return loaded;
    }

    /** 配置变更后清理缓存，下一次请求立即生效 */
    public void evictAll() {
        cache.clear();
    }

    public void require(String permission) {
        if (!has(permission)) {
            throw BizException.forbidden("越权访问：" + permission);
        }
    }

    public void requirePlatform() {
        LoginUser user = AuthContext.get();
        if (user == null || !user.isPlatform()) {
            throw BizException.forbidden("仅平台账号可操作");
        }
        if (TenantContext.isReadOnly()) {
            throw BizException.forbidden("下钻租户为只读模式");
        }
    }

    /** 高风险平台写操作：仅 system，平台运营也拒绝 */
    public void requireSystem() {
        if (!isSystemRole(AuthContext.get())) {
            throw BizException.forbidden("仅平台超级管理员可操作");
        }
        if (TenantContext.isReadOnly()) {
            throw BizException.forbidden("下钻租户为只读模式");
        }
    }

    private Set<String> loadFromDb(LoginUser user) {
        // 登录请求尚未写入 TenantContext（tenant_id 默认 0），必须绕过租户插件并显式按用户租户过滤，
        // 否则会把空结果缓存成回退权限。
        boolean prevIgnore = TenantContext.isIgnoreTenant();
        TenantContext.setIgnoreTenant(true);
        try {
            SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getTenantId, user.getTenantId())
                    .eq(SysRole::getRoleCode, user.getRoleCode())
                    .last("limit 1"));
            if (role == null) {
                return Set.of();
            }
            List<SysRoleMenu> links = roleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getTenantId, user.getTenantId())
                    .eq(SysRoleMenu::getRoleId, role.getId()));
            if (links.isEmpty()) {
                return Set.of();
            }
            List<Long> menuIds = new ArrayList<>();
            for (SysRoleMenu link : links) {
                menuIds.add(link.getMenuId());
            }
            List<SysMenu> menus = menuMapper.selectBatchIds(menuIds);
            java.util.HashSet<String> perms = new java.util.HashSet<>();
            for (SysMenu menu : menus) {
                if (menu.getPermission() != null && !menu.getPermission().isBlank()) {
                    perms.add(menu.getPermission());
                }
            }
            return perms;
        } finally {
            TenantContext.setIgnoreTenant(prevIgnore);
        }
    }

    /** 角色未配置菜单时回退内置矩阵（默认拒绝原则不变） */
    private Set<String> fallback(String roleCode) {
        java.util.HashSet<String> perms = new java.util.HashSet<>();
        if (roleCode == null) {
            return perms;
        }
        switch (roleCode) {
            case "TENANT_ADMIN" -> {
                addAll(perms, BUSINESS, true);
                addAll(perms, TENANT_SYSTEM, false);
                perms.add("dict:write");
                perms.add("system:write");
            }
            case "CAPTAIN", "GUARD" -> addAll(perms, GUARD_WRITE, true);
            case "LEADER" -> addReadOnly(perms, BUSINESS);
            case "STAFF", "STUDENT", "VISITEE" -> {
                addReadOnly(perms, SELF_READ);
                addAll(perms, SELF_WRITE, true);
                if (!"STUDENT".equals(roleCode)) {
                    perms.add("visitor:approve");
                }
            }
            default -> {
                return perms;
            }
        }
        return perms;
    }

    private void addAll(Set<String> target, Set<String> modules, boolean write) {
        for (String module : modules) {
            target.add(module + ":read");
            if (write) {
                target.add(module + ":write");
            }
        }
    }

    private void addReadOnly(Set<String> target, Set<String> modules) {
        for (String module : modules) {
            target.add(module + ":read");
        }
    }

    /** 菜单表存在 user/role 与注解 system 的差异，统一归并 */
    private String normalize(String module) {
        if ("user".equals(module) || "role".equals(module) || "users".equals(module)) {
            return "system";
        }
        return module;
    }
}
