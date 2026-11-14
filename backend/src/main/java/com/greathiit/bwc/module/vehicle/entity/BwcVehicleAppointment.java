package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_vehicle_appointment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_vehicle_appointment")
@CrudResource(value = "vehicle-appointment", module = "vehicle", search = {"plateNo", "visitorName"})
public class BwcVehicleAppointment extends BaseEntity {

    private String plateNo;

    private String visitorName;

    private String phoneMask;

    /** 提交人账号：C 端仅能查看本人预约 */
    private Long ownerId;

    private String reason;

    private LocalDateTime visitTime;

    private LocalDateTime leaveTime;

    private String status;

    private Long approveBy;

    private LocalDateTime approveTime;

    private String authStatus;

    private String remark;

}