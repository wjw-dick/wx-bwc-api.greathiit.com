package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_message
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_message")
@CrudResource(value = "message", module = "system", search = {"title"})
public class SysMessage extends BaseEntity {

    private Long userId;

    private String title;

    private String content;

    private String bizType;

    private Long bizId;

    private Integer readFlag;

}