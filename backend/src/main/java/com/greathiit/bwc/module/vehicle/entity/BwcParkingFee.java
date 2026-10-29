package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_parking_fee
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_parking_fee")
@CrudResource(value = "parking-fee", module = "vehicle", search = {"plateNo"})
public class BwcParkingFee extends BaseEntity {

    private String plateNo;

    private LocalDateTime enterTime;

    private LocalDateTime exitTime;

    private String payStatus;

    private LocalDateTime payTime;

}