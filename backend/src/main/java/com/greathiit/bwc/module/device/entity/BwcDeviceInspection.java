package com.greathiit.bwc.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_device_inspection
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_device_inspection")
@CrudResource(value = "device-inspection", module = "device", search = {})
public class BwcDeviceInspection extends BaseEntity {

    private Long deviceId;

    private String inspector;

    private LocalDateTime inspectTime;

    private String result;

    private String remark;

}