package com.greathiit.bwc.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDate;

/**
 * bwc_device
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_device")
@CrudResource(value = "device", module = "device", search = {"name"})
public class BwcDevice extends BaseEntity {

    private String deviceNo;

    private String type;

    private String name;

    private String location;

    private String vendor;

    private String model;

    private LocalDate installDate;

    private String status;

    private String remark;

}