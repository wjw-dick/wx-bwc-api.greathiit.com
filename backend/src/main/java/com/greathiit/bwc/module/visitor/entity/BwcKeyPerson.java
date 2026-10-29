package com.greathiit.bwc.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_key_person
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_key_person")
@CrudResource(value = "key-person", module = "visitor", search = {"name"})
public class BwcKeyPerson extends BaseEntity {

    private String name;

    private String idCardMask;

    private String phoneMask;

    private String category;

    private String remark;

    private String status;

}