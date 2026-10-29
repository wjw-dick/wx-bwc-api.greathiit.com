package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_audit_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_audit_log")
@CrudResource(value = "audit-log", module = "system", search = {"username"})
public class SysAuditLog extends BaseEntity {

    private Long userId;

    private String username;

    private String action;

    private String module;

    private String target;

    private String detail;

    private String ip;

    private String userAgent;

}