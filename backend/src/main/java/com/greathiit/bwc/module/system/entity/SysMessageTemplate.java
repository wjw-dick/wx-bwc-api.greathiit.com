package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_message_template
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message_template")
@CrudResource(value = "message-template", module = "system", search = {"title"})
public class SysMessageTemplate extends BaseEntity {

    private String bizType;

    private String channel;

    private String templateId;

    private String title;

    private String fieldsJson;

    private Integer enabled;

}