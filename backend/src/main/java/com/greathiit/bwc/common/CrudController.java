package com.greathiit.bwc.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final ResourceRegistry registry;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;

    @GetMapping("/{resource}")
    public R<IPage<Object>> list(@PathVariable String resource,
                                 @RequestParam(defaultValue = "1") long page,
                                 @RequestParam(defaultValue = "10") long size,
                                 @RequestParam(required = false) String orderBy,
                                 @RequestParam(required = false) String keyword,
                                 @RequestParam Map<String, String> params) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        permissionService.require(def.getModule() + ":read");
        QueryWrapper<Object> wrapper = buildWrapper(def, params, keyword);
        applyOrder(wrapper, orderBy);
        IPage<Object> result = def.getMapper().selectPage(new Page<>(page, Math.min(size, 500)), wrapper);
        return R.ok(result);
    }

    @GetMapping("/{resource}/all")
    public R<List<Object>> all(@PathVariable String resource,
                               @RequestParam(required = false) Map<String, String> params,
                               @RequestParam(required = false) String keyword) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        permissionService.require(def.getModule() + ":read");
        QueryWrapper<Object> wrapper = buildWrapper(def, params, keyword);
        wrapper.orderByDesc("id");
        return R.ok(def.getMapper().selectList(wrapper));
    }

    @GetMapping("/{resource}/{id}")
    public R<Object> get(@PathVariable String resource, @PathVariable Long id) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        permissionService.require(def.getModule() + ":read");
        Object entity = def.getMapper().selectById(id);
        if (entity == null) {
            throw BizException.notFound("数据不存在或无权访问");
        }
        return R.ok(entity);
    }

    @PostMapping("/{resource}")
    public R<Object> create(@PathVariable String resource, @RequestBody Map<String, Object> body) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        permissionService.require(def.getModule() + ":write");
        Object entity = toEntity(def, body, null);
        def.getMapper().insert(entity);
        return R.ok(entity);
    }

    @PutMapping("/{resource}/{id}")
    public R<Object> update(@PathVariable String resource, @PathVariable Long id,
                            @RequestBody Map<String, Object> body) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        permissionService.require(def.getModule() + ":write");
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
        permissionService.require(def.getModule() + ":write");
        int rows = def.getMapper().deleteById(id);
        if (rows == 0) {
            throw BizException.notFound("数据不存在或无权访问");
        }
        return R.ok();
    }

    @PostMapping("/{resource}/batch-delete")
    public R<Integer> batchDelete(@PathVariable String resource, @RequestBody List<Long> ids) {
        ResourceRegistry.ResourceDef def = registry.get(resource);
        permissionService.require(def.getModule() + ":write");
        int rows = def.getMapper().deleteBatchIds(ids);
        return R.ok(rows);
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
                String field = toColumn(key.substring(0, key.length() - 5));
                if (start) {
                    wrapper.ge(field, parseDate(value, false));
                } else {
                    wrapper.le(field, parseDate(value, true));
                }
                continue;
            }
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

    private void applyOrder(QueryWrapper<Object> wrapper, String orderBy) {
        if (orderBy == null || orderBy.isBlank()) {
            wrapper.orderByDesc("id");
            return;
        }
        for (String item : orderBy.split(",")) {
            String[] parts = item.split(":");
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
