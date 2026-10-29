package com.greathiit.bwc.security;

import com.greathiit.bwc.common.BizException;
import org.springframework.stereotype.Component;

/**
 * 当前请求的用户上下文。
 */
@Component
public class AuthContext {

    private static final ThreadLocal<LoginUser> USER = new ThreadLocal<>();

    public static void set(LoginUser user) {
        USER.set(user);
    }

    public static LoginUser get() {
        return USER.get();
    }

    public static LoginUser require() {
        LoginUser user = USER.get();
        if (user == null) {
            throw new BizException(401, "登录已失效，请重新登录");
        }
        return user;
    }

    public static Long userId() {
        return require().getUserId();
    }

    public static Long tenantId() {
        return TenantContext.getTenantId();
    }

    public static String username() {
        LoginUser user = USER.get();
        return user == null ? "system" : user.getUsername();
    }

    public static boolean isPlatform() {
        LoginUser user = USER.get();
        return user != null && user.isPlatform();
    }

    public static String roleCode() {
        LoginUser user = USER.get();
        return user == null ? "" : user.getRoleCode();
    }

    public static void clear() {
        USER.remove();
    }
}
