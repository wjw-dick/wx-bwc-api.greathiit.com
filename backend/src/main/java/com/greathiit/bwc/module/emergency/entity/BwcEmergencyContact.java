package com.greathiit.bwc.module.emergency.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_emergency_contact
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_emergency_contact")
@CrudResource(value = "emergency-contact", module = "emergency", search = {"name"})
public class BwcEmergencyContact extends BaseEntity {

    private String name;

    private String dept;

    private String duty;

    private String phone;

    private Integer sort;

}