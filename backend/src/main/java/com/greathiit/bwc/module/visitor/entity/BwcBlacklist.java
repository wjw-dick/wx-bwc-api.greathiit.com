package com.greathiit.bwc.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_blacklist
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_blacklist")
@CrudResource(value = "blacklist", module = "visitor", search = {"name"})
public class BwcBlacklist extends BaseEntity {

    private String name;

    private String idCardMask;

    private String phoneMask;

    private String reason;

    private String level;

    private String operator;

    private String status;

}