package com.greathiit.bwc.module.emergency.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_emergency_plan
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_emergency_plan")
@CrudResource(value = "emergency-plan", module = "emergency", search = {"title"})
public class BwcEmergencyPlan extends BaseEntity {

    private String title;

    private String type;

    private String content;

    private String fileUrl;

    private String version;

    private String status;

    private LocalDateTime publishTime;

}