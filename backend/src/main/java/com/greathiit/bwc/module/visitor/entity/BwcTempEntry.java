package com.greathiit.bwc.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_temp_entry
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_temp_entry")
@CrudResource(value = "temp-entry", module = "visitor", search = {"visitorName"})
public class BwcTempEntry extends BaseEntity {

    private String visitorName;

    @JsonIgnore
    private String idCardEnc;

    private String idCardMask;

    @JsonIgnore
    private String phoneEnc;

    private String phoneMask;

    private String reason;

    private String visitUser;

    private LocalDateTime inTime;

    private LocalDateTime outTime;

    private String operator;

    private String status;

}