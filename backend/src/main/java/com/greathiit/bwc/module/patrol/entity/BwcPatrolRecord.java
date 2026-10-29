package com.greathiit.bwc.module.patrol.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * bwc_patrol_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_patrol_record")
@CrudResource(value = "patrol-record", module = "patrol", search = {"guardName"})
public class BwcPatrolRecord extends BaseEntity {

    private Long routeId;

    private Long pointId;

    private Long guardId;

    private String guardName;

    private LocalDateTime checkTime;

    private String method;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String status;

    private String abnormalDesc;

    private String photos;

    private String remark;

}