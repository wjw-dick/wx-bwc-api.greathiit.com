package com.greathiit.bwc.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_device_follow
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_device_follow")
@CrudResource(value = "device-follow", module = "device", search = {})
public class BwcDeviceFollow extends BaseEntity {

    private Long repairId;

    private String content;

    private String operator;

    private LocalDateTime followTime;

    private String photos;

}