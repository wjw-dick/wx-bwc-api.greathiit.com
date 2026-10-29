package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_language
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_language")
@CrudResource(value = "language", module = "language", search = {})
public class SysLanguage extends BaseEntity {

    private String langCode;

    private String langName;

    private Integer enabled;

    private Integer sort;

}