package com.greathiit.bwc.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.common.NoGenerator;
import com.greathiit.bwc.common.QrCodeUtil;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.common.RateLimitService;
import com.greathiit.bwc.module.device.entity.BwcDeviceRepair;
import com.greathiit.bwc.module.device.mapper.BwcDeviceRepairMapper;
import com.greathiit.bwc.module.education.entity.BwcNotice;
import com.greathiit.bwc.module.education.entity.BwcWarning;
import com.greathiit.bwc.module.education.mapper.BwcNoticeMapper;
import com.greathiit.bwc.module.education.mapper.BwcWarningMapper;
import com.greathiit.bwc.module.event.entity.BwcSecurityEvent;
import com.greathiit.bwc.module.event.mapper.BwcSecurityEventMapper;
import com.greathiit.bwc.module.fire.entity.BwcHazard;
import com.greathiit.bwc.module.fire.mapper.BwcHazardMapper;
import com.greathiit.bwc.module.lost.entity.BwcLostClaim;
import com.greathiit.bwc.module.lost.entity.BwcLostItem;
import com.greathiit.bwc.module.lost.entity.BwcLostNotice;
import com.greathiit.bwc.module.lost.mapper.BwcLostClaimMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostItemMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostNoticeMapper;
import com.greathiit.bwc.module.lost.service.LostService;
import com.greathiit.bwc.module.system.entity.SysMenu;
import com.greathiit.bwc.module.system.entity.SysMessageTemplate;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import com.greathiit.bwc.module.system.mapper.SysMenuMapper;
import com.greathiit.bwc.module.system.mapper.SysMessageTemplateMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantUserMapper;
import com.greathiit.bwc.module.system.service.TenantService;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicleAppointment;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleAppointmentMapper;
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
    private final SysMessageTemplateMapper messageTemplateMapper;
    private final BwcNoticeMapper noticeMapper;
    private final BwcWarningMapper warningMapper;
    private final BwcLostItemMapper lostItemMapper;
    private final BwcLostNoticeMapper lostNoticeMapper;
    private final TenantService tenantService;
    private final VisitorService visitorService;
    private final BwcVisitorAppointmentMapper visitorAppointmentMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final BwcVehicleAppointmentMapper vehicleAppointmentMapper;
    private final BwcSecurityEventMapper securityEventMapper;
    private final BwcHazardMapper hazardMapper;
    private final BwcDeviceRepairMapper deviceRepairMapper;
    private final BwcLostClaimMapper lostClaimMapper;
    private final LostService lostService;
    private final CryptoService cryptoService;
    private final NoGenerator noGenerator;
    private final RateLimitService rateLimitService;

    private static final java.time.format.DateTimeFormatter TIME_FMT =
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
            result.put("config", publicTenantConfig(config));
            result.put("intro", config.getOrDefault("portal_intro", "校园安全，人人有责。"));
            result.put("dutyPhone", config.get("duty_phone"));
            result.put("logo", config.get("logo"));
            return R.ok(result);
        });
    }

    /**
     * 公开页仅暴露学校展示类配置，避免租户配置中的支付/存储/通知密钥进入匿名响应。
     */
    private Map<String, String> publicTenantConfig(Map<String, String> config) {
        Map<String, String> safe = new LinkedHashMap<>();
        List<String> keys = List.of("school_name", "portal_intro", "duty_phone", "logo",
                "notice_enabled", "official_account", "miniprogram", "parking_fee_enabled");
        for (String key : keys) {
            if (config.containsKey(key)) {
                safe.put(key, config.get(key));
            }
        }
        return safe;
    }

    @GetMapping("/tenant/{code}/subscribe-templates")
    public R<List<Map<String, Object>>> subscribeTemplates(@PathVariable String code) {
        return withTenant(code, () -> {
            List<SysMessageTemplate> templates = messageTemplateMapper.selectList(
                    new LambdaQueryWrapper<SysMessageTemplate>()
                            .eq(SysMessageTemplate::getChannel, "MINI")
                            .eq(SysMessageTemplate::getEnabled, 1));
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (SysMessageTemplate template : templates) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("bizType", template.getBizType());
                item.put("templateId", template.getTemplateId());
                item.put("title", template.getTitle());
                item.put("fieldsJson", template.getFieldsJson());
                result.add(item);
            }
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
     * 被访人选择：公开只返回姓名/部门/身份，不返回手机号等敏感信息。
     */
    @GetMapping("/tenant/{code}/visitees")
    public R<List<Map<String, Object>>> visitees(@PathVariable String code,
                                                  @RequestParam(required = false) String keyword) {
        return withTenant(code, () -> {
            LambdaQueryWrapper<SysTenantUser> wrapper = new LambdaQueryWrapper<SysTenantUser>()
                    .eq(SysTenantUser::getStatus, "NORMAL")
                    .in(SysTenantUser::getUserType, List.of("STAFF", "STUDENT", "VISITEE", "ADMIN"))
                    .orderByAsc(SysTenantUser::getId)
                    .last("limit 50");
            if (keyword != null && !keyword.isBlank()) {
                wrapper.and(w -> w.like(SysTenantUser::getRealName, keyword)
                        .or().like(SysTenantUser::getDept, keyword));
            }
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (SysTenantUser user : tenantUserMapper.selectList(wrapper)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", user.getId());
                item.put("name", user.getRealName());
                item.put("dept", user.getDept());
                item.put("userType", user.getUserType());
                result.add(item);
            }
            return R.ok(result);
        });
    }

    /**
     * 访客预约进度查询：预约编号 + 预留手机号，审批通过后返回通行码与二维码。
     */
    @GetMapping("/tenant/{code}/visitor-appointment/query")
    public R<Map<String, Object>> queryAppointment(@PathVariable String code,
                                                    @RequestParam Long id,
                                                    @RequestParam String phone) {
        rateLimitService.check("public:visitor:query:" + code, 60, Duration.ofMinutes(10));
        return withTenant(code, () -> {
            BwcVisitorAppointment entity = visitorAppointmentMapper.selectById(id);
            if (entity == null || !cryptoService.maskPhone(phone).equals(entity.getPhoneMask())) {
                throw BizException.notFound("预约不存在，请核对预约编号与手机号");
            }
            if ("APPROVED".equals(entity.getStatus()) && entity.getPassExpireTime() != null
                    && entity.getPassExpireTime().isBefore(LocalDateTime.now())) {
                entity.setStatus("EXPIRED");
                visitorAppointmentMapper.updateById(entity);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", entity.getId());
            result.put("visitorName", entity.getVisitorName());
            result.put("phoneMask", entity.getPhoneMask());
            result.put("idCardMask", entity.getIdCardMask());
            result.put("visitTime", entity.getVisitTime());
            result.put("leaveTime", entity.getLeaveTime());
            result.put("visitUserName", entity.getVisitUserName());
            result.put("status", entity.getStatus());
            result.put("approveRemark", entity.getApproveRemark());
            result.put("passCode", entity.getPassCode());
            result.put("passExpireTime", entity.getPassExpireTime());
            if ("APPROVED".equals(entity.getStatus()) && entity.getPassCode() != null) {
                result.put("qrBase64", java.util.Base64.getEncoder()
                        .encodeToString(QrCodeUtil.png(entity.getPassCode(), 320)));
            }
            return R.ok(result);
        });
    }

    /**
     * 公开车辆预约：外来车辆提前登记，审批通过后自动下发临时车牌。
     */
    @PostMapping("/tenant/{code}/vehicle-appointment")
    public R<Map<String, Object>> bookVehicle(@PathVariable String code,
                                               @RequestBody Map<String, String> body) {
        rateLimitService.check("public:vehicle:" + code, 30, Duration.ofMinutes(10));
        String plate = body.getOrDefault("plateNo", "").trim().toUpperCase();
        String phone = body.getOrDefault("phone", "").trim();
        String name = body.getOrDefault("visitorName", "").trim();
        if (plate.isBlank() || name.isBlank()) {
            throw new BizException(400, "车牌号与驾驶人不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(400, "请输入正确的手机号");
        }
        return withTenant(code, () -> {
            BwcVehicleAppointment appointment = new BwcVehicleAppointment();
            appointment.setPlateNo(plate);
            appointment.setVisitorName(name);
            appointment.setPhoneMask(cryptoService.maskPhone(phone));
            appointment.setReason(body.get("reason"));
            appointment.setVisitTime(parseTime(body.get("visitTime"),
                    LocalDateTime.now().plusHours(2)));
            appointment.setLeaveTime(parseTime(body.get("leaveTime"),
                    appointment.getVisitTime().plusHours(4)));
            appointment.setStatus("PENDING");
            appointment.setAuthStatus("NONE");
            appointment.setRemark("H5 公开预约");
            vehicleAppointmentMapper.insert(appointment);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", appointment.getId());
            result.put("plateNo", appointment.getPlateNo());
            result.put("phoneMask", appointment.getPhoneMask());
            result.put("visitTime", appointment.getVisitTime());
            result.put("status", appointment.getStatus());
            result.put("message", "车辆预约已提交，审批通过后将自动下发临时车牌。");
            return R.ok(result);
        });
    }

    @GetMapping("/tenant/{code}/vehicle-appointment/query")
    public R<Map<String, Object>> queryVehicle(@PathVariable String code,
                                                @RequestParam Long id,
                                                @RequestParam String phone) {
        rateLimitService.check("public:vehicle:query:" + code, 60, Duration.ofMinutes(10));
        return withTenant(code, () -> {
            BwcVehicleAppointment entity = vehicleAppointmentMapper.selectById(id);
            if (entity == null || !cryptoService.maskPhone(phone).equals(entity.getPhoneMask())) {
                throw BizException.notFound("预约不存在，请核对预约编号与手机号");
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", entity.getId());
            result.put("plateNo", entity.getPlateNo());
            result.put("visitorName", entity.getVisitorName());
            result.put("phoneMask", entity.getPhoneMask());
            result.put("reason", entity.getReason());
            result.put("visitTime", entity.getVisitTime());
            result.put("leaveTime", entity.getLeaveTime());
            result.put("status", entity.getStatus());
            result.put("authStatus", entity.getAuthStatus());
            result.put("remark", entity.getRemark());
            return R.ok(result);
        });
    }

    /**
     * 安全随手拍：治安事件 / 消防隐患 / 设备故障统一上报，返回受理编号供查询进度。
     */
    @PostMapping("/tenant/{code}/safety-report")
    public R<Map<String, Object>> safetyReport(@PathVariable String code,
                                                @RequestBody Map<String, String> body) {
        String category = body.getOrDefault("category", "SECURITY").toUpperCase();
        String reporter = body.getOrDefault("reporter", "").trim();
        String phone = body.getOrDefault("phone", "").trim();
        String location = body.getOrDefault("location", "").trim();
        String description = body.getOrDefault("description", "").trim();
        if (reporter.isBlank() || location.isBlank() || description.isBlank()) {
            throw new BizException(400, "上报人、位置和描述不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(400, "请输入正确的手机号");
        }
        rateLimitService.check("public:safety:" + code + ":" + phone, 10, Duration.ofMinutes(10));
        return withTenant(code, () -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("category", category);
            result.put("reporter", reporter);
            result.put("phoneMask", cryptoService.maskPhone(phone));
            if ("FIRE".equals(category)) {
                BwcHazard hazard = new BwcHazard();
                hazard.setTenantId(TenantContext.getTenantId());
                hazard.setHazardNo(noGenerator.next("HZ"));
                hazard.setSource("H5");
                hazard.setHazardLevel(body.getOrDefault("level", "GENERAL"));
                hazard.setLocation(location);
                hazard.setDescription(description);
                hazard.setReporter(reporter);
                hazard.setReportTime(LocalDateTime.now());
                hazard.setStatus("REPORTED");
                hazardMapper.insert(hazard);
                result.put("id", hazard.getId());
                result.put("reportNo", hazard.getHazardNo());
                result.put("message", "消防隐患已上报，保卫处将尽快核实处理。");
            } else if ("DEVICE".equals(category)) {
                BwcDeviceRepair repair = new BwcDeviceRepair();
                repair.setTenantId(TenantContext.getTenantId());
                repair.setRepairNo(noGenerator.next("DR"));
                repair.setDeviceName(body.getOrDefault("deviceName", location));
                repair.setFaultDesc(description);
                repair.setReporter(reporter);
                repair.setReportTime(LocalDateTime.now());
                repair.setStatus("PENDING");
                deviceRepairMapper.insert(repair);
                result.put("id", repair.getId());
                result.put("reportNo", repair.getRepairNo());
                result.put("message", "设备故障已报修，维修人员将尽快处理。");
            } else {
                BwcSecurityEvent event = new BwcSecurityEvent();
                event.setTenantId(TenantContext.getTenantId());
                event.setEventNo(noGenerator.next("SE"));
                event.setEventType(body.getOrDefault("type", "其他"));
                event.setLocation(location);
                event.setDescription(description);
                event.setUrgency(body.getOrDefault("urgency", "NORMAL"));
                event.setReporter(reporter);
                event.setReporterPhoneMask(cryptoService.maskPhone(phone));
                event.setSource("H5");
                event.setStatus("REPORTED");
                securityEventMapper.insert(event);
                result.put("id", event.getId());
                result.put("reportNo", event.getEventNo());
                result.put("message", "事件已上报，保卫处将尽快处置。");
            }
            return R.ok(result);
        });
    }

    @GetMapping("/tenant/{code}/safety-report/query")
    public R<Map<String, Object>> safetyQuery(@PathVariable String code,
                                               @RequestParam String category,
                                               @RequestParam(required = false) String reportNo,
                                               @RequestParam(required = false) Long id,
                                               @RequestParam String phone,
                                               @RequestParam(required = false) String reporter) {
        rateLimitService.check("public:safety:query:" + code, 60, Duration.ofMinutes(10));
        if ((reportNo == null || reportNo.isBlank()) && id == null) {
            throw new BizException(400, "请输入受理编号");
        }
        return withTenant(code, () -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("category", category);
            String mask = cryptoService.maskPhone(phone);
            switch (category.toUpperCase()) {
                case "FIRE" -> {
                    BwcHazard hazard = queryHazard(reportNo, id, reporter);
                    if (hazard == null || !isReporter(reporter, hazard.getReporter())) {
                        throw BizException.notFound("记录不存在，请核对受理编号与上报人");
                    }
                    result.put("id", hazard.getId());
                    result.put("reportNo", hazard.getHazardNo());
                    result.put("status", hazard.getStatus());
                    result.put("statusText", statusText(hazard.getStatus()));
                    result.put("createTime", hazard.getReportTime());
                    result.put("result", hazard.getRectifyPlan());
                }
                case "DEVICE" -> {
                    BwcDeviceRepair repair = queryRepair(reportNo, id, reporter);
                    if (repair == null || !isReporter(reporter, repair.getReporter())) {
                        throw BizException.notFound("记录不存在，请核对受理编号与上报人");
                    }
                    result.put("id", repair.getId());
                    result.put("reportNo", repair.getRepairNo());
                    result.put("status", repair.getStatus());
                    result.put("statusText", statusText(repair.getStatus()));
                    result.put("createTime", repair.getReportTime());
                    result.put("result", repair.getResult());
                }
                default -> {
                    BwcSecurityEvent event = queryEvent(reportNo, id, mask);
                    if (event == null || !mask.equals(event.getReporterPhoneMask())) {
                        throw BizException.notFound("记录不存在，请核对受理编号与手机号");
                    }
                    result.put("id", event.getId());
                    result.put("reportNo", event.getEventNo());
                    result.put("status", event.getStatus());
                    result.put("statusText", statusText(event.getStatus()));
                    result.put("createTime", event.getCreateTime());
                    result.put("result", event.getHandleResult());
                }
            }
            return R.ok(result);
        });
    }

    private BwcHazard queryHazard(String reportNo, Long id, String reporter) {
        if (reportNo != null && !reportNo.isBlank()) {
            List<BwcHazard> list = hazardMapper.selectList(new LambdaQueryWrapper<BwcHazard>()
                    .eq(BwcHazard::getHazardNo, reportNo.trim()).orderByDesc(BwcHazard::getId));
            for (BwcHazard item : list) {
                if (isReporter(reporter, item.getReporter())) {
                    return item;
                }
            }
            return list.isEmpty() ? null : list.get(0);
        }
        return hazardMapper.selectById(id);
    }

    private BwcDeviceRepair queryRepair(String reportNo, Long id, String reporter) {
        if (reportNo != null && !reportNo.isBlank()) {
            List<BwcDeviceRepair> list = deviceRepairMapper.selectList(
                    new LambdaQueryWrapper<BwcDeviceRepair>()
                            .eq(BwcDeviceRepair::getRepairNo, reportNo.trim())
                            .orderByDesc(BwcDeviceRepair::getId));
            for (BwcDeviceRepair item : list) {
                if (isReporter(reporter, item.getReporter())) {
                    return item;
                }
            }
            return list.isEmpty() ? null : list.get(0);
        }
        return deviceRepairMapper.selectById(id);
    }

    private BwcSecurityEvent queryEvent(String reportNo, Long id, String phoneMask) {
        if (reportNo != null && !reportNo.isBlank()) {
            List<BwcSecurityEvent> list = securityEventMapper.selectList(
                    new LambdaQueryWrapper<BwcSecurityEvent>()
                            .eq(BwcSecurityEvent::getEventNo, reportNo.trim())
                            .orderByDesc(BwcSecurityEvent::getId));
            for (BwcSecurityEvent item : list) {
                if (phoneMask != null && phoneMask.equals(item.getReporterPhoneMask())) {
                    return item;
                }
            }
            return null;
        }
        return securityEventMapper.selectById(id);
    }

    /**
     * 公开失物认领申请：失主在线提交，保卫处核验后线下领取。
     */
    @PostMapping("/tenant/{code}/lost-claim")
    public R<Map<String, Object>> lostClaim(@PathVariable String code,
                                             @RequestBody Map<String, String> body) {
        rateLimitService.check("public:lost-claim:" + code, 20, Duration.ofMinutes(10));
        String phone = body.getOrDefault("phone", "").trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(400, "请输入正确的手机号");
        }
        Long itemId;
        try {
            itemId = Long.valueOf(body.getOrDefault("itemId", ""));
        } catch (Exception e) {
            throw new BizException(400, "请选择要认领的物品");
        }
        return withTenant(code, () -> {
            BwcLostItem item = lostItemMapper.selectById(itemId);
            if (item == null) {
                throw BizException.notFound("物品不存在");
            }
            BwcLostClaim claim = new BwcLostClaim();
            claim.setTenantId(TenantContext.getTenantId());
            claim.setItemId(itemId);
            claim.setClaimant(body.getOrDefault("claimant", "").trim());
            claim.setPhoneMask(cryptoService.maskPhone(phone));
            claim.setIdCardMask(cryptoService.maskIdCard(body.get("idCard")));
            claim.setRemark(body.get("remark"));
            BwcLostClaim created = lostService.claim(claim);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", created.getId());
            result.put("phoneMask", created.getPhoneMask());
            result.put("status", created.getStatus());
            result.put("message", "认领申请已提交，请携带有效证件到保卫处核验领取。");
            return R.ok(result);
        });
    }

    @GetMapping("/tenant/{code}/lost-claim/query")
    public R<Map<String, Object>> lostClaimQuery(@PathVariable String code,
                                                  @RequestParam(required = false) Long id,
                                                  @RequestParam(required = false) String itemNo,
                                                  @RequestParam String phone) {
        rateLimitService.check("public:lost-claim:query:" + code, 60, Duration.ofMinutes(10));
        if (id == null && (itemNo == null || itemNo.isBlank())) {
            throw new BizException(400, "请输入认领编号或物品编号");
        }
        return withTenant(code, () -> {
            String mask = cryptoService.maskPhone(phone);
            BwcLostClaim claim = id == null ? null : lostClaimMapper.selectById(id);
            if (claim == null && itemNo != null && !itemNo.isBlank()) {
                List<BwcLostItem> items = lostItemMapper.selectList(new LambdaQueryWrapper<BwcLostItem>()
                        .eq(BwcLostItem::getItemNo, itemNo.trim()).orderByDesc(BwcLostItem::getId));
                for (BwcLostItem item : items) {
                    claim = lostClaimMapper.selectOne(new LambdaQueryWrapper<BwcLostClaim>()
                            .eq(BwcLostClaim::getItemId, item.getId())
                            .eq(BwcLostClaim::getPhoneMask, mask)
                            .orderByDesc(BwcLostClaim::getId)
                            .last("limit 1"));
                    if (claim != null) {
                        break;
                    }
                }
            }
            if (claim == null || !mask.equals(claim.getPhoneMask())) {
                throw BizException.notFound("认领记录不存在，请核对编号与手机号");
            }
            BwcLostItem item = lostItemMapper.selectById(claim.getItemId());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", claim.getId());
            result.put("status", claim.getStatus());
            result.put("statusText", "APPROVED".equals(claim.getStatus()) ? "已通过"
                    : "REJECTED".equals(claim.getStatus()) ? "已驳回" : "待核验");
            result.put("claimTime", claim.getClaimTime());
            result.put("remark", claim.getRemark());
            result.put("itemName", item == null ? null : item.getName());
            result.put("itemNo", item == null ? null : item.getItemNo());
            return R.ok(result);
        });
    }

    private boolean isReporter(String input, String stored) {
        // 隐患/报修表未存上报人手机号，公开查询以「编号 + 上报人姓名」校验
        return input != null && !input.isBlank() && input.trim().equals(stored);
    }

    private String statusText(String status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case "REPORTED" -> "已上报";
            case "ASSIGNED" -> "已分派";
            case "HANDLING", "REPAIRING", "RESPONDING" -> "处理中";
            case "RECTIFYING" -> "整改中";
            case "PENDING_ACCEPT" -> "待验收";
            case "CLOSED", "DONE" -> "已办结";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }

    private LocalDateTime parseTime(String value, LocalDateTime fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDateTime.parse(value.replace("T", " ").substring(0, 19), TIME_FMT);
        } catch (Exception e) {
            return fallback;
        }
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
            item.setFinderPhoneMask(cryptoService.maskPhone(body.get("phone")));
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
     * 公开寻物启事：失主发布丢失信息，拾获人可在失物公告中看到并联系认领。
     */
    @PostMapping("/tenant/{code}/lost-post")
    public R<Map<String, Object>> lostPost(@PathVariable String code,
                                            @RequestBody Map<String, String> body) {
        rateLimitService.check("public:lost-post:" + code, 20, Duration.ofMinutes(10));
        String name = body.getOrDefault("name", "").trim();
        String reporter = body.getOrDefault("reporter", "").trim();
        String phone = body.getOrDefault("phone", "").trim();
        if (name.isBlank() || reporter.isBlank()) {
            throw new BizException(400, "物品名称与联系人不能为空");
        }
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(400, "请输入正确的手机号");
        }
        return withTenant(code, () -> {
            StringBuilder content = new StringBuilder();
            content.append("丢失物品：").append(name).append('\n');
            if (body.get("location") != null) {
                content.append("丢失地点：").append(body.get("location")).append('\n');
            }
            if (body.get("lostTime") != null) {
                content.append("丢失时间：").append(body.get("lostTime")).append('\n');
            }
            if (body.get("description") != null) {
                content.append("物品描述：").append(body.get("description")).append('\n');
            }
            content.append("联系人：").append(reporter)
                    .append("（").append(cryptoService.maskPhone(phone)).append("）");
            BwcLostNotice notice = new BwcLostNotice();
            notice.setTenantId(TenantContext.getTenantId());
            notice.setTitle("寻物：" + name);
            notice.setContent(content.toString());
            notice.setPublishTime(LocalDateTime.now());
            notice.setPublisher(reporter);
            notice.setStatus("PUBLISHED");
            notice.setViews(0);
            lostNoticeMapper.insert(notice);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", notice.getId());
            result.put("title", notice.getTitle());
            result.put("message", "寻物启事已发布，拾获人可在失物公告中看到。");
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
