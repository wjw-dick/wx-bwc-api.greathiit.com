package com.greathiit.bwc.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_plate_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_plate_config")
@CrudResource(value = "plate-config", module = "vehicle", search = {})
public class BwcPlateConfig extends BaseEntity {

    private String baseUrl;

    @JsonIgnore
    private String apiKeyEnc;

    private String apiKeyMask;

    private String status;

    private LocalDateTime lastTestTime;

    private Integer mqttConnected;

}