package com.greathiit.bwc.module.patrol.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_patrol_point
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_patrol_point")
@CrudResource(value = "patrol-point", module = "patrol", search = {"name"})
public class BwcPatrolPoint extends BaseEntity {

    private Long routeId;

    private String name;

    private String location;

    private String qrCode;

    private String timeWindowStart;

    private String timeWindowEnd;

    private Integer sort;

    private String status;

}