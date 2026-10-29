package com.greathiit.bwc.module.fire.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.QrCodeUtil;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.fire.entity.BwcFireFacility;
import com.greathiit.bwc.module.fire.entity.BwcFireInspection;
import com.greathiit.bwc.module.fire.entity.BwcHazard;
import com.greathiit.bwc.module.fire.mapper.BwcFireFacilityMapper;
import com.greathiit.bwc.module.fire.mapper.BwcFireInspectionMapper;
import com.greathiit.bwc.module.fire.service.FireService;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消防：设施台账/一物一码、扫码巡检、隐患整改闭环、演练培训、到期预警。
 */
@RestController
@RequestMapping("/api/fire")
@RequiredArgsConstructor
public class FireController {

    private final FireService fireService;
    private final BwcFireFacilityMapper facilityMapper;
    private final BwcFireInspectionMapper inspectionMapper;

    @GetMapping("/facilities")
    @RequirePermission("fire:read")
    public R<IPage<BwcFireFacility>> facilities(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "10") long size,
                                                @RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BwcFireFacility> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcFireFacility::getFacilityNo, keyword)
                    .or().like(BwcFireFacility::getLocation, keyword)
                    .or().like(BwcFireFacility::getQrCode, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcFireFacility::getStatus, status);
        }
        wrapper.orderByDesc(BwcFireFacility::getId);
        return R.ok(facilityMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/facilities")
    @RequirePermission("fire:write")
    public R<BwcFireFacility> createFacility(@RequestBody BwcFireFacility facility) {
        facility.setId(null);
        return R.ok(fireService.saveFacility(facility));
    }

    @PutMapping("/facilities/{id}")
    @RequirePermission("fire:write")
    public R<BwcFireFacility> updateFacility(@PathVariable Long id, @RequestBody BwcFireFacility facility) {
        facility.setId(id);
        return R.ok(fireService.saveFacility(facility));
    }

    @GetMapping(value = "/facilities/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @RequirePermission("fire:read")
    public byte[] facilityQr(@PathVariable Long id) {
        BwcFireFacility facility = facilityMapper.selectById(id);
        return QrCodeUtil.png(facility == null ? "unknown" : facility.getQrCode(), 320);
    }

    @GetMapping("/inspections")
    @RequirePermission("fire:read")
    public R<IPage<BwcFireInspection>> inspections(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) String facilityNo) {
        LambdaQueryWrapper<BwcFireInspection> wrapper = new LambdaQueryWrapper<>();
        if (facilityNo != null && !facilityNo.isBlank()) {
            wrapper.eq(BwcFireInspection::getFacilityNo, facilityNo);
        }
        wrapper.orderByDesc(BwcFireInspection::getId);
        return R.ok(inspectionMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/inspect")
    @RequirePermission("fire:write")
    public R<Map<String, Object>> inspect(@RequestBody Map<String, Object> body) {
        Long facilityId = body.get("facilityId") == null ? null
                : Long.valueOf(String.valueOf(body.get("facilityId")));
        return R.ok(fireService.inspect((String) body.get("qrCode"), facilityId,
                (String) body.get("result"), (String) body.get("photos"),
                (String) body.get("remark"), AuthContext.username()));
    }

    @GetMapping("/hazards")
    @RequirePermission("hazard:read")
    public R<IPage<BwcHazard>> hazards(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String level,
                                        @RequestParam(required = false) String keyword) {
        return R.ok(fireService.hazardPage(page, size, status, level, keyword));
    }

    @PostMapping("/hazards")
    @RequirePermission("hazard:write")
    public R<BwcHazard> reportHazard(@RequestBody BwcHazard hazard) {
        return R.ok(fireService.reportHazard(hazard));
    }

    @PostMapping("/hazards/{id}/assign")
    @RequirePermission("hazard:write")
    public R<BwcHazard> assignHazard(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long assignTo = body.get("assignTo") == null ? null
                : Long.valueOf(String.valueOf(body.get("assignTo")));
        String name = body.get("assignToName") == null ? null : String.valueOf(body.get("assignToName"));
        Integer days = body.get("days") == null ? null : Integer.valueOf(String.valueOf(body.get("days")));
        return R.ok(fireService.assignHazard(id, assignTo, name, days));
    }

    @PostMapping("/hazards/{id}/rectify")
    @RequirePermission("hazard:write")
    public R<BwcHazard> rectify(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(fireService.rectify(id, body.get("plan"), body.get("photos")));
    }

    @PostMapping("/hazards/{id}/accept")
    @RequirePermission("hazard:write")
    public R<BwcHazard> accept(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", true)));
        return R.ok(fireService.accept(id, approved,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @GetMapping("/stats")
    @RequirePermission("fire:read")
    public R<Map<String, Object>> stats() {
        return R.ok(fireService.stats());
    }

    @GetMapping("/expiring")
    @RequirePermission("fire:read")
    public R<List<BwcFireFacility>> expiring(@RequestParam(defaultValue = "30") int days) {
        return R.ok(fireService.expiring(days));
    }
}
