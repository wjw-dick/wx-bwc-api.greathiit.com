package com.greathiit.bwc.module.report.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_report_apply
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_report_apply")
@CrudResource(value = "report-apply", module = "report", search = {"title", "reportNo"})
public class BwcReportApply extends BaseEntity {

    private String reportNo;

    private String type;

    private String title;

    private String applicant;

    private String applicantPhone;

    /** 提交人账号：C 端仅能查看本人报备 */
    private Long ownerId;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String location;

    private String safetyPlan;

    private String attachment;

    private String status;

    private String approver;

    private LocalDateTime approveTime;

    private String approveRemark;

    private String receiptNo;

}