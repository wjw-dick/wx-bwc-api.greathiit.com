package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * sys_tenant
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
@CrudResource(value = "tenant", module = "tenant", search = {"name"})
public class SysTenant extends BaseEntity {

    private String name;

    private String code;

    private String type;

    private String status;

    private LocalDateTime openTime;

    private LocalDateTime expireTime;

    private Integer userQuota;

    private String contact;

    private String phone;

    private String remark;

}