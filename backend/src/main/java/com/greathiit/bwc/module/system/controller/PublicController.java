package com.greathiit.bwc.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.NoGenerator;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.common.RateLimitService;
import com.greathiit.bwc.module.education.entity.BwcNotice;
import com.greathiit.bwc.module.education.entity.BwcWarning;
import com.greathiit.bwc.module.education.mapper.BwcNoticeMapper;
import com.greathiit.bwc.module.education.mapper.BwcWarningMapper;
import com.greathiit.bwc.module.lost.entity.BwcLostItem;
import com.greathiit.bwc.module.lost.entity.BwcLostNotice;
import com.greathiit.bwc.module.lost.mapper.BwcLostItemMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostNoticeMapper;
import com.greathiit.bwc.module.system.entity.SysMenu;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.mapper.SysMenuMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.service.TenantService;
import com.greathiit.bwc.module.visitor.dto.VisitorCreateRequest;
import com.greathiit.bwc.module.visitor.entity.BwcVisitorAppointment;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitorAppointmentMapper;
import com.greathiit.bwc.module.visitor.service.VisitorService;
import com.greathiit.bwc.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 公开门户 / H5：学校门户、公告、失物公开页、访客公开预约、分享落地页。
 * 公开接口按 URL 中的租户编码临时设置 TenantContext，避免跨租户串数据；敏感字段仅返回掩码。
 */
@RestController
@RequestMapping({"/api/public", "/api/portal"})
@RequiredArgsConstructor
public class PublicController {

    private final SysTenantMapper tenantMapper;
    private final SysMenuMapper menuMapper;
    private final BwcNoticeMapper noticeMapper;
    private final BwcWarningMapper warningMapper;
    private final BwcLostItemMapper lostItemMapper;
    private final BwcLostNoticeMapper lostNoticeMapper;
    private final TenantService tenantService;
    private final VisitorService visitorService;
    private final BwcVisitorAppointmentMapper visitorAppointmentMapper;
    private final NoGenerator noGenerator;
    private final RateLimitService rateLimitService;

    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "bwc-public-portal");
        result.put("time", LocalDateTime.now());
        return R.ok(result);
    }

    @GetMapping("/tenant/{code}")
    public R<Map<String, Object>> tenant(@PathVariable String code) {
        return withTenant(code, () -> {
            SysTenant tenant = tenantService.requireTenant(TenantContext.getTenantId());
            Map<String, String> config = tenantService.tenantConfig(tenant.getId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", tenant.getId());
            result.put("name", tenant.getName());
            result.put("code", tenant.getCode());
            result.put("type", tenant.getType());
            result.put("status", tenant.getStatus());
            result.put("config", config);
            result.put("intro", config.getOrDefault("portal_intro", "校园安全，人人有责。"));
            result.put("dutyPhone", config.get("duty_phone"));
            result.put("logo", config.get("logo"));
            return R.ok(result);
        });
    }

    @GetMapping("/tenant/{code}/menus")
    public R<List<SysMenu>> menus(@PathVariable String code,
                                   @RequestParam(defaultValue = "h5") String platform) {
        return withTenant(code, () -> R.ok(menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPlatform, platform)
                .eq(SysMenu::getVisible, 1)
                .in(SysMenu::getTenantId, List.of(0L, TenantContext.getTenantId()))
                .orderByAsc(SysMenu::getSort))));
    }

    @GetMapping("/tenant/{code}/notices")
    public R<List<BwcNotice>> notices(@PathVariable String code,
                                       @RequestParam(defaultValue = "20") int limit) {
        return withTenant(code, () -> R.ok(noticeMapper.selectList(new LambdaQueryWrapper<BwcNotice>()
                .eq(BwcNotice::getStatus, "PUBLISHED")
                .orderByDesc(BwcNotice::getPublishTime)
                .last("limit " + Math.min(Math.max(limit, 1), 100)))));
    }

    @GetMapping("/tenant/{code}/notices/{id}")
    public R<BwcNotice> notice(@PathVariable String code, @PathVariable Long id) {
        return withTenant(code, () -> {
            BwcNotice notice = noticeMapper.selectById(id);
            if (notice == null || !"PUBLISHED".equals(notice.getStatus())) {
                throw BizException.notFound("公告不存在");
            }
            notice.setViews((notice.getViews() == null ? 0 : notice.getViews()) + 1);
            noticeMapper.updateById(notice);
            return R.ok(notice);
        });
    }

    @GetMapping("/tenant/{code}/warnings")
    public R<List<BwcWarning>> warnings(@PathVariable String code) {
        return withTenant(code, () -> R.ok(warningMapper.selectList(new LambdaQueryWrapper<BwcWarning>()
                .eq(BwcWarning::getStatus, "PUBLISHED")
                .orderByDesc(BwcWarning::getPublishTime)
                .last("limit 20"))));
    }

    @GetMapping("/tenant/{code}/lost")
    public R<List<BwcLostItem>> lost(@PathVariable String code,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) String keyword) {
        return withTenant(code, () -> {
            LambdaQueryWrapper<BwcLostItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(BwcLostItem::getStatus, List.of("FOUND", "PUBLISHED"));
            if (category != null && !category.isBlank()) {
                wrapper.eq(BwcLostItem::getCategory, category);
            }
            if (keyword != null && !keyword.isBlank()) {
                wrapper.and(w -> w.like(BwcLostItem::getName, keyword)
                        .or().like(BwcLostItem::getDescription, keyword)
                        .or().like(BwcLostItem::getLocation, keyword));
            }
            wrapper.orderByDesc(BwcLostItem::getFoundTime).last("limit 100");
            return R.ok(lostItemMapper.selectList(wrapper));
        });
    }

    @GetMapping("/tenant/{code}/lost-notices")
    public R<List<BwcLostNotice>> lostNotices(@PathVariable String code) {
        return withTenant(code, () -> R.ok(lostNoticeMapper.selectList(
                new LambdaQueryWrapper<BwcLostNotice>()
                        .eq(BwcLostNotice::getStatus, "PUBLISHED")
                        .orderByDesc(BwcLostNotice::getPublishTime)
                        .last("limit 50"))));
    }

    /**
     * 公开访客预约：未注册访客可直接提交，身份证号仅加密存储、响应仅返回掩码。
     */
    @PostMapping("/tenant/{code}/visitor-appointment")
    public R<Map<String, Object>> book(@PathVariable String code,
                                        @Valid @RequestBody VisitorCreateRequest request) {
        rateLimitService.check("public:visitor:" + code, 30, Duration.ofMinutes(10));
        return withTenant(code, () -> {
            request.setSource("H5");
            BwcVisitorAppointment appointment = visitorService.create(request, "H5");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", appointment.getId());
            result.put("visitorName", appointment.getVisitorName());
            result.put("phoneMask", appointment.getPhoneMask());
            result.put("idCardMask", appointment.getIdCardMask());
            result.put("visitTime", appointment.getVisitTime());
            result.put("status", appointment.getStatus());
            result.put("message", "预约已提交，请等待被访人审批；审批通过后凭通行码入校。");
            return R.ok(result);
        });
    }

    /**
     * 公开捡拾登记：任何人可登记拾获物品，进入失物招领台账待审核发布。
     */
    @PostMapping("/tenant/{code}/lost-report")
    public R<Map<String, Object>> lostReport(@PathVariable String code,
                                              @RequestBody Map<String, String> body) {
        rateLimitService.check("public:lost:" + code, 20, Duration.ofMinutes(10));
        return withTenant(code, () -> {
            String name = body.get("name");
            if (name == null || name.isBlank()) {
                throw new BizException(400, "物品名称不能为空");
            }
            BwcLostItem item = new BwcLostItem();
            item.setTenantId(TenantContext.getTenantId());
            item.setItemNo(noGenerator.next("LOST"));
            item.setName(name);
            item.setCategory(body.getOrDefault("category", "OTHER"));
            item.setLocation(body.get("location"));
            item.setFoundTime(LocalDateTime.now());
            item.setDescription(body.get("description"));
            item.setFinder(body.get("finder"));
            item.setStorageLocation(body.get("storageLocation"));
            item.setStatus("FOUND");
            lostItemMapper.insert(item);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", item.getId());
            result.put("itemNo", item.getItemNo());
            result.put("message", "登记成功，请将物品交至保卫处失物招领点。");
            return R.ok(result);
        });
    }

    /**
     * 分享落地页元数据：预约/失物/预警链接在微信内直接打开。
     */
    @GetMapping("/tenant/{code}/share/{type}/{id}")
    public R<Map<String, Object>> share(@PathVariable String code,
                                         @PathVariable String type,
                                         @PathVariable Long id) {
        return withTenant(code, () -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("type", type);
            result.put("id", id);
            result.put("source", "wechat-share");
            switch (type) {
                case "visitor" -> {
                    BwcVisitorAppointment appointment = visitorAppointmentMapper.selectById(id);
                    result.put("title", appointment == null ? "访客预约通行"
                            : appointment.getVisitorName() + " 的访客预约");
                    result.put("description", "凭通行码在预约时段内入校，配合门卫核验。");
                    result.put("path", "/visitor/" + id);
                }
                case "lost" -> {
                    BwcLostItem item = lostItemMapper.selectById(id);
                    result.put("title", item == null ? "失物招领" : item.getName());
                    result.put("description", item == null ? "校园失物招领" : item.getDescription());
                    result.put("path", "/lost/" + id);
                }
                case "warning" -> {
                    BwcWarning warning = warningMapper.selectById(id);
                    result.put("title", warning == null ? "安全预警" : warning.getTitle());
                    result.put("description", warning == null ? "" : warning.getContent());
                    result.put("path", "/news/" + id);
                }
                default -> {
                    result.put("title", "校园安全门户");
                    result.put("description", "校园保卫安防服务");
                    result.put("path", "/");
                }
            }
            return R.ok(result);
        });
    }

    private <T> T withTenant(String code, java.util.function.Supplier<T> action) {
        SysTenant tenant = tenantMapper.selectOne(new LambdaQueryWrapper<SysTenant>()
                .eq(SysTenant::getCode, code));
        if (tenant == null) {
            throw BizException.notFound("学校不存在：" + code);
        }
        if (!"NORMAL".equals(tenant.getStatus()) && !"TRIAL".equals(tenant.getStatus())) {
            throw BizException.forbidden("该学校门户已停用");
        }
        TenantContext.set(tenant.getId(), false, false, false);
        try {
            return action.get();
        } finally {
            TenantContext.clear();
        }
    }
}
