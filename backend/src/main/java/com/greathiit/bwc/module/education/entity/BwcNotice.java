package com.greathiit.bwc.module.education.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_notice
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_notice")
@CrudResource(value = "notice", module = "education", search = {"title"})
public class BwcNotice extends BaseEntity {

    private String title;

    private String content;

    private String category;

    private LocalDateTime publishTime;

    private String publisher;

    private String status;

    private Integer views;

}