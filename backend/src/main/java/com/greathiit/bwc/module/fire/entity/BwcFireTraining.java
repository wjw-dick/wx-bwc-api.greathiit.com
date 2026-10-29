package com.greathiit.bwc.module.fire.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_fire_training
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_fire_training")
@CrudResource(value = "fire-training", module = "fire", search = {"title"})
public class BwcFireTraining extends BaseEntity {

    private String title;

    private String trainer;

    private LocalDateTime trainingTime;

    private String location;

    private Integer participants;

    private String content;

    private String status;

}