package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * sys_platform_user
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_platform_user")
@CrudResource(value = "platform-user", module = "platform", search = {"username", "realName"})
public class SysPlatformUser extends BaseEntity {

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String realName;

    private String roleCode;

    private String status;

    private LocalDateTime lastLoginTime;

}