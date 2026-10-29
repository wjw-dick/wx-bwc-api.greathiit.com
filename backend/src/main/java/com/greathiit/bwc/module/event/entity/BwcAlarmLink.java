package com.greathiit.bwc.module.event.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_alarm_link
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_alarm_link")
@CrudResource(value = "alarm-link", module = "event", search = {})
public class BwcAlarmLink extends BaseEntity {

    private String alarmType;

    private String source;

    private Long eventId;

    private String content;

    private LocalDateTime linkTime;

    private String status;

}