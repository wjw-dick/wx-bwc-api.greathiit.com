package com.greathiit.bwc.security;

import com.greathiit.bwc.common.BizException;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 业务侧数据与操作范围判定。
 * <p>
 * 权限注解只解决“有没有某个模块的权限”，本类解决“这个角色能不能做管理动作、
 * 能不能看别人的数据”。所有判断均在服务端执行，前端隐藏按钮不能替代。
 */
@Component
public class AccessGuard {

    /** 租户内管理岗位：可管理本校全部业务数据 */
    private static final Set<String> MANAGER_ROLES = Set.of("TENANT_ADMIN", "CAPTAIN", "GUARD");

    /** 审批管理岗：访客/报备等审批仅租户管理员与队长可跨人处理 */
    private static final Set<String> APPROVER_ROLES = Set.of("TENANT_ADMIN", "CAPTAIN");

    /** C 端自助角色：只能操作本人数据 */
    private static final Set<String> SELF_ROLES = Set.of("STAFF", "STUDENT", "VISITEE");

    public LoginUser user() {
        return AuthContext.get();
    }

    public boolean isSystem() {
        LoginUser user = user();
        return user != null && user.isPlatform()
                && "SYSTEM".equalsIgnoreCase(user.getRoleCode());
    }

    public boolean isOperator() {
        LoginUser user = user();
        return user != null && user.isPlatform()
                && "PLATFORM_OPERATOR".equalsIgnoreCase(user.getRoleCode());
    }

    public boolean isManager() {
        LoginUser user = user();
        return user != null && !user.isPlatform()
                && MANAGER_ROLES.contains(user.getRoleCode());
    }

    public boolean isApprovalManager() {
        LoginUser user = user();
        return user != null && !user.isPlatform()
                && APPROVER_ROLES.contains(user.getRoleCode());
    }

    public boolean isGuardRole() {
        LoginUser user = user();
        return user != null && !user.isPlatform() && "GUARD".equals(user.getRoleCode());
    }

    public boolean isLeader() {
        LoginUser user = user();
        return user != null && "LEADER".equals(user.getRoleCode());
    }

    public boolean isSelfRole() {
        LoginUser user = user();
        return user != null && !user.isPlatform()
                && SELF_ROLES.contains(user.getRoleCode());
    }

    /** 管理端只读视图：租户管理岗、校领导、平台超管（跨租户只读） */
    public boolean canReadManagement() {
        return isManager() || isLeader() || isSystem();
    }

    public boolean isSelfServiceAllowed() {
        return isManager() || isSelfRole();
    }

    /** 管理动作（审批、分派、处置、下发、删除等）：只允许租户管理岗 */
    public void requireManager() {
        if (!isManager()) {
            throw BizException.forbidden("仅保卫处管理岗位可执行该操作");
        }
    }

    /** 管理列表/统计/导出：管理岗、校领导、平台超管可读 */
    public void requireRead() {
        if (!canReadManagement()) {
            throw BizException.forbidden("无权查看管理数据");
        }
    }

    /** C 端自助写操作：管理岗或本人角色，校领导只读、平台账号不可写业务数据 */
    public void requireSelfService() {
        if (!isSelfServiceAllowed()) {
            throw BizException.forbidden("当前角色无权执行该操作");
        }
    }

    /** 记录归属校验：管理岗可操作本校全部，C 端只能操作本人数据 */
    public void requireOwnerOrManager(Long ownerId) {
        if (isManager()) {
            return;
        }
        if (!isSelfRole() || ownerId == null || !ownerId.equals(AuthContext.userId())) {
            throw BizException.forbidden("无权访问他人数据");
        }
    }

    /** 审批范围：管理岗可审批全部，教职工/被访人只能审批本人被访的预约 */
    public void requireApprover(Long visitUserId) {
        if (isApprovalManager()) {
            return;
        }
        boolean assignedToMe = visitUserId != null && visitUserId.equals(AuthContext.userId());
        if (!assignedToMe || !(isSelfRole() || isGuardRole())) {
            throw BizException.forbidden("只能审批本人被访的预约");
        }
    }

    public void requireSystem() {
        if (!isSystem()) {
            throw BizException.forbidden("仅平台超级管理员可执行该操作");
        }
        if (TenantContext.isReadOnly()) {
            throw BizException.forbidden("下钻租户为只读模式");
        }
    }
}
