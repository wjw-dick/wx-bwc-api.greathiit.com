package com.greathiit.bwc.module.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * sys_tenant_user Mapper。
 */
@Mapper
public interface SysTenantUserMapper extends BaseMapper<SysTenantUser> {

    /**
     * 登录阶段尚无租户上下文，需跳过租户插件；由业务层校验租户编码。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant_user WHERE username = #{username} AND deleted = 0 ORDER BY id")
    List<SysTenantUser> selectByUsernameIgnoreTenant(@Param("username") String username);

    /** 请求鉴权阶段租户上下文尚未建立，按主键读取账号需绕过租户插件 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant_user WHERE id = #{id} AND deleted = 0 LIMIT 1")
    SysTenantUser selectByIdIgnoreTenant(@Param("id") Long id);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant_user WHERE unionid = #{unionid} AND deleted = 0 LIMIT 1")
    SysTenantUser selectByUnionidIgnoreTenant(@Param("unionid") String unionid);

    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_tenant_user WHERE openid = #{openid} AND deleted = 0 LIMIT 1")
    SysTenantUser selectByOpenidIgnoreTenant(@Param("openid") String openid);
}
