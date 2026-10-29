package com.greathiit.bwc.module.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * bwc_schedule
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_schedule")
@CrudResource(value = "schedule", module = "logistics", search = {"guardName"})
public class BwcSchedule extends BaseEntity {

    private Long guardId;

    private String guardName;

    private LocalDate shiftDate;

    private String shiftType;

    private String startTime;

    private String endTime;

    private String remark;

}