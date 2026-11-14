package com.greathiit.bwc.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greathiit.bwc.security.AccessGuard;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.PermissionService;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用 CRUD 控制器：/api/crud/{resource}
 * 租户隔离由 MyBatis-Plus 租户插件强制生效；权限按资源所属模块校验。
 */
@RestController
@RequestMapping("/api/crud")
@RequiredArgsConstructor
public class CrudController {

    private static final Set<String> RESERVED = Set.of(
            "page", "size", "orderBy", "keyword", "current", "asc", "desc");

    /** 只能走专用接口的模块，避免通用 CRUD 绕过密码加密、权限校验等业务规则 */
    private static final Set<String> BLOCKED_MODULES = Set.of(
            "platform", "tenant", "system", "dict", "menu", "param", "i18n",
            "storage", "wechat", "audit");

    /** 含密钥或安全配置的资源，禁止通用 CRUD 读写 */
    private static final Set<String> BLOCKED_RESOURCES = Set.of(
            "plate-config", "plate-camera", "plate-auth-record",
            "tenant-user", "role", "role-menu", "tenant-config", "attachment",
            "message", "message-log", "storage-config", "wechat-config", "message-template");

    /** 台账类资源只读，防止通过通用 CRUD 篡改/删除审计痕迹 */
    private static final Set<String> READ_ONLY_RESOURCES = Set.of(
            "visit-record", "vehicle-log", "plate-log", "plate-auth-record", "audit-log");

    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "tenantId", "deleted", "idCardEnc", "phoneEnc", "password", "openid", "unionid");

    private final ResourceRegistry registry;
    private final PermissionService permissionService;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    @GetMapping("/{resource}")
    public R<IPage<Object>> list(@PathVariable String resource,
                                 @RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String orderBy,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam Map<String, String> params) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        guardRead(def);
        QueryWrapper<Object> wrapper = buildWrapper(def, params, keyword);
        applyOrder(def, wrapper, orderBy);
        IPage<Object> result = def.getMapper().selectPage(new Page<>(page, Math.min(size, 500)), wrapper);
        return R.ok(result);
    }

    @GetMapping("/{resource}/all")
    public R<List<Object>> all(@PathVariable String resource,
                               @RequestParam(required = false) Map<String, String> params,
                               @RequestParam(required = false) String keyword) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        guardRead(def);
        QueryWrapper<Object> wrapper = buildWrapper(def, params, keyword);
        wrapper.orderByDesc("id");
        return R.ok(def.getMapper().selectList(wrapper));
    }

    @GetMapping("/{resource}/{id}")
    public R<Object> get(@PathVariable String resource, @PathVariable Long id) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        guardRead(def);
        Object entity = def.getMapper().selectById(id);
        if (entity == null) {
            throw BizException.notFound("数据不存在或无权访问");
        }
        return R.ok(entity);
    }

    @PostMapping("/{resource}")
    public R<Object> create(@PathVariable String resource, @RequestBody Map<String, Object> body) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        guardWrite(def);
        Object entity = toEntity(def, body, null);
        def.getMapper().insert(entity);
        return R.ok(entity);
    }

    @PutMapping("/{resource}/{id}")
    public R<Object> update(@PathVariable String resource, @PathVariable Long id,
                            @RequestBody Map<String, Object> body) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        guardWrite(def);
        Object existing = def.getMapper().selectById(id);
        if (existing == null) {
            throw BizException.notFound("数据不存在或无权访问");
        }
        Object entity = toEntity(def, body, id);
        def.getMapper().updateById(entity);
        return R.ok(def.getMapper().selectById(id));
    }

    @DeleteMapping("/{resource}/{id}")
    public R<Void> delete(@PathVariable String resource, @PathVariable Long id) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        guardWrite(def);
        int rows = def.getMapper().deleteById(id);
        if (rows == 0) {
            throw BizException.notFound("数据不存在或无权访问");
        }
        return R.ok();
    }

    @PostMapping("/{resource}/batch-delete")
    public R<Integer> batchDelete(@PathVariable String resource, @RequestBody List<Long> ids) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        guardWrite(def);
        int rows = def.getMapper().deleteBatchIds(ids);
        return R.ok(rows);
    }

    private void guardRead(ResourceRegistry.ResourceDef def) {
        if (!accessGuard.canReadManagement()) {
            throw BizException.forbidden("通用接口仅向管理岗位开放，请使用对应自助接口");
        }
        if (BLOCKED_MODULES.contains(def.getModule()) || BLOCKED_RESOURCES.contains(def.getName())) {
            throw BizException.forbidden("该资源仅支持专用接口访问");
        }
        permissionService.require(def.getModule() + ":read");
    }

    private void guardWrite(ResourceRegistry.ResourceDef def) {
        accessGuard.requireManager();
        if (BLOCKED_MODULES.contains(def.getModule()) || BLOCKED_RESOURCES.contains(def.getName())) {
            throw BizException.forbidden("该资源仅支持专用接口访问");
        }
        if (READ_ONLY_RESOURCES.contains(def.getName())) {
            throw BizException.forbidden("台账类资源只读，禁止修改或删除");
        }
        permissionService.require(def.getModule() + ":write");
    }

    private void checkField(Class<?> entityClass, String field) {
        if (field == null || field.isBlank()) {
            throw new BizException(400, "查询字段不能为空");
        }
        if (SENSITIVE_FIELDS.contains(field)) {
            throw BizException.forbidden("不允许按敏感字段查询：" + field);
        }
        Class<?> current = entityClass;
        while (current != null && current != Object.class) {
            for (java.lang.reflect.Field declared : current.getDeclaredFields()) {
                if (declared.getName().equals(field)) {
                    return;
                }
            }
            current = current.getSuperclass();
        }
        throw new BizException(400, "非法查询字段：" + field);
    }

    private Object toEntity(ResourceRegistry.ResourceDef def, Map<String, Object> body, Long id) {
        Map<String, Object> copy = new LinkedHashMap<>(body);
        copy.remove("id");
        copy.remove("createTime");
        copy.remove("updateTime");
        copy.remove("deleted");
        Object entity = objectMapper.convertValue(copy, def.getEntityClass());
        if (entity instanceof BaseEntity base) {
            base.setId(id);
            if (!AuthContext.isPlatform() || TenantContext.getTenantId() > 0) {
                base.setTenantId(TenantContext.getTenantId());
            }
        }
        return entity;
    }

    private QueryWrapper<Object> buildWrapper(ResourceRegistry.ResourceDef def,
                                              Map<String, String> params, String keyword) {
        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (RESERVED.contains(key) || value == null || value.isBlank()) {
                continue;
            }
            if ("keyword".equals(key)) {
                continue;
            }
            if (key.endsWith("Start") || key.endsWith("End")) {
                boolean start = key.endsWith("Start");
                String rawField = key.substring(0, key.length() - 5);
                checkField(def.getEntityClass(), rawField);
                String field = toColumn(rawField);
                if (start) {
                    wrapper.ge(field, parseDate(value, false));
                } else {
                    wrapper.le(field, parseDate(value, true));
                }
                continue;
            }
            checkField(def.getEntityClass(), key);
            if (value.contains(",") && !key.toLowerCase().contains("status")) {
                wrapper.in(toColumn(key), List.of(value.split(",")));
            } else {
                wrapper.eq(toColumn(key), value);
            }
        }
        if (keyword != null && !keyword.isBlank() && def.getSearchFields().length > 0) {
            List<String> fields = new ArrayList<>();
            for (String field : def.getSearchFields()) {
                fields.add(toColumn(field));
            }
            wrapper.and(w -> {
                for (int i = 0; i < fields.size(); i++) {
                    if (i == 0) {
                        w.like(fields.get(i), keyword);
                    } else {
                        w.or().like(fields.get(i), keyword);
                    }
                }
            });
        }
        return wrapper;
    }

    private void applyOrder(ResourceRegistry.ResourceDef def, QueryWrapper<Object> wrapper, String orderBy) {
        if (orderBy == null || orderBy.isBlank()) {
            wrapper.orderByDesc("id");
            return;
        }
        for (String item : orderBy.split(",")) {
            String[] parts = item.split(":");
            checkField(def.getEntityClass(), parts[0]);
            String column = toColumn(parts[0]);
            boolean asc = parts.length > 1 && "asc".equalsIgnoreCase(parts[1]);
            if (asc) {
                wrapper.orderByAsc(column);
            } else {
                wrapper.orderByDesc(column);
            }
        }
    }

    private Object parseDate(String value, boolean endOfDay) {
        if (value.length() <= 10) {
            LocalDate date = LocalDate.parse(value);
            return endOfDay ? date.atTime(23, 59, 59) : date.atStartOfDay();
        }
        return LocalDateTime.parse(value.replace("T", " "));
    }

    private String toColumn(String field) {
        StringBuilder sb = new StringBuilder();
        for (char c : field.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
