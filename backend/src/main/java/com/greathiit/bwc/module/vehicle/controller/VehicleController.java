package com.greathiit.bwc.module.vehicle.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateLog;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicle;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicleAppointment;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicleLog;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateLogMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleAppointmentMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleLogMapper;
import com.greathiit.bwc.module.vehicle.service.VehicleService;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 车辆管理：备案、预约审批、车牌下发/注销、进出记录、在场车辆、统计。
 */
@RestController
@RequestMapping("/api/vehicle")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final BwcVehicleAppointmentMapper appointmentMapper;
    private final BwcVehicleLogMapper vehicleLogMapper;
    private final BwcPlateLogMapper plateLogMapper;

    @GetMapping("/vehicles")
    @RequirePermission("vehicle:read")
    public R<IPage<BwcVehicle>> vehicles(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String authStatus) {
        return R.ok(vehicleService.pageVehicle(page, size, keyword, authStatus));
    }

    @PostMapping("/register")
    public R<BwcVehicle> register(@RequestBody Map<String, String> body) {
        return R.ok(vehicleService.registerVehicle(body));
    }

    @GetMapping("/my")
    public R<List<BwcVehicle>> myVehicles() {
        return R.ok(vehicleService.myVehicles());
    }

    @PostMapping("/vehicles/{id}/approve")
    @RequirePermission("vehicle:write")
    public R<BwcVehicle> approveRegistration(@PathVariable Long id,
                                             @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> data = body == null ? Map.of() : body;
        boolean approved = Boolean.parseBoolean(String.valueOf(data.getOrDefault("approved", true)));
        String remark = data.get("remark") == null ? null : String.valueOf(data.get("remark"));
        return R.ok(vehicleService.approveRegistration(id, approved, remark));
    }

    @PostMapping("/vehicles/{id}/issue")
    @RequirePermission("vehicle:write")
    public R<BwcVehicle> issue(@PathVariable Long id) {
        return R.ok(vehicleService.issueFixed(id));
    }

    @PostMapping("/vehicles/{id}/remove")
    @RequirePermission("vehicle:write")
    public R<BwcVehicle> remove(@PathVariable Long id) {
        return R.ok(vehicleService.removeFixed(id));
    }

    @GetMapping("/appointments")
    @RequirePermission("vehicle:read")
    public R<IPage<BwcVehicleAppointment>> appointments(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size,
                                                        @RequestParam(required = false) String status,
                                                        @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BwcVehicleAppointment> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcVehicleAppointment::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcVehicleAppointment::getPlateNo, keyword)
                    .or().like(BwcVehicleAppointment::getVisitorName, keyword));
        }
        wrapper.orderByDesc(BwcVehicleAppointment::getId);
        return R.ok(appointmentMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/appointments")
    @RequirePermission("vehicle:write")
    public R<BwcVehicleAppointment> createAppointment(@RequestBody BwcVehicleAppointment body) {
        body.setStatus("PENDING");
        body.setAuthStatus("NONE");
        appointmentMapper.insert(body);
        return R.ok(body);
    }

    @PostMapping("/appointments/{id}/approve")
    @RequirePermission("vehicle:write")
    public R<BwcVehicleAppointment> approve(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", false)));
        return R.ok(vehicleService.approveAppointment(id, approved,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @PostMapping("/appointments/{id}/retry")
    @RequirePermission("vehicle:write")
    public R<BwcVehicleAppointment> retry(@PathVariable Long id) {
        return R.ok(vehicleService.retryAppointment(id));
    }

    @PostMapping("/violations/report")
    public R<com.greathiit.bwc.module.vehicle.entity.BwcViolation> reportViolation(@RequestBody Map<String, String> body) {
        return R.ok(vehicleService.reportViolation(body));
    }

    @GetMapping("/violations/my")
    public R<List<com.greathiit.bwc.module.vehicle.entity.BwcViolation>> myViolations() {
        return R.ok(vehicleService.myViolations());
    }

    @PostMapping("/violations/{id}/handle")
    @RequirePermission("vehicle:write")
    public R<com.greathiit.bwc.module.vehicle.entity.BwcViolation> handleViolation(@PathVariable Long id,
                                                                                   @RequestBody Map<String, String> body) {
        return R.ok(vehicleService.handleViolation(id, body.get("result")));
    }

    @GetMapping("/logs")
    @RequirePermission("vehicle:read")
    public R<IPage<BwcVehicleLog>> logs(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String plateNo,
                                        @RequestParam(required = false) String inOut) {
        LambdaQueryWrapper<BwcVehicleLog> wrapper = new LambdaQueryWrapper<>();
        if (plateNo != null && !plateNo.isBlank()) {
            wrapper.like(BwcVehicleLog::getPlateNo, plateNo);
        }
        if (inOut != null && !inOut.isBlank()) {
            wrapper.eq(BwcVehicleLog::getInOut, inOut);
        }
        wrapper.orderByDesc(BwcVehicleLog::getTriggerTime);
        return R.ok(vehicleLogMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/in-park")
    @RequirePermission("vehicle:read")
    public R<List<BwcPlateLog>> inPark() {
        List<BwcPlateLog> list = plateLogMapper.selectList(new LambdaQueryWrapper<BwcPlateLog>()
                .orderByDesc(BwcPlateLog::getTriggerTime).last("limit 500"));
        Map<String, BwcPlateLog> latest = new java.util.LinkedHashMap<>();
        for (BwcPlateLog item : list) {
            latest.putIfAbsent(item.getPlateNo(), item);
        }
        return R.ok(latest.values().stream().filter(i -> "in".equalsIgnoreCase(i.getInOut())).toList());
    }

    @PostMapping("/sync-logs")
    @RequirePermission("vehicle:write")
    public R<Integer> syncLogs() {
        return R.ok(vehicleService.syncLogs());
    }

    @GetMapping("/stats")
    @RequirePermission("vehicle:read")
    public R<Map<String, Object>> stats() {
        long total = vehicleLogMapper.selectCount(null);
        long inCount = vehicleLogMapper.selectCount(
                new LambdaQueryWrapper<BwcVehicleLog>().eq(BwcVehicleLog::getInOut, "in"));
        long outCount = vehicleLogMapper.selectCount(
                new LambdaQueryWrapper<BwcVehicleLog>().eq(BwcVehicleLog::getInOut, "out"));
        long whitelist = vehicleLogMapper.selectCount(
                new LambdaQueryWrapper<BwcVehicleLog>().eq(BwcVehicleLog::getIsWhitelist, 1));
        return R.ok(Map.of("total", total, "in", inCount, "out", outCount, "whitelist", whitelist));
    }
}
