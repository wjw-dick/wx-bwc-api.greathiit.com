package com.greathiit.bwc.module.emergency.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_emergency_event
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_emergency_event")
@CrudResource(value = "emergency-event", module = "emergency", search = {"title"})
public class BwcEmergencyEvent extends BaseEntity {

    private String title;

    private String type;

    private String location;

    private LocalDateTime occurTime;

    private String description;

    private String response;

    private String status;

    private String reporter;

    private String photos;

}