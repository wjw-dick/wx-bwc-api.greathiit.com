package com.greathiit.bwc.module.patrol.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * bwc_patrol_task
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_patrol_task")
@CrudResource(value = "patrol-task", module = "patrol", search = {"guardName"})
public class BwcPatrolTask extends BaseEntity {

    private Long routeId;

    private Long guardId;

    private String guardName;

    private LocalDate planDate;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

}