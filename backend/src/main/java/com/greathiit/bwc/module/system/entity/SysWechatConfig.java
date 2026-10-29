package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_wechat_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_wechat_config")
@CrudResource(value = "wechat-config", module = "system", search = {})
public class SysWechatConfig extends BaseEntity {

    private String appType;

    private String appId;

    @JsonIgnore
    private String appSecretEnc;

    private String appSecretMask;

    private String token;

    @JsonIgnore
    private String aesKeyEnc;

    private String serverUrl;

    private String authDomain;

    private String jsDomain;

    private String remark;

}