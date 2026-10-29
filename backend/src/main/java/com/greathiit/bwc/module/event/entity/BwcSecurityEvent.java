package com.greathiit.bwc.module.event.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_security_event
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_security_event")
@CrudResource(value = "security-event", module = "event", search = {"eventNo"})
public class BwcSecurityEvent extends BaseEntity {

    private String eventNo;

    private String eventType;

    private String location;

    private String description;

    private String urgency;

    private String photos;

    private String reporter;

    private String reporterPhoneMask;

    private String status;

    private Long assignTo;

    private String assignToName;

    private LocalDateTime assignTime;

    private String handleResult;

    private String handlePhotos;

    private LocalDateTime closeTime;

    private String source;

}