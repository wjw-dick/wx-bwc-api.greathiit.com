package com.greathiit.bwc.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_visitor_appointment
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_visitor_appointment")
@CrudResource(value = "visitor-appointment", module = "visitor", search = {"plateNo", "visitorName"})
public class BwcVisitorAppointment extends BaseEntity {

    private String visitorName;

    @JsonIgnore
    private String idCardEnc;

    private String idCardMask;

    @JsonIgnore
    private String phoneEnc;

    private String phoneMask;

    private String reason;

    private LocalDateTime visitTime;

    private LocalDateTime leaveTime;

    private Integer accompanyCount;

    private String plateNo;

    private Long visitUserId;

    private String visitUserName;

    private String status;

    private Long approveBy;

    private LocalDateTime approveTime;

    private String approveRemark;

    private String passCode;

    private LocalDateTime passExpireTime;

    private String source;

    private String remark;

}