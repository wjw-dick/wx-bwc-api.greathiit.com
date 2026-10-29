package com.greathiit.bwc.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.greathiit.bwc.security.TenantContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 公共字段自动填充：租户、创建/更新时间、逻辑删除。
 */
@Component
public class BwcMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        if (metaObject.hasGetter("tenantId") && getFieldValByName("tenantId", metaObject) == null) {
            setFieldValByName("tenantId", TenantContext.getTenantId(), metaObject);
        }
        if (metaObject.hasGetter("deleted") && getFieldValByName("deleted", metaObject) == null) {
            setFieldValByName("deleted", 0, metaObject);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
