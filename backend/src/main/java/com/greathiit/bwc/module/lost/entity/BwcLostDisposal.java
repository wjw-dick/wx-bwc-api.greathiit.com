package com.greathiit.bwc.module.lost.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_lost_disposal
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_lost_disposal")
@CrudResource(value = "lost-disposal", module = "lost", search = {})
public class BwcLostDisposal extends BaseEntity {

    private Long itemId;

    private String disposalType;

    private String reason;

    private String applyBy;

    private LocalDateTime applyTime;

    private String approveBy;

    private LocalDateTime approveTime;

    private String status;

}