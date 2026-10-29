package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_message_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_log")
@CrudResource(value = "message-log", module = "system", search = {})
public class SysMessageLog extends BaseEntity {

    private String channel;

    private String bizType;

    private String templateId;

    private String toUser;

    private String content;

    private String status;

    private String error;

}