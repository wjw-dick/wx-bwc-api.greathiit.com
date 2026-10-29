package com.greathiit.bwc.module.event.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_work_order
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_work_order")
@CrudResource(value = "work-order", module = "event", search = {"title", "orderNo"})
public class BwcWorkOrder extends BaseEntity {

    private String orderNo;

    private Long eventId;

    private String title;

    private Long assignTo;

    private String assignToName;

    private String status;

    private LocalDateTime deadline;

    private LocalDateTime finishTime;

    private String result;

    private String photos;

}