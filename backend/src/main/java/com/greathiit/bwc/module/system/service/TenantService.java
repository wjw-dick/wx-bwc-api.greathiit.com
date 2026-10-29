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
