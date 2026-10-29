package com.greathiit.bwc.module.fire.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_power_check
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_power_check")
@CrudResource(value = "power-check", module = "fire", search = {})
public class BwcPowerCheck extends BaseEntity {

    private String location;

    private LocalDateTime checkTime;

    private String checker;

    private String result;

    private String violationDesc;

    private String photos;

    private String handleResult;

}