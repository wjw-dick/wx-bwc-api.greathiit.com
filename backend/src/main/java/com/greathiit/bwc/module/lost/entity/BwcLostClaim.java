package com.greathiit.bwc.module.lost.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_lost_claim
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_lost_claim")
@CrudResource(value = "lost-claim", module = "lost", search = {})
public class BwcLostClaim extends BaseEntity {

    private Long itemId;

    private String claimant;

    private String phoneMask;

    private String idCardMask;

    private LocalDateTime claimTime;

    private String verifyBy;

    private String status;

    private String remark;

}