package com.greathiit.bwc.module.patrol.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_patrol_route
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_patrol_route")
@CrudResource(value = "patrol-route", module = "patrol", search = {"name"})
public class BwcPatrolRoute extends BaseEntity {

    private String name;

    private String description;

    private String status;

}