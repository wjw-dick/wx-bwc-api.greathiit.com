package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_menu
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
@CrudResource(value = "menu", module = "menu", search = {"name"})
public class SysMenu extends BaseEntity {

    private String platform;

    private Long parentId;

    private String name;

    private String nameEn;

    private String path;

    private String icon;

    private Integer sort;

    private String menuType;

    private String permission;

    private Integer visible;

    private String configJson;

}