package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_vehicle_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_vehicle_log")
@CrudResource(value = "vehicle-log", module = "vehicle", search = {"plateNo"})
public class BwcVehicleLog extends BaseEntity {

    private String plateNo;

    private String cameraId;

    private String cameraName;

    private String inOut;

    private LocalDateTime triggerTime;

    private Integer isWhitelist;

    private String imageUrl;

}