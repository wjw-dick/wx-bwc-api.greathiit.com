package com.greathiit.bwc.module.education.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.greathiit.bwc.common.BaseEntity;
import com.greathiit.bwc.common.CrudResource;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * bwc_material
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bwc_material")
@CrudResource(value = "material", module = "education", search = {"title"})
public class BwcMaterial extends BaseEntity {

    private String title;

    private String category;

    private String fileUrl;

    private String cover;

    private String description;

    private String status;

}