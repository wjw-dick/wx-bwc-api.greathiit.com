package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * sys_attachment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_attachment")
@CrudResource(value = "attachment", module = "system", search = {})
public class SysAttachment extends BaseEntity {

    private String bizType;

    private Long bizId;

    private String fileName;

    private String url;

    private Long size;

    private String contentType;

}