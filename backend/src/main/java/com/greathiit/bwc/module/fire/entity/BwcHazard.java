package com.greathiit.bwc.module.fire.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_hazard
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_hazard")
@CrudResource(value = "hazard", module = "fire", search = {"hazardNo"})
public class BwcHazard extends BaseEntity {

    private String hazardNo;

    private String source;

    private String hazardLevel;

    private String location;

    private String description;

    private String photos;

    private String reporter;

    private LocalDateTime reportTime;

    private Long assignTo;

    private String assignToName;

    private LocalDateTime assignTime;

    private LocalDateTime deadline;

    private String status;

    private String rectifyPlan;

    private String rectifyPhotos;

    private LocalDateTime rectifyTime;

    private String acceptBy;

    private LocalDateTime acceptTime;

    private String acceptRemark;

}