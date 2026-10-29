package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_plate_camera
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_plate_camera")
@CrudResource(value = "plate-camera", module = "vehicle", search = {"name"})
public class BwcPlateCamera extends BaseEntity {

    private String camId;

    private String name;

    private String campus;

    private String camType;

    private String remark;

    private Integer enabled;

    private String syncStatus;

    private LocalDateTime syncTime;

}