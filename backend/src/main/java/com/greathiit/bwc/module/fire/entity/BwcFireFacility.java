package com.greathiit.bwc.module.fire.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * bwc_fire_facility
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_fire_facility")
@CrudResource(value = "fire-facility", module = "fire", search = {"facilityNo"})
public class BwcFireFacility extends BaseEntity {

    private String facilityNo;

    private String type;

    private String location;

    private String qrCode;

    private LocalDate productionDate;

    private LocalDate expireDate;

    private LocalDate lastCheckDate;

    private LocalDate nextCheckDate;

    private String status;

    private String responsible;

    private String remark;

}