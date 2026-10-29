package com.greathiit.bwc.module.event.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_dispute
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_dispute")
@CrudResource(value = "dispute", module = "event", search = {"title"})
public class BwcDispute extends BaseEntity {

    private String title;

    private String parties;

    private String location;

    private LocalDateTime occurTime;

    private String description;

    private String mediator;

    private String status;

}