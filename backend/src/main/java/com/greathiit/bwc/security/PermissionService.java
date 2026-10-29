package com.greathiit.bwc.security;

import com.greathiit.bwc.common.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 两级 RBAC 权限判定。
 * 平台：SYSTEM / PLATFORM_OPERATOR；租户：TENANT_ADMIN / CAPTAIN / GUARD / STAFF / STUDENT / LEADER / VISITEE。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    /** 保安可写的业务模块 */
    private static final Set<String> GUARD_WRITE = Set.of(
            "visitor", "vehicle", "fire", "hazard", "event", "patrol", "lost",
            "device", "emergency", "logistics", "feedback");

    /** C 端（教职工/学生/被访人）可写的业务模块 */
    private static final Set<String> SELF_WRITE = Set.of(
            "visitor", "vehicle", "lost", "education", "report", "device", "feedback", "emergency");

    /** 平台可写的平台级模块 */
    private static final Set<String> PLATFORM_WRITE = Set.of(
            "tenant", "dict", "param", "language", "platform", "menu", "audit");

    public boolean has(String permission) {
        LoginUser user = AuthContext.get();
        if (user == null || permission == null || permission.isBlank()) {
            return false;
        }
        String[] parts = permission.split(":", 2);
        String module = parts[0];
        String action = parts.length > 1 ? parts[1] : "read";
        boolean write = !"read".equals(action);

        if (user.isPlatform()) {
            if ("platform".equals(module) || "tenant".equals(module)) {
                return true;
            }
            if (!write) {
                return true;
            }
            // 下钻租户时只读；平台账号不直接写业务数据
            if (TenantContext.isReadOnly() || TenantContext.getTenantId() != 0L) {
                return false;
            }
            return PLATFORM_WRITE.contains(module);
        }

        if (module.startsWith("platform")) {
            return false;
        }
        if ("TENANT_ADMIN".equals(user.getRoleCode()) || "CAPTAIN".equals(user.getRoleCode())) {
            return true;
        }
        if ("LEADER".equals(user.getRoleCode())) {
            return !write;
        }
        if ("GUARD".equals(user.getRoleCode())) {
            return !write || GUARD_WRITE.contains(module);
        }
        if ("STAFF".equals(user.getRoleCode()) || "STUDENT".equals(user.getRoleCode())
                || "VISITEE".equals(user.getRoleCode())) {
            if ("approve".equals(action)) {
                return "VISITEE".equals(user.getRoleCode()) || "STAFF".equals(user.getRoleCode());
            }
            return !write || SELF_WRITE.contains(module);
        }
        return !write;
    }

    public void require(String permission) {
        if (!has(permission)) {
            throw BizException.forbidden("越权访问：" + permission);
        }
    }

    public void requirePlatform() {
        LoginUser user = AuthContext.get();
        if (user == null || !user.isPlatform()) {
            throw BizException.forbidden("仅平台超级管理员可操作");
        }
        if (TenantContext.isReadOnly()) {
            throw BizException.forbidden("下钻租户为只读模式");
        }
    }
}
