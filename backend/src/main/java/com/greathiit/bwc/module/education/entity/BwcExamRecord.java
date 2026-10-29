package com.greathiit.bwc.module.education.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_exam_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_exam_record")
@CrudResource(value = "exam-record", module = "education", search = {})
public class BwcExamRecord extends BaseEntity {

    private Long examId;

    private Long userId;

    private String userName;

    private Integer score;

    private Integer passFlag;

    private String answersJson;

    private LocalDateTime submitTime;

}