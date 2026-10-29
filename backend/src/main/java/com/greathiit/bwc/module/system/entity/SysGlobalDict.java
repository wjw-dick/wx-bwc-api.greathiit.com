package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_global_dict
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_global_dict")
@CrudResource(value = "global-dict", module = "dict", search = {})
public class SysGlobalDict extends BaseEntity {

    private String dictType;

    private String dictLabel;

    private String dictValue;

    private Integer sort;

    private String status;

    private String remark;

}