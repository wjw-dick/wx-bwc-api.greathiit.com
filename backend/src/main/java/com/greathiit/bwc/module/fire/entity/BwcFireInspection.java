package com.greathiit.bwc.module.fire.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_fire_inspection
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_fire_inspection")
@CrudResource(value = "fire-inspection", module = "fire", search = {"facilityNo"})
public class BwcFireInspection extends BaseEntity {

    private Long facilityId;

    private String facilityNo;

    private String inspector;

    private LocalDateTime inspectTime;

    private String result;

    private String photos;

    private Integer abnormalFlag;

    private Long hazardId;

    private String remark;

}