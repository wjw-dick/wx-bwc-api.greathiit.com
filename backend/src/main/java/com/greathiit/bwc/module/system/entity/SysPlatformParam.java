package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_platform_param
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_platform_param")
@CrudResource(value = "platform-param", module = "param", search = {})
public class SysPlatformParam extends BaseEntity {

    private String paramKey;

    private String paramName;

    private String paramValue;

    private String remark;

}