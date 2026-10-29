package com.greathiit.bwc.module.lost.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_lost_notice
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_lost_notice")
@CrudResource(value = "lost-notice", module = "lost", search = {"title"})
public class BwcLostNotice extends BaseEntity {

    private Long itemId;

    private String title;

    private String content;

    private LocalDateTime publishTime;

    private String publisher;

    private String status;

    private Integer views;

}