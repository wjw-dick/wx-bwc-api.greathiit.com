package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.module.system.entity.SysLanguage;
import com.greathiit.bwc.module.system.entity.SysMenu;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.entity.SysTenantConfig;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import com.greathiit.bwc.module.system.mapper.SysLanguageMapper;
import com.greathiit.bwc.module.system.mapper.SysMenuMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantConfigMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.security.LoginUser;
import com.greathiit.bwc.security.PermissionService;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 租户与菜单配置读取。
 */
@Service
@RequiredArgsConstructor
public class TenantService {

    private final SysTenantMapper tenantMapper;
    private final SysMenuMapper menuMapper;
    private final SysTenantConfigMapper configMapper;
    private final SysLanguageMapper languageMapper;
    private final PermissionService permissionService;

    public SysTenant requireTenant(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        return tenant;
    }

    public void checkTenantUsable(SysTenant tenant) {
        if (!"NORMAL".equals(tenant.getStatus()) && !"TRIAL".equals(tenant.getStatus())) {
            throw BizException.forbidden("租户已停用，请联系平台管理员");
        }
        if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
            throw BizException.forbidden("租户已到期，请联系平台管理员续期");
        }
    }

    public List<Map<String, Object>> menuTree(String platform, Long tenantId) {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPlatform, platform)
                .eq(SysMenu::getVisible, 1)
                .in(SysMenu::getTenantId, List.of(0L, tenantId))
                .orderByAsc(SysMenu::getSort));
        return buildTree(menus);
    }

    /** C 端角色在 PC 后台只保留自助页面，避免出现管理列表与审批入口 */
    private static final java.util.Set<String> SELF_MENU_PATHS = java.util.Set.of(
            "/dashboard",
            "/visitor", "/visitor/appointments", "/visitor/late-return",
            "/vehicle", "/vehicle/appointments",
            "/lost", "/lost/claims",
            "/education", "/education/courses", "/education/exams",
            "/report", "/report/applies", "/report/monitor-access",
            "/device", "/device/repairs",
            "/emergency", "/emergency/plans", "/emergency/events",
            "/logistics", "/logistics/feedback"
    );

    /** 按当前用户有效权限过滤菜单树，目录下无可见子项时一并隐藏 */
    public List<Map<String, Object>> menuTree(String platform, Long tenantId, LoginUser user) {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPlatform, platform)
                .eq(SysMenu::getVisible, 1)
                .in(SysMenu::getTenantId, List.of(0L, tenantId))
                .orderByAsc(SysMenu::getSort));
        boolean selfRole = user != null && !user.isPlatform()
                && java.util.Set.of("STAFF", "STUDENT", "VISITEE").contains(user.getRoleCode());
        java.util.Map<Long, SysMenu> byId = new java.util.HashMap<>();
        menus.forEach(menu -> byId.put(menu.getId(), menu));
        java.util.Set<Long> allowedIds = new java.util.HashSet<>();
        for (SysMenu menu : menus) {
            String path = menu.getPath();
            // 平台专属菜单即使被误授权给租户角色也不下发
            if (user != null && !user.isPlatform() && path != null
                    && (path.equals("/platform") || path.startsWith("/platform/")
                    || path.equals("/report-center/platform"))) {
                continue;
            }
            if (!permissionService.canSeeMenu(user, menu.getPermission())) {
                continue;
            }
            if (selfRole && (path == null || !SELF_MENU_PATHS.contains(path))) {
                continue;
            }
            allowedIds.add(menu.getId());
            SysMenu parent = byId.get(menu.getParentId());
            while (parent != null && allowedIds.add(parent.getId())) {
                parent = byId.get(parent.getParentId());
            }
        }
        List<SysMenu> visible = new ArrayList<>();
        for (SysMenu menu : menus) {
            if (allowedIds.contains(menu.getId())) {
                visible.add(menu);
            }
        }
        List<Map<String, Object>> tree = buildTree(visible);
        pruneEmptyDirs(tree);
        return tree;
    }

    /** 角色授权用的菜单目录：含按钮级权限，过滤平台专属路径 */
    public List<Map<String, Object>> roleMenuTree(Long tenantId) {
        long tid = tenantId == null ? 0L : tenantId;
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPlatform, "admin")
                .in(SysMenu::getTenantId, List.of(0L, tid))
                .orderByAsc(SysMenu::getSort));
        List<SysMenu> filtered = new ArrayList<>();
        for (SysMenu menu : menus) {
            String path = menu.getPath();
            if (path != null && (path.equals("/platform") || path.startsWith("/platform/")
                    || path.equals("/report-center/platform"))) {
                continue;
            }
            filtered.add(menu);
        }
        return buildTree(filtered);
    }

    public List<SysMenu> menuList(String platform, Long tenantId) {
        return menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPlatform, platform)
                .in(SysMenu::getTenantId, List.of(0L, tenantId))
                .orderByAsc(SysMenu::getSort));
    }

    private List<Map<String, Object>> buildTree(List<SysMenu> menus) {
        Map<Long, Map<String, Object>> nodes = new HashMap<>();
        for (SysMenu menu : menus) {
            Map<String, Object> node = toNode(menu);
            nodes.put(menu.getId(), node);
        }
        List<Map<String, Object>> roots = new ArrayList<>();
        for (SysMenu menu : menus) {
            Map<String, Object> node = nodes.get(menu.getId());
            Map<String, Object> parent = nodes.get(menu.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children =
                        (List<Map<String, Object>>) parent.computeIfAbsent("children", k -> new ArrayList<>());
                children.add(node);
            }
        }
        roots.sort(Comparator.comparing(o -> ((Integer) o.getOrDefault("sort", 0))));
        return roots;
    }

    private void pruneEmptyDirs(List<Map<String, Object>> nodes) {
        if (nodes == null) {
            return;
        }
        java.util.Iterator<Map<String, Object>> it = nodes.iterator();
        while (it.hasNext()) {
            Map<String, Object> node = it.next();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
            if (children != null) {
                pruneEmptyDirs(children);
                if (children.isEmpty() && (node.get("path") == null || "".equals(node.get("path")))) {
                    it.remove();
                    continue;
                }
            }
            Object path = node.get("path");
            Object childrenRaw = node.get("children");
            if ((path == null || "".equals(path)) && childrenRaw == null
                    && (node.get("permission") == null || "".equals(node.get("permission")))) {
                it.remove();
            }
        }
    }

    private Map<String, Object> toNode(SysMenu menu) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", menu.getId());
        node.put("name", menu.getName());
        node.put("nameEn", menu.getNameEn());
        node.put("path", menu.getPath());
        node.put("icon", menu.getIcon());
        node.put("sort", menu.getSort());
        node.put("permission", menu.getPermission());
        node.put("menuType", menu.getMenuType());
        node.put("configJson", menu.getConfigJson());
        return node;
    }

    public Map<String, String> tenantConfig(Long tenantId) {
        List<SysTenantConfig> list = configMapper.selectList(
                new LambdaQueryWrapper<SysTenantConfig>().eq(SysTenantConfig::getTenantId, tenantId));
        Map<String, String> result = new HashMap<>();
        list.forEach(c -> result.put(c.getConfigKey(), c.getConfigValue()));
        return result;
    }

    public List<SysLanguage> enabledLanguages() {
        return languageMapper.selectList(new LambdaQueryWrapper<SysLanguage>()
                .eq(SysLanguage::getEnabled, 1)
                .orderByAsc(SysLanguage::getSort));
    }

    public Long currentTenantIdOrOne() {
        Long id = TenantContext.getTenantId();
        return id == null || id == 0 ? 1L : id;
    }
}
