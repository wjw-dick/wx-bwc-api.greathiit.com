package com.greathiit.bwc.module.visitor.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.ExcelUtil;
import com.greathiit.bwc.common.QrCodeUtil;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.visitor.dto.VisitorCreateRequest;
import com.greathiit.bwc.module.visitor.entity.BwcLateReturn;
import com.greathiit.bwc.module.visitor.entity.BwcTempEntry;
import com.greathiit.bwc.module.visitor.entity.BwcVisitRecord;
import com.greathiit.bwc.module.visitor.entity.BwcVisitorAppointment;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitRecordMapper;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitorAppointmentMapper;
import com.greathiit.bwc.module.visitor.service.VisitorService;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.RequirePermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门禁访客：预约、审批、通行码、扫码通行、台账、统计、导出。
 */
@RestController
@RequestMapping("/api/visitor")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;
    private final BwcVisitorAppointmentMapper appointmentMapper;
    private final BwcVisitRecordMapper visitRecordMapper;
    private final AuditService auditService;

    @GetMapping("/appointments")
    @RequirePermission("visitor:read")
    public R<IPage<BwcVisitorAppointment>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return R.ok(visitorService.page(page, size, status, keyword, start, end));
    }

    @PostMapping("/appointments")
    @RequirePermission("visitor:write")
    public R<BwcVisitorAppointment> create(@Valid @RequestBody VisitorCreateRequest request) {
        return R.ok(visitorService.create(request, request.getSource()));
    }

    @PostMapping("/appointments/{id}/approve")
    @RequirePermission("visitor:approve")
    public R<BwcVisitorAppointment> approve(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", false)));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(visitorService.approve(id, approved, remark));
    }

    @PostMapping("/appointments/{id}/cancel")
    @RequirePermission("visitor:write")
    public R<Void> cancel(@PathVariable Long id) {
        BwcVisitorAppointment entity = appointmentMapper.selectById(id);
        if (entity == null) {
            throw BizException.notFound("预约不存在");
        }
        entity.setStatus("CANCELLED");
        appointmentMapper.updateById(entity);
        auditService.record("visitor", "CANCEL", String.valueOf(id), "取消预约");
        return R.ok();
    }

    @GetMapping("/appointments/{id}/qr")
    @RequirePermission("visitor:read")
    public R<Map<String, Object>> passQr(@PathVariable Long id) {
        BwcVisitorAppointment entity = appointmentMapper.selectById(id);
        if (entity == null || entity.getPassCode() == null) {
            throw BizException.notFound("通行码不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("passCode", entity.getPassCode());
        result.put("expireTime", entity.getPassExpireTime());
        result.put("qrBase64", java.util.Base64.getEncoder()
                .encodeToString(QrCodeUtil.png(entity.getPassCode(), 320)));
        return R.ok(result);
    }

    @GetMapping(value = "/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @RequirePermission("visitor:read")
    public byte[] qr(@RequestParam String content,
                     @RequestParam(defaultValue = "240") int size) {
        return QrCodeUtil.png(content, Math.min(size, 640));
    }

    @PostMapping("/check")
    @RequirePermission("visitor:write")
    public R<Map<String, Object>> check(@RequestBody Map<String, String> body) {
        return R.ok(visitorService.checkPass(body.get("passCode"), body.get("gate"),
                AuthContext.username(), body.getOrDefault("method", "QR")));
    }

    @GetMapping("/records")
    @RequirePermission("visitor:read")
    public R<IPage<BwcVisitRecord>> records(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BwcVisitRecord> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcVisitRecord::getVisitorName, keyword)
                    .or().like(BwcVisitRecord::getPassCode, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcVisitRecord::getStatus, status);
        }
        wrapper.orderByDesc(BwcVisitRecord::getId);
        return R.ok(visitRecordMapper.selectPage(new Page<>(page, size), wrapper));
    }

    // ------------------------------------------------------------------ 临时入校

    @GetMapping("/temp-entries")
    @RequirePermission("visitor:read")
    public R<IPage<BwcTempEntry>> tempEntries(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) String keyword) {
        return R.ok(visitorService.tempEntryPage(page, size, status, keyword));
    }

    @PostMapping("/temp-entry")
    @RequirePermission("visitor:write")
    public R<BwcTempEntry> createTempEntry(@RequestBody Map<String, String> body) {
        return R.ok(visitorService.createTempEntry(body.get("visitorName"), body.get("idCard"),
                body.get("phone"), body.get("reason"), body.get("visitUser")));
    }

    @PostMapping("/temp-entry/{id}/out")
    @RequirePermission("visitor:write")
    public R<BwcTempEntry> outTempEntry(@PathVariable Long id) {
        return R.ok(visitorService.outTempEntry(id));
    }

    // ------------------------------------------------------------------ 晚归登记

    @GetMapping("/late-returns")
    @RequirePermission("visitor:read")
    public R<IPage<BwcLateReturn>> lateReturns(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String confirmStatus) {
        return R.ok(visitorService.lateReturnPage(page, size, confirmStatus));
    }

    @PostMapping("/late-return")
    @RequirePermission("visitor:write")
    public R<BwcLateReturn> reportLateReturn(@RequestBody BwcLateReturn body) {
        return R.ok(visitorService.reportLateReturn(body));
    }

    @PostMapping("/late-return/{id}/confirm")
    @RequirePermission("visitor:write")
    public R<BwcLateReturn> confirmLateReturn(@PathVariable Long id) {
        return R.ok(visitorService.confirmLateReturn(id, AuthContext.username()));
    }

    @GetMapping("/stats")
    @RequirePermission("visitor:read")
    public R<Map<String, Object>> stats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return R.ok(visitorService.stats(start, end));
    }

    @GetMapping("/export")
    @RequirePermission("visitor:read")
    public void export(@RequestParam(required = false) String status,
                       HttpServletResponse response) throws IOException {
        List<Map<String, ?>> rows = new ArrayList<>();
        for (BwcVisitorAppointment item : visitorService.listForExport(status)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("visitorName", item.getVisitorName());
            row.put("phoneMask", item.getPhoneMask());
            row.put("idCardMask", item.getIdCardMask());
            row.put("reason", item.getReason());
            row.put("visitTime", item.getVisitTime());
            row.put("visitUserName", item.getVisitUserName());
            row.put("status", item.getStatus());
            row.put("passCode", item.getPassCode());
            rows.add(row);
        }
        ExcelUtil.write(response, "访客台账",
                List.of("访客姓名", "手机号", "身份证号", "来访事由", "来访时间", "被访人", "状态", "通行码"),
                List.of("visitorName", "phoneMask", "idCardMask", "reason", "visitTime",
                        "visitUserName", "status", "passCode"), rows);
        auditService.record("visitor", "EXPORT", "visitor", "导出访客台账");
    }
}
