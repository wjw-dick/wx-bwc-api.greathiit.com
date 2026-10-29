package com.greathiit.bwc.module.fire.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_fire_alarm
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_fire_alarm")
@CrudResource(value = "fire-alarm", module = "fire", search = {})
public class BwcFireAlarm extends BaseEntity {

    private String alarmType;

    private String location;

    private LocalDateTime alarmTime;

    private String deviceId;

    private String content;

    private Long linkEventId;

    private String status;

}