package com.greathiit.bwc.module.education.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_safety_course
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_safety_course")
@CrudResource(value = "safety-course", module = "education", search = {"title"})
public class BwcSafetyCourse extends BaseEntity {

    private String title;

    private String category;

    private String cover;

    private String content;

    private String videoUrl;

    private String status;

    private LocalDateTime publishTime;

}