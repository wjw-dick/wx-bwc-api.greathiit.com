package com.greathiit.bwc.module.education.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_exam
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_exam")
@CrudResource(value = "exam", module = "education", search = {"title"})
public class BwcExam extends BaseEntity {

    private String title;

    private Long courseId;

    private Integer totalScore;

    private Integer passScore;

    private String questionsJson;

    private String status;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

}