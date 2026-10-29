package com.greathiit.bwc.module.patrol.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * bwc_dorm_check
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_dorm_check")
@CrudResource(value = "dorm-check", module = "patrol", search = {})
public class BwcDormCheck extends BaseEntity {

    private String dorm;

    private LocalDate checkDate;

    private String checker;

    private Integer shouldCount;

    private Integer actualCount;

    private String absentNames;

    private String status;

    private String remark;

}