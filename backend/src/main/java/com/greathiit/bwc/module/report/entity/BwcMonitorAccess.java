package com.greathiit.bwc.module.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_monitor_access
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_monitor_access")
@CrudResource(value = "monitor-access", module = "report", search = {})
public class BwcMonitorAccess extends BaseEntity {

    private String applyNo;

    private String applicant;

    private Long applicantId;

    private String reason;

    private String area;

    private String timeRange;

    private String approveLevel;

    private String status;

    private String approver1;

    private LocalDateTime approve1Time;

    private String approver2;

    private LocalDateTime approve2Time;

    private String accessRecord;

    private String result;

    private String feedback;

}