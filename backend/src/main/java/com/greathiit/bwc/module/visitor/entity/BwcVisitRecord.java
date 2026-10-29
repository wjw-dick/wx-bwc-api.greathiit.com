package com.greathiit.bwc.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_visit_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_visit_record")
@CrudResource(value = "visit-record", module = "visitor", search = {"visitorName"})
public class BwcVisitRecord extends BaseEntity {

    private Long appointmentId;

    private String visitorName;

    private String passCode;

    private String gate;

    private LocalDateTime inTime;

    private LocalDateTime outTime;

    private String operator;

    private String method;

    private String status;

    private String remark;

}