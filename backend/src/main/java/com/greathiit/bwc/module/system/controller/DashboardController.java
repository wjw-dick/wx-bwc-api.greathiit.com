package com.greathiit.bwc.module.system.controller;

import com.greathiit.bwc.common.ExcelUtil;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.system.service.StatsService;
import com.greathiit.bwc.security.PermissionService;
import com.greathiit.bwc.security.RequirePermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 看板与报表：租户看板、平台全部学校看板、按模块统计与 Excel 导出。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final StatsService statsService;
    private final PermissionService permissionService;

    @GetMapping("/dashboard")
    @RequirePermission("dashboard:read")
    public R<Map<String, Object>> dashboard() {
        return R.ok(statsService.tenantOverview());
    }

    @GetMapping("/dashboard/platform")
    @RequirePermission("platform:read")
    public R<Map<String, Object>> platformDashboard() {
        permissionService.requirePlatform();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("schools", statsService.crossSchool());
        result.put("generatedAt", java.time.LocalDateTime.now());
        return R.ok(result);
    }

    @GetMapping("/reports/tenant")
    @RequirePermission("report:read")
    public R<Map<String, Object>> tenantReports() {
        return R.ok(statsService.tenantOverview());
    }

    @GetMapping("/reports/{module}")
    @RequirePermission("report:read")
    public R<Map<String, Object>> module(@PathVariable String module,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                         @RequestParam(required = false)
                                         @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return R.ok(statsService.moduleStats(module, start, end));
    }

    @GetMapping("/reports/export")
    @RequirePermission("report:read")
    public void export(@RequestParam(defaultValue = "overview") String module,
                       @RequestParam(required = false)
                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                       @RequestParam(required = false)
                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
                       HttpServletResponse response) throws IOException {
        Map<String, Object> data = "overview".equals(module)
                ? statsService.tenantOverview()
                : statsService.moduleStats(module, start, end);
        List<Map<String, ?>> rows = new ArrayList<>();
        data.forEach((key, value) -> {
            if (value instanceof Number || value instanceof String || value instanceof Boolean) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("key", key);
                row.put("value", value);
                rows.add(row);
            }
        });
        ExcelUtil.write(response, "统计报表-" + module, List.of("统计项", "数值"),
                List.of("key", "value"), rows);
    }
}
