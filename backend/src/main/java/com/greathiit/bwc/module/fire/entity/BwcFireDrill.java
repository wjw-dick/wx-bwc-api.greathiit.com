package com.greathiit.bwc.module.fire.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_fire_drill
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_fire_drill")
@CrudResource(value = "fire-drill", module = "fire", search = {"title"})
public class BwcFireDrill extends BaseEntity {

    private String title;

    private LocalDateTime drillTime;

    private String location;

    private Integer participants;

    private Integer signCount;

    private String summary;

    private String status;

}