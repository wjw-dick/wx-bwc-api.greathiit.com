package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_role_menu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
@CrudResource(value = "role-menu", module = "system", search = {})
public class SysRoleMenu extends BaseEntity {

    private Long roleId;

    private Long menuId;

}