package com.greathiit.bwc.module.report.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.report.entity.BwcMonitorAccess;
import com.greathiit.bwc.module.report.entity.BwcReportApply;
import com.greathiit.bwc.module.report.service.ReportService;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报备审批与调取监控分级审批。
 */
@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/applies")
    @RequirePermission("report:read")
    public R<IPage<BwcReportApply>> applies(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String status) {
        return R.ok(reportService.applyPage(page, size, type, status));
    }

    @PostMapping("/applies")
    @RequirePermission("report:write")
    public R<BwcReportApply> submit(@RequestBody BwcReportApply apply) {
        return R.ok(reportService.submit(apply));
    }

    @PostMapping("/applies/{id}/approve")
    @RequirePermission("report:write")
    public R<BwcReportApply> approve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", false)));
        return R.ok(reportService.approve(id, approved,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @GetMapping("/monitor-access")
    @RequirePermission("report:read")
    public R<IPage<BwcMonitorAccess>> monitorAccess(@RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size,
                                                     @RequestParam(required = false) String status) {
        return R.ok(reportService.monitorPage(page, size, status));
    }

    @PostMapping("/monitor-access")
    @RequirePermission("report:write")
    public R<BwcMonitorAccess> submitMonitor(@RequestBody BwcMonitorAccess access) {
        return R.ok(reportService.submitMonitor(access));
    }

    @PostMapping("/monitor-access/{id}/approve")
    @RequirePermission("report:write")
    public R<BwcMonitorAccess> approveMonitor(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", false)));
        String stage = body.get("stage") == null ? "DEPT" : String.valueOf(body.get("stage"));
        return R.ok(reportService.approveMonitor(id, stage, approved,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @PostMapping("/monitor-access/{id}/access")
    @RequirePermission("report:write")
    public R<BwcMonitorAccess> recordAccess(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(reportService.recordAccess(id, body.get("accessRecord"), body.get("result")));
    }

    @GetMapping("/stats")
    @RequirePermission("report:read")
    public R<Map<String, Object>> stats() {
        return R.ok(reportService.stats());
    }
}
