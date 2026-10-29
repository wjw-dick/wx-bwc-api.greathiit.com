package com.greathiit.bwc.module.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_monthly_report
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_monthly_report")
@CrudResource(value = "monthly-report", module = "logistics", search = {})
public class BwcMonthlyReport extends BaseEntity {

    private String month;

    private String contentJson;

    private String fileUrl;

}