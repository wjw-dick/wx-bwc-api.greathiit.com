package com.greathiit.bwc.module.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.system.entity.SysPlatformUser;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.service.PlatformService;
import com.greathiit.bwc.security.PermissionService;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 平台端：租户管理、全部学校看板、跨校报表、平台账号。
 */
@RestController
@RequestMapping("/api/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;
    private final PermissionService permissionService;

    @GetMapping("/tenants")
    @RequirePermission("tenant:read")
    public R<IPage<SysTenant>> tenants(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) String status) {
        return R.ok(platformService.page(page, size, keyword, status));
    }

    @GetMapping("/tenants/options")
    @RequirePermission("tenant:read")
    public R<List<Map<String, Object>>> tenantOptions() {
        return R.ok(platformService.tenantOptions());
    }

    @PostMapping("/tenants")
    @RequirePermission("tenant:write")
    public R<SysTenant> create(@RequestBody SysTenant tenant) {
        permissionService.requireSystem();
        return R.ok(platformService.create(tenant));
    }

    @PutMapping("/tenants/{id}")
    @RequirePermission("tenant:write")
    public R<SysTenant> update(@PathVariable Long id, @RequestBody SysTenant tenant) {
        permissionService.requireSystem();
        return R.ok(platformService.update(id, tenant));
    }

    @PostMapping("/tenants/{id}/status")
    @RequirePermission("tenant:write")
    public R<SysTenant> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        permissionService.requireSystem();
        return R.ok(platformService.changeStatus(id, body.get("status")));
    }

    @PostMapping("/tenants/{id}/renew")
    @RequirePermission("tenant:write")
    public R<SysTenant> renew(@PathVariable Long id,
                              @RequestBody(required = false) Map<String, Object> body) {
        permissionService.requireSystem();
        LocalDateTime expireTime = null;
        Object raw = body == null ? null : body.get("expireTime");
        if (raw != null && !String.valueOf(raw).isBlank()) {
            expireTime = LocalDateTime.parse(String.valueOf(raw).replace(" ", "T"));
        }
        Integer quota = body == null || body.get("userQuota") == null ? null
                : Integer.valueOf(String.valueOf(body.get("userQuota")));
        return R.ok(platformService.renew(id, expireTime, quota));
    }

    @GetMapping("/tenants/{id}")
    @RequirePermission("tenant:read")
    public R<Map<String, Object>> detail(@PathVariable Long id) {
        return R.ok(platformService.tenantDetail(id));
    }

    @GetMapping("/dashboard")
    @RequirePermission("platform:read")
    public R<Map<String, Object>> dashboard() {
        return R.ok(platformService.dashboard());
    }

    @GetMapping("/users")
    @RequirePermission("platform:read")
    public R<List<SysPlatformUser>> users() {
        permissionService.requireSystem();
        return R.ok(platformService.platformUsers());
    }

    @PostMapping("/users")
    @RequirePermission("platform:write")
    public R<SysPlatformUser> createUser(@RequestBody Map<String, Object> body) {
        permissionService.requireSystem();
        SysPlatformUser user = new SysPlatformUser();
        user.setUsername(body.get("username") == null ? null : String.valueOf(body.get("username")));
        user.setRealName(body.get("realName") == null ? null : String.valueOf(body.get("realName")));
        user.setRoleCode(body.get("roleCode") == null ? null : String.valueOf(body.get("roleCode")));
        String password = body.get("password") == null ? null : String.valueOf(body.get("password"));
        return R.ok(platformService.createPlatformUser(user, password));
    }

    @GetMapping("/reports")
    @RequirePermission("platform:read")
    public R<Map<String, Object>> reports() {
        return R.ok(platformService.dashboard());
    }
}
