package com.greathiit.bwc.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 当前登录用户（JWT 载荷的业务视图）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private Long userId;
    private Long tenantId;
    private String tenantCode;
    private String username;
    private String realName;
    private String roleCode;
    private String userType;
    private boolean platform;
}
