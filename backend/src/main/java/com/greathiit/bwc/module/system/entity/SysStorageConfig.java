package com.greathiit.bwc.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * sys_storage_config
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_storage_config")
@CrudResource(value = "storage-config", module = "system", search = {})
public class SysStorageConfig extends BaseEntity {

    private String driver;

    private String configJson;

    private Integer enabled;

    private LocalDateTime lastTestTime;

}