package com.greathiit.bwc.module.emergency.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_supply_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_supply_record")
@CrudResource(value = "supply-record", module = "emergency", search = {})
public class BwcSupplyRecord extends BaseEntity {

    private Long supplyId;

    private String type;

    private Integer quantity;

    private String userName;

    private LocalDateTime recordTime;

    private String operator;

    private String remark;

}