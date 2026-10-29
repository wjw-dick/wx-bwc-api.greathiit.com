package com.greathiit.bwc.module.device.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.device.entity.BwcDeviceFollow;
import com.greathiit.bwc.module.device.entity.BwcDeviceRepair;
import com.greathiit.bwc.module.device.service.DeviceService;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 设备运维：报修、分派、维修跟进、完成。
 */
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping("/repairs")
    @RequirePermission("device:read")
    public R<IPage<BwcDeviceRepair>> repairs(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String status) {
        return R.ok(deviceService.page(page, size, status));
    }

    @PostMapping("/repairs")
    @RequirePermission("device:write")
    public R<BwcDeviceRepair> report(@RequestBody BwcDeviceRepair repair) {
        return R.ok(deviceService.report(repair));
    }

    @PostMapping("/repairs/{id}/assign")
    @RequirePermission("device:write")
    public R<BwcDeviceRepair> assign(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(deviceService.assign(id, body.get("assignTo")));
    }

    @PostMapping("/repairs/{id}/follow")
    @RequirePermission("device:write")
    public R<BwcDeviceFollow> follow(@PathVariable Long id, @RequestBody BwcDeviceFollow follow) {
        return R.ok(deviceService.follow(id, follow));
    }

    @PostMapping("/repairs/{id}/finish")
    @RequirePermission("device:write")
    public R<BwcDeviceRepair> finish(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(deviceService.finish(id, body.get("result")));
    }

    @GetMapping("/stats")
    @RequirePermission("device:read")
    public R<Map<String, Object>> stats() {
        return R.ok(deviceService.stats());
    }
}
