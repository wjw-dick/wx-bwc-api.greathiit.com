package com.greathiit.bwc.common;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.greathiit.bwc.security.TenantContext;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用 CRUD 资源注册表：扫描所有 Mapper，按实体上的 @CrudResource 建立资源名映射。
 */
@Component
@RequiredArgsConstructor
public class ResourceRegistry {

    private final ApplicationContext context;

    @Getter
    private final Map<String, ResourceDef> resources = new HashMap<>();

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() {
        Collection<BaseMapper<?>> mappers = (Collection) context.getBeansOfType(BaseMapper.class).values();
        for (BaseMapper<?> mapper : mappers) {
            Class<?> target = AopUtils.getTargetClass(mapper);
            for (Class<?> iface : target.getInterfaces()) {
                if (iface == BaseMapper.class || !BaseMapper.class.isAssignableFrom(iface)) {
                    continue;
                }
                Class<?> entity = GenericTypeResolver.resolveTypeArgument(iface, BaseMapper.class);
                if (entity == null) {
                    continue;
                }
                CrudResource annotation = entity.getAnnotation(CrudResource.class);
                if (annotation == null) {
                    continue;
                }
                resources.put(annotation.value(), new ResourceDef(
                        annotation.value(), annotation.module(), annotation.search(),
                        (Class<Object>) entity, (BaseMapper<Object>) mapper));
            }
        }
    }

    public ResourceDef get(String resource) {
        ResourceDef def = resources.get(resource);
        if (def == null) {
            throw BizException.notFound("资源不存在：" + resource);
        }
        return def;
    }

    @Getter
    public static class ResourceDef {
        private final String name;
        private final String module;
        private final String[] searchFields;
        private final Class<Object> entityClass;
        private final BaseMapper<Object> mapper;

        public ResourceDef(String name, String module, String[] searchFields,
                           Class<Object> entityClass, BaseMapper<Object> mapper) {
            this.name = name;
            this.module = module;
            this.searchFields = searchFields;
            this.entityClass = entityClass;
            this.mapper = mapper;
        }

        public boolean isPlatformScoped() {
            return TenantContext.isPlatform() && TenantContext.getTenantId() == 0L;
        }
    }
}
