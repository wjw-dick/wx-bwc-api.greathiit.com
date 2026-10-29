package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_vehicle
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_vehicle")
@CrudResource(value = "vehicle", module = "vehicle", search = {"plateNo"})
public class BwcVehicle extends BaseEntity {

    private String plateNo;

    private String ownerName;

    private Long ownerId;

    private String ownerPhoneMask;

    private String vehicleType;

    private LocalDateTime validStart;

    private LocalDateTime validEnd;

    private String status;

    private String authStatus;

    private String remark;

}