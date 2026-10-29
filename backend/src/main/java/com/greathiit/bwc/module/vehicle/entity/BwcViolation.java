package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_violation
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_violation")
@CrudResource(value = "violation", module = "vehicle", search = {"plateNo"})
public class BwcViolation extends BaseEntity {

    private String plateNo;

    private String location;

    private LocalDateTime violateTime;

    private String description;

    private String photos;

    private String handleStatus;

    private String handleResult;

    private String operator;

}