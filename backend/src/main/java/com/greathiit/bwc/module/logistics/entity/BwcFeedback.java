package com.greathiit.bwc.module.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_feedback
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_feedback")
@CrudResource(value = "feedback", module = "logistics", search = {"title"})
public class BwcFeedback extends BaseEntity {

    private String type;

    private String title;

    private String content;

    private String contact;

    /** 提交人账号：C 端仅能查看本人反馈 */
    private Long userId;

    private LocalDateTime submitTime;

    private String status;

    private String handler;

    private LocalDateTime handleTime;

    private String result;

}