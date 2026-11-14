package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.module.system.entity.SysPlatformUser;
import com.greathiit.bwc.module.system.entity.SysMenu;
import com.greathiit.bwc.module.system.entity.SysRole;
import com.greathiit.bwc.module.system.entity.SysRoleMenu;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.entity.SysTenantConfig;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import com.greathiit.bwc.module.system.mapper.SysMenuMapper;
import com.greathiit.bwc.module.system.mapper.SysPlatformUserMapper;
import com.greathiit.bwc.module.system.mapper.SysRoleMapper;
import com.greathiit.bwc.module.system.mapper.SysRoleMenuMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantConfigMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台端：租户开通/停用/续期/配额、全部学校看板下钻、平台账号。
 */
@Service
@RequiredArgsConstructor
public class PlatformService {

    private final SysTenantMapper tenantMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final SysTenantConfigMapper tenantConfigMapper;
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysPlatformUserMapper platformUserMapper;
    private final StatsService statsService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public IPage<SysTenant> page(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysTenant::getName, keyword)
                    .or().like(SysTenant::getCode, keyword)
                    .or().like(SysTenant::getContact, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(SysTenant::getStatus, status);
        }
        wrapper.orderByAsc(SysTenant::getId);
        return tenantMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public SysTenant create(SysTenant tenant) {
        if (tenant.getName() == null || tenant.getName().isBlank()) {
            throw new BizException(400, "学校名称不能为空");
        }
        if (tenant.getCode() == null || tenant.getCode().isBlank()) {
            throw new BizException(400, "租户编码不能为空");
        }
        Long exists = tenantMapper.selectCount(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getCode, tenant.getCode()));
        if (exists != null && exists > 0) {
            throw new BizException(400, "租户编码已存在");
        }
        tenant.setId(null);
        tenant.setTenantId(0L);
        tenant.setStatus(tenant.getStatus() == null ? "TRIAL" : tenant.getStatus());
        tenant.setOpenTime(tenant.getOpenTime() == null ? LocalDateTime.now() : tenant.getOpenTime());
        if (tenant.getExpireTime() == null) {
            tenant.setExpireTime(LocalDateTime.now().plusDays(30));
        }
        if (tenant.getUserQuota() == null) {
            tenant.setUserQuota(100);
        }
        tenantMapper.insert(tenant);
        seedTenantData(tenant);
        auditService.record("tenant", "CREATE", tenant.getCode(), "开通租户：" + tenant.getName());
        return tenant;
    }

    private void seedTenantData(SysTenant tenant) {
        String[][] roles = {
                {"TENANT_ADMIN", "租户管理员", "TENANT"},
                {"CAPTAIN", "保安队长", "TENANT"},
                {"GUARD", "保安", "TENANT"},
                {"STAFF", "教职工", "SELF"},
                {"STUDENT", "学生", "SELF"},
                {"LEADER", "校领导", "TENANT_READ"},
                {"VISITEE", "被访人", "SELF"}
        };
        for (String[] role : roles) {
            Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                    .eq(SysRole::getTenantId, tenant.getId())
                    .eq(SysRole::getRoleCode, role[0]));
            if (count != null && count > 0) {
                continue;
            }
            SysRole entity = new SysRole();
            entity.setTenantId(tenant.getId());
            entity.setRoleCode(role[0]);
            entity.setRoleName(role[1]);
            entity.setDataScope(role[2]);
            roleMapper.insert(entity);
            seedRoleMenus(tenant.getId(), entity);
        }
        SysTenantUser admin = new SysTenantUser();
        admin.setTenantId(tenant.getId());
        admin.setUsername(tenant.getCode().toLowerCase() + "_admin");
        admin.setPassword(passwordEncoder.encode("Admin@123"));
        admin.setRealName(tenant.getContact() == null ? tenant.getName() + "管理员" : tenant.getContact());
        admin.setRoleCode("TENANT_ADMIN");
        admin.setUserType("ADMIN");
        admin.setDept("保卫处");
        admin.setStatus("NORMAL");
        tenantUserMapper.insert(admin);

        SysTenantConfig config = new SysTenantConfig();
        config.setTenantId(tenant.getId());
        config.setConfigKey("school_name");
        config.setConfigValue(tenant.getName());
        config.setEncrypted(0);
        config.setRemark("学校名称");
        tenantConfigMapper.insert(config);
    }

    /** 新租户按内置角色策略初始化菜单授权，避免角色无任何权限 */
    private void seedRoleMenus(Long tenantId, SysRole role) {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPlatform, "admin"));
        for (SysMenu menu : menus) {
            if (!allowMenu(role.getRoleCode(), menu)) {
                continue;
            }
            SysRoleMenu link = new SysRoleMenu();
            link.setTenantId(tenantId);
            link.setRoleId(role.getId());
            link.setMenuId(menu.getId());
            roleMenuMapper.insert(link);
        }
    }

    private boolean allowMenu(String roleCode, SysMenu menu) {
        String permission = menu.getPermission();
        String path = menu.getPath();
        if (permission == null || permission.isBlank()) {
            return false;
        }
        if (path != null && (path.equals("/platform") || path.startsWith("/platform/")
                || path.equals("/report-center/platform"))) {
            return false;
        }
        String module = permission.split(":", 2)[0];
        boolean read = permission.endsWith(":read");
        boolean business = List.of("visitor", "vehicle", "plate", "fire", "hazard", "event",
                "patrol", "lost", "device", "emergency", "logistics", "feedback", "report")
                .contains(module);
        return switch (roleCode) {
            case "TENANT_ADMIN" -> !List.of("platform", "tenant", "menu", "param",
                    "i18n", "storage", "wechat").contains(module);
            case "CAPTAIN", "GUARD" -> "dashboard".equals(module) || business;
            case "LEADER" -> "dashboard".equals(module) || (read && business);
            case "STAFF", "VISITEE", "STUDENT" -> {
                if ("dashboard".equals(module)) {
                    yield true;
                }
                if ("STUDENT".equals(roleCode) && "visitor:approve".equals(permission)) {
                    yield false;
                }
                if (path != null && path.startsWith("/logistics") && !"/logistics/feedback".equals(path)) {
                    yield false;
                }
                yield List.of("visitor", "vehicle", "lost", "education", "report",
                        "device", "feedback", "emergency").contains(module);
            }
            default -> false;
        };
    }

    @Transactional
    public SysTenant update(Long id, SysTenant patch) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        if (patch.getName() != null) {
            tenant.setName(patch.getName());
        }
        if (patch.getType() != null) {
            tenant.setType(patch.getType());
        }
        if (patch.getStatus() != null) {
            tenant.setStatus(patch.getStatus());
        }
        if (patch.getOpenTime() != null) {
            tenant.setOpenTime(patch.getOpenTime());
        }
        if (patch.getExpireTime() != null) {
            tenant.setExpireTime(patch.getExpireTime());
        }
        if (patch.getUserQuota() != null) {
            tenant.setUserQuota(patch.getUserQuota());
        }
        if (patch.getContact() != null) {
            tenant.setContact(patch.getContact());
        }
        if (patch.getPhone() != null) {
            tenant.setPhone(patch.getPhone());
        }
        if (patch.getRemark() != null) {
            tenant.setRemark(patch.getRemark());
        }
        tenantMapper.updateById(tenant);
        auditService.record("tenant", "UPDATE", tenant.getCode(), "修改租户信息");
        return tenant;
    }

    @Transactional
    public SysTenant changeStatus(Long id, String status) {
        if (!List.of("TRIAL", "NORMAL", "DISABLED").contains(status)) {
            throw new BizException(400, "非法租户状态");
        }
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        tenant.setStatus(status);
        tenantMapper.updateById(tenant);
        auditService.record("tenant", "STATUS", tenant.getCode(), "租户状态变更为：" + status);
        return tenant;
    }

    @Transactional
    public SysTenant renew(Long id, LocalDateTime expireTime, Integer quota) {
        SysTenant tenant = tenantMapper.selectById(id);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        tenant.setExpireTime(expireTime == null ? LocalDateTime.now().plusYears(1) : expireTime);
        if (quota != null && quota > 0) {
            tenant.setUserQuota(quota);
        }
        if ("DISABLED".equals(tenant.getStatus())) {
            tenant.setStatus("NORMAL");
        }
        tenantMapper.updateById(tenant);
        auditService.record("tenant", "RENEW", tenant.getCode(), "续期至：" + tenant.getExpireTime());
        return tenant;
    }

    public Map<String, Object> dashboard() {
        List<Map<String, Object>> rows = statsService.crossSchool();
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("schools", rows.size());
        summary.put("normalSchools", rows.stream().filter(r -> "NORMAL".equals(r.get("status"))).count());
        summary.put("trialSchools", rows.stream().filter(r -> "TRIAL".equals(r.get("status"))).count());
        summary.put("expiredSchools", rows.stream().filter(r -> {
            Object expire = r.get("expireTime");
            return expire instanceof LocalDateTime time && time.isBefore(LocalDateTime.now());
        }).count());
        summary.put("users", rows.stream().mapToLong(r -> ((Number) r.getOrDefault("userCount", 0)).longValue()).sum());
        summary.put("visitors", rows.stream().mapToLong(r -> ((Number) r.getOrDefault("visitorCount", 0)).longValue()).sum());
        summary.put("events", rows.stream().mapToLong(r -> ((Number) r.getOrDefault("eventCount", 0)).longValue()).sum());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("schools", rows);
        result.put("generatedAt", LocalDateTime.now());
        return result;
    }

    public Map<String, Object> tenantDetail(Long tenantId) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw BizException.notFound("租户不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tenant", tenant);
        result.put("config", tenantConfigMapper.selectList(new LambdaQueryWrapper<SysTenantConfig>()
                .eq(SysTenantConfig::getTenantId, tenantId)));
        result.put("userCount", tenantUserMapper.selectCount(new LambdaQueryWrapper<SysTenantUser>()
                .eq(SysTenantUser::getTenantId, tenantId)));
        result.put("stats", statsService.crossSchool().stream()
                .filter(r -> tenantId.equals(r.get("tenantId"))).findFirst().orElse(Map.of()));
        return result;
    }

    public List<SysPlatformUser> platformUsers() {
        return platformUserMapper.selectList(new LambdaQueryWrapper<SysPlatformUser>()
                .orderByAsc(SysPlatformUser::getId));
    }

    @Transactional
    public SysPlatformUser createPlatformUser(SysPlatformUser user, String rawPassword) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BizException(400, "用户名不能为空");
        }
        Long exists = platformUserMapper.selectCount(new LambdaQueryWrapper<SysPlatformUser>()
                .eq(SysPlatformUser::getUsername, user.getUsername()));
        if (exists != null && exists > 0) {
            throw new BizException(400, "用户名已存在");
        }
        user.setId(null);
        user.setTenantId(0L);
        user.setPassword(passwordEncoder.encode(rawPassword == null ? "Admin@123" : rawPassword));
        user.setStatus("NORMAL");
        if (user.getRoleCode() == null || user.getRoleCode().isBlank()) {
            user.setRoleCode("PLATFORM_OPERATOR");
        }
        if (!List.of("SYSTEM", "PLATFORM_OPERATOR").contains(user.getRoleCode())) {
            throw new BizException(400, "平台账号角色只能是 SYSTEM 或 PLATFORM_OPERATOR");
        }
        platformUserMapper.insert(user);
        auditService.record("platform", "USER_CREATE", user.getUsername(), "新增平台账号");
        return user;
    }

    public List<Map<String, Object>> tenantOptions() {
        List<SysTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .orderByAsc(SysTenant::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysTenant tenant : tenants) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", tenant.getId());
            item.put("name", tenant.getName());
            item.put("code", tenant.getCode());
            item.put("status", tenant.getStatus());
            result.add(item);
        }
        return result;
    }
}
