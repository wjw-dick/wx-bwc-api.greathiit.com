package com.greathiit.bwc.module.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_attendance
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_attendance")
@CrudResource(value = "attendance", module = "logistics", search = {"guardName"})
public class BwcAttendance extends BaseEntity {

    private Long guardId;

    private String guardName;

    private LocalDateTime checkTime;

    private String type;

    private String method;

    private String status;

}