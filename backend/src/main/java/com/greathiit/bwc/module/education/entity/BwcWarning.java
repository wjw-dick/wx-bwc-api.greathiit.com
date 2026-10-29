package com.greathiit.bwc.module.education.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_warning
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_warning")
@CrudResource(value = "warning", module = "education", search = {"title"})
public class BwcWarning extends BaseEntity {

    private String type;

    private String title;

    private String content;

    private String warningLevel;

    private LocalDateTime publishTime;

    private String publisher;

    private String status;

}