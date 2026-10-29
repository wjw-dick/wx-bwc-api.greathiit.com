package com.greathiit.bwc.module.lost.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

/**
 * bwc_lost_item
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_lost_item")
@CrudResource(value = "lost-item", module = "lost", search = {"name", "itemNo"})
public class BwcLostItem extends BaseEntity {

    private String itemNo;

    private String name;

    private String category;

    private String location;

    private LocalDateTime foundTime;

    private String photos;

    private String description;

    private String finder;

    private String status;

    private String storageLocation;

}