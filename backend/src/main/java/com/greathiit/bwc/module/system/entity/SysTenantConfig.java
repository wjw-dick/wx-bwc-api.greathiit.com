package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_tenant_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_config")
@CrudResource(value = "tenant-config", module = "system", search = {})
public class SysTenantConfig extends BaseEntity {

    private String configKey;

    private String configValue;

    private Integer encrypted;

    private String remark;

}