package com.greathiit.bwc.module.vehicle.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateAuthRecord;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateCamera;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateAuthRecordMapper;
import com.greathiit.bwc.module.vehicle.service.PlateAuthService;
import com.greathiit.bwc.module.vehicle.service.VehicleService;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.RequirePermission;
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

import java.util.List;
import java.util.Map;

/**
 * 车牌识别接口配置（租户级）：配置、连接测试、相机、下发/删除/查询、对账、日志。
 */
@RestController
@RequestMapping("/api/plate")
@RequiredArgsConstructor
public class PlateAuthController {

    private final PlateAuthService plateAuthService;
    private final VehicleService vehicleService;
    private final BwcPlateAuthRecordMapper authRecordMapper;
    private final AuditService auditService;

    @GetMapping("/config")
    @RequirePermission("plate:read")
    public R<Map<String, Object>> config() {
        return R.ok(plateAuthService.maskedConfig());
    }

    @PutMapping("/config")
    @RequirePermission("plate:write")
    public R<Map<String, Object>> save(@RequestBody Map<String, Object> body) {
        String baseUrl = body.get("baseUrl") == null ? null : String.valueOf(body.get("baseUrl"));
        String apiKey = body.get("apiKey") == null ? null : String.valueOf(body.get("apiKey"));
        boolean tested = Boolean.parseBoolean(String.valueOf(body.getOrDefault("tested", false)));
        boolean mqtt = Boolean.parseBoolean(String.valueOf(body.getOrDefault("mqttConnected", false)));
        Map<String, Object> result = plateAuthService.saveConfig(baseUrl, apiKey, tested, mqtt);
        auditService.record("plate", "SAVE_CONFIG", baseUrl, "保存车牌识别接口配置");
        return R.ok(result);
    }

    @PostMapping("/test")
    @RequirePermission("plate:write")
    public R<Map<String, Object>> test(@RequestBody Map<String, Object> body) {
        String baseUrl = body.get("baseUrl") == null ? null : String.valueOf(body.get("baseUrl"));
        String apiKey = body.get("apiKey") == null ? null : String.valueOf(body.get("apiKey"));
        return R.ok(plateAuthService.test(baseUrl, apiKey));
    }

    @GetMapping("/cameras")
    @RequirePermission("plate:read")
    public R<List<BwcPlateCamera>> cameras() {
        return R.ok(plateAuthService.localCameras());
    }

    @PostMapping("/cameras")
    @RequirePermission("plate:write")
    public R<BwcPlateCamera> saveCamera(@RequestBody BwcPlateCamera camera) {
        return R.ok(plateAuthService.saveCamera(camera));
    }

    @PutMapping("/cameras/{id}")
    @RequirePermission("plate:write")
    public R<BwcPlateCamera> updateCamera(@PathVariable Long id, @RequestBody BwcPlateCamera camera) {
        camera.setId(id);
        return R.ok(plateAuthService.saveCamera(camera));
    }

    @DeleteMapping("/cameras/{id}")
    @RequirePermission("plate:write")
    public R<Void> deleteCamera(@PathVariable Long id) {
        plateAuthService.deleteCamera(id);
        return R.ok();
    }

    @PostMapping("/cameras/sync")
    @RequirePermission("plate:write")
    public R<List<BwcPlateCamera>> syncCameras() {
        return R.ok(plateAuthService.syncCameras());
    }

    @PostMapping("/auth/add-all")
    @RequirePermission("plate:write")
    public R<Map<String, Object>> addAll(@RequestBody Map<String, String> body) {
        return R.ok(plateAuthService.addAll(body.get("plateNo"), AuthContext.username(), "ADD_ALL"));
    }

    @PostMapping("/auth/add")
    @RequirePermission("plate:write")
    public R<Map<String, Object>> add(@RequestBody Map<String, String> body) {
        return R.ok(plateAuthService.add(body.get("plateNo"), body.get("cameraId"), AuthContext.username()));
    }

    @PostMapping("/auth/temp")
    @RequirePermission("plate:write")
    public R<Map<String, Object>> temp(@RequestBody Map<String, String> body) {
        return R.ok(plateAuthService.addTemp(body.get("plateNo"),
                parse(body.get("startTime")), parse(body.get("endTime")), AuthContext.username()));
    }

    @PostMapping("/auth/del")
    @RequirePermission("plate:write")
    public R<Map<String, Object>> delete(@RequestBody Map<String, String> body) {
        return R.ok(plateAuthService.delete(body.get("plateNo"), body.get("cameraId"), AuthContext.username()));
    }

    @PostMapping("/auth/del-all")
    @RequirePermission("plate:write")
    public R<Map<String, Object>> deleteAll(@RequestBody Map<String, String> body) {
        return R.ok(plateAuthService.deleteAll(body.get("plateNo"), AuthContext.username()));
    }

    @GetMapping("/auth/query")
    @RequirePermission("plate:read")
    public R<Map<?, ?>> query(@RequestParam String plateNo) {
        return R.ok(plateAuthService.query(plateNo));
    }

    @GetMapping("/auth/records")
    @RequirePermission("plate:read")
    public R<Map<?, ?>> remoteRecords() {
        return R.ok(plateAuthService.records());
    }

    @GetMapping("/records")
    @RequirePermission("plate:read")
    public R<IPage<BwcPlateAuthRecord>> records(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String plateNo,
                                                @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BwcPlateAuthRecord> wrapper = new LambdaQueryWrapper<>();
        if (plateNo != null && !plateNo.isBlank()) {
            wrapper.like(BwcPlateAuthRecord::getPlateNo, plateNo);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcPlateAuthRecord::getStatus, status);
        }
        wrapper.orderByDesc(BwcPlateAuthRecord::getId);
        return R.ok(authRecordMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/logs")
    @RequirePermission("plate:read")
    public R<Map<?, ?>> logs(@RequestParam(required = false) String plateNo,
                             @RequestParam(required = false) String cameraId,
                             @RequestParam(required = false) String inOut,
                             @RequestParam(required = false) String startTime,
                             @RequestParam(required = false) String endTime) {
        return R.ok(plateAuthService.logs(plateNo, cameraId, inOut, startTime, endTime));
    }

    @GetMapping("/logs/in-park")
    @RequirePermission("plate:read")
    public R<Map<?, ?>> inPark() {
        return R.ok(plateAuthService.inPark());
    }

    @PostMapping("/logs/sync")
    @RequirePermission("plate:write")
    public R<Integer> syncLogs() {
        return R.ok(vehicleService.syncLogs());
    }

    private java.time.LocalDateTime parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return java.time.LocalDateTime.parse(value.replace("T", " ").substring(0, 19),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
