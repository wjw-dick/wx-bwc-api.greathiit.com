package com.greathiit.bwc.module.logistics.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.logistics.entity.BwcAttendance;
import com.greathiit.bwc.module.logistics.entity.BwcFeedback;
import com.greathiit.bwc.module.logistics.entity.BwcMonthlyReport;
import com.greathiit.bwc.module.logistics.entity.BwcPerformance;
import com.greathiit.bwc.module.logistics.entity.BwcSchedule;
import com.greathiit.bwc.module.logistics.mapper.BwcFeedbackMapper;
import com.greathiit.bwc.module.logistics.service.LogisticsService;
import com.greathiit.bwc.security.AccessGuard;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.RequirePermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;

/**
 * 后勤：排班、考勤、绩效、投诉反馈、月报季报与 PDF。
 */
@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsService logisticsService;
    private final BwcFeedbackMapper feedbackMapper;
    private final AccessGuard accessGuard;

    @GetMapping("/schedules")
    @RequirePermission("logistics:read")
    public R<IPage<BwcSchedule>> schedules(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
            @RequestParam(required = false) Long guardId) {
        accessGuard.requireRead();
        if (accessGuard.isGuardRole()) {
            guardId = AuthContext.userId();
        }
        return R.ok(logisticsService.schedules(page, size, start, end, guardId));
    }

    @PostMapping("/schedules")
    @RequirePermission("logistics:write")
    public R<BwcSchedule> createSchedule(@RequestBody BwcSchedule schedule) {
        accessGuard.requireManager();
        return R.ok(logisticsService.saveSchedule(schedule, null));
    }

    @PutMapping("/schedules/{id}")
    @RequirePermission("logistics:write")
    public R<BwcSchedule> updateSchedule(@PathVariable Long id, @RequestBody BwcSchedule schedule) {
        accessGuard.requireManager();
        return R.ok(logisticsService.saveSchedule(schedule, id));
    }

    @DeleteMapping("/schedules/{id}")
    @RequirePermission("logistics:write")
    public R<Void> deleteSchedule(@PathVariable Long id) {
        accessGuard.requireManager();
        logisticsService.deleteSchedule(id);
        return R.ok();
    }

    @GetMapping("/attendance")
    @RequirePermission("logistics:read")
    public R<IPage<BwcAttendance>> attendance(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        accessGuard.requireRead();
        if (accessGuard.isGuardRole()) {
            guardId = AuthContext.userId();
        }
        return R.ok(logisticsService.attendance(page, size, guardId, date));
    }

    @PostMapping("/attendance/check")
    @RequirePermission("logistics:write")
    public R<BwcAttendance> check(@RequestBody Map<String, Object> body) {
        accessGuard.requireSelfService();
        Long guardId = accessGuard.isManager()
                ? (body.get("guardId") == null ? null : Long.valueOf(String.valueOf(body.get("guardId"))))
                : AuthContext.userId();
        return R.ok(logisticsService.checkIn(guardId,
                body.get("guardName") == null ? null : String.valueOf(body.get("guardName")),
                body.get("type") == null ? "ON" : String.valueOf(body.get("type")),
                body.get("method") == null ? "APP" : String.valueOf(body.get("method")),
                body.get("status") == null ? "NORMAL" : String.valueOf(body.get("status"))));
    }

    @GetMapping("/performance")
    @RequirePermission("logistics:read")
    public R<IPage<BwcPerformance>> performance(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size,
                                                 @RequestParam(required = false) String month) {
        accessGuard.requireRead();
        return R.ok(logisticsService.performance(page, size, month, accessGuard.isGuardRole() ? AuthContext.userId() : null));
    }

    @PostMapping("/performance")
    @RequirePermission("logistics:write")
    public R<BwcPerformance> savePerformance(@RequestBody BwcPerformance performance) {
        accessGuard.requireManager();
        return R.ok(logisticsService.savePerformance(performance));
    }

    @GetMapping("/feedback")
    @RequirePermission("logistics:read")
    public R<IPage<BwcFeedback>> feedback(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String type) {
        LambdaQueryWrapper<BwcFeedback> wrapper = new LambdaQueryWrapper<>();
        if (accessGuard.isSelfRole()) {
            wrapper.eq(BwcFeedback::getUserId, AuthContext.userId());
        } else {
            accessGuard.requireRead();
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcFeedback::getStatus, status);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(BwcFeedback::getType, type);
        }
        wrapper.orderByDesc(BwcFeedback::getId);
        return R.ok(feedbackMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/feedback")
    @RequirePermission("logistics:write")
    public R<BwcFeedback> submitFeedback(@RequestBody BwcFeedback feedback) {
        accessGuard.requireSelfService();
        return R.ok(logisticsService.submitFeedback(feedback));
    }

    @PostMapping("/feedback/{id}/handle")
    @RequirePermission("logistics:write")
    public R<BwcFeedback> handleFeedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        accessGuard.requireManager();
        return R.ok(logisticsService.handleFeedback(id, body.get("status"), body.get("result")));
    }

    @GetMapping("/monthly")
    @RequirePermission("logistics:read")
    public R<java.util.List<BwcMonthlyReport>> monthly() {
        accessGuard.requireRead();
        return R.ok(logisticsService.monthlyReports());
    }

    @PostMapping("/monthly/generate")
    @RequirePermission("logistics:write")
    public R<BwcMonthlyReport> generate(@RequestBody(required = false) Map<String, String> body) {
        accessGuard.requireManager();
        String month = body == null ? null : body.get("month");
        return R.ok(logisticsService.generateMonthly(month));
    }

    @GetMapping("/monthly/{month}/pdf")
    @RequirePermission("logistics:read")
    public void monthlyPdf(@PathVariable String month, HttpServletResponse response) throws IOException {
        accessGuard.requireRead();
        byte[] pdf = logisticsService.monthlyPdf(month);
        response.setContentType("application/pdf");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String fileName = URLEncoder.encode("校园保卫月报-" + month, StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName + ".pdf");
        response.getOutputStream().write(pdf);
    }

    @GetMapping("/stats")
    @RequirePermission("logistics:read")
    public R<Map<String, Object>> stats() {
        accessGuard.requireRead();
        return R.ok(logisticsService.stats());
    }
}
