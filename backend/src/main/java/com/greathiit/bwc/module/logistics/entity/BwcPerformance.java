package com.greathiit.bwc.module.logistics.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_performance
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_performance")
@CrudResource(value = "performance", module = "logistics", search = {"guardName"})
public class BwcPerformance extends BaseEntity {

    private Long guardId;

    private String guardName;

    private String month;

    private Integer score;

    private String itemsJson;

    private String remark;

}