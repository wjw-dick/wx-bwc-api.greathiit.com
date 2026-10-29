package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_vehicle_pass
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_vehicle_pass")
@CrudResource(value = "vehicle-pass", module = "vehicle", search = {"plateNo"})
public class BwcVehiclePass extends BaseEntity {

    private String plateNo;

    private String type;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private String operator;

    private String remark;

}