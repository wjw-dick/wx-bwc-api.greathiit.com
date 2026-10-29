package com.greathiit.bwc.module.event.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_mediation
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_mediation")
@CrudResource(value = "mediation", module = "event", search = {})
public class BwcMediation extends BaseEntity {

    private Long disputeId;

    private String content;

    private String result;

    private String mediator;

    private LocalDateTime mediationTime;

}