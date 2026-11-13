package com.greathiit.bwc.security;

/**
 * 租户上下文：登录后由 JWT 解析 tenant_id，通过 ThreadLocal 传递。
 * system 超管可通过 X-Tenant-Id 下钻指定租户；未指定时 ignoreTenant=true 表示跨租户查询。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = ThreadLocal.withInitial(() -> 0L);
    private static final ThreadLocal<Boolean> IGNORE = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> PLATFORM = ThreadLocal.withInitial(() -> Boolean.FALSE);
    private static final ThreadLocal<Boolean> READ_ONLY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TenantContext() {
    }

    public static void set(Long tenantId, boolean ignoreTenant, boolean platform, boolean readOnly) {
        TENANT_ID.set(tenantId == null ? 0L : tenantId);
        IGNORE.set(ignoreTenant);
        PLATFORM.set(platform);
        READ_ONLY.set(readOnly);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static boolean isIgnoreTenant() {
        return Boolean.TRUE.equals(IGNORE.get());
    }

    /** 仅切换跨租户忽略标记（用于登录态建立前按用户显式租户查询） */
    public static void setIgnoreTenant(boolean ignoreTenant) {
        IGNORE.set(ignoreTenant);
    }

    public static boolean isPlatform() {
        return Boolean.TRUE.equals(PLATFORM.get());
    }

    public static boolean isReadOnly() {
        return Boolean.TRUE.equals(READ_ONLY.get());
    }

    public static void clear() {
        TENANT_ID.remove();
        IGNORE.remove();
        PLATFORM.remove();
        READ_ONLY.remove();
    }
}
