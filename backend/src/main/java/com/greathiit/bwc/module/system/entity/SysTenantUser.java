package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * sys_tenant_user
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_user")
@CrudResource(value = "tenant-user", module = "system", search = {"username", "realName"})
public class SysTenantUser extends BaseEntity {

    private String username;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String realName;

    @JsonIgnore
    private String phoneEnc;

    private String phoneMask;

    @JsonIgnore
    private String idCardEnc;

    private String idCardMask;

    private Long roleId;

    private String roleCode;

    private String userType;

    private String dept;

    private String openid;

    private String unionid;

    private String status;

    private LocalDateTime lastLoginTime;

}