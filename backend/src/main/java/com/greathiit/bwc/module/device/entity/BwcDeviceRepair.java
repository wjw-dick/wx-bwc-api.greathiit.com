package com.greathiit.bwc.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_device_repair
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_device_repair")
@CrudResource(value = "device-repair", module = "device", search = {"repairNo", "deviceName"})
public class BwcDeviceRepair extends BaseEntity {

    private String repairNo;

    private Long deviceId;

    private String deviceName;

    private String faultDesc;

    private String photos;

    private String reporter;

    private LocalDateTime reportTime;

    private String status;

    private String assignTo;

    private LocalDateTime finishTime;

    private String result;

}