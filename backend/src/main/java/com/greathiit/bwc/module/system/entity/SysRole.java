package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_role
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@CrudResource(value = "role", module = "system", search = {})
public class SysRole extends BaseEntity {

    private String roleCode;

    private String roleName;

    private String dataScope;

    private String remark;

}