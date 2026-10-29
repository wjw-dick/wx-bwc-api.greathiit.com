package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_tenant_dict
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_dict")
@CrudResource(value = "tenant-dict", module = "system", search = {})
public class SysTenantDict extends BaseEntity {

    private String dictType;

    private String dictLabel;

    private String dictValue;

    private Integer sort;

    private String status;

}