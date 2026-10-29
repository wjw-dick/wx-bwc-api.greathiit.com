package com.greathiit.bwc.module.emergency.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_emergency_supply
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_emergency_supply")
@CrudResource(value = "emergency-supply", module = "emergency", search = {"name"})
public class BwcEmergencySupply extends BaseEntity {

    private String name;

    private String category;

    private String spec;

    private Integer quantity;

    private String unit;

    private String location;

    private String status;

}