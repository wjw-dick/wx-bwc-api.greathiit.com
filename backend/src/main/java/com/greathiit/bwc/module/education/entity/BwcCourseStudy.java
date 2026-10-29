package com.greathiit.bwc.module.education.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_course_study
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_course_study")
@CrudResource(value = "course-study", module = "education", search = {})
public class BwcCourseStudy extends BaseEntity {

    private Long courseId;

    private Long userId;

    private String userName;

    private Integer progress;

    private LocalDateTime studyTime;

    private Integer finishFlag;

}