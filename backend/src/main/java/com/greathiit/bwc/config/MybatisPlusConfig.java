package com.greathiit.bwc.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.greathiit.bwc.security.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * MyBatis-Plus：租户插件（行级隔离）+ 分页插件。
 * 平台级表加入白名单；system 超管跨租户查询时跳过租户条件。
 */
@Configuration
public class MybatisPlusConfig {

    /** 平台级表，不参与租户隔离 */
    public static final Set<String> IGNORE_TABLES = Set.of(
            "sys_tenant",
            "sys_platform_user",
            "sys_global_dict",
            "sys_platform_param",
            "sys_language",
            "sys_menu"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                return new LongValue(TenantContext.getTenantId());
            }

            @Override
            public boolean ignoreTable(String tableName) {
                return TenantContext.isIgnoreTenant()
                        || IGNORE_TABLES.contains(tableName.toLowerCase());
            }
        }));
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
