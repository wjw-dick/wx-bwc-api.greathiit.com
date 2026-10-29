package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_plate_auth_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_plate_auth_record")
@CrudResource(value = "plate-auth-record", module = "vehicle", search = {"plateNo"})
public class BwcPlateAuthRecord extends BaseEntity {

    private String plateNo;

    private String plateType;

    private String cameraId;

    private String cameraName;

    private String action;

    private String status;

    private String message;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String batchNo;

    private String operator;

}