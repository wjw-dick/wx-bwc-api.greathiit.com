package com.greathiit.bwc.module.visitor.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_late_return
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_late_return")
@CrudResource(value = "late-return", module = "visitor", search = {})
public class BwcLateReturn extends BaseEntity {

    private Long studentId;

    private String studentName;

    private String dorm;

    private String type;

    private LocalDateTime recordTime;

    private String reason;

    private String confirmStatus;

    private String confirmBy;

    private LocalDateTime confirmTime;

}