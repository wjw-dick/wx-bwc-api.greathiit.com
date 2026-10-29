package com.greathiit.bwc.module.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求。
 */
@Data
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** platform=平台端，tenant=租户端 */
    private String loginType = "tenant";

    /** 多租户下同名账号需指定租户编码 */
    private String tenantCode;
}
