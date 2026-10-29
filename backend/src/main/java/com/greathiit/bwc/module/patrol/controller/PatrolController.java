package com.greathiit.bwc.module.patrol.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.greathiit.bwc.common.QrCodeUtil;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.patrol.entity.BwcPatrolPoint;
import com.greathiit.bwc.module.patrol.entity.BwcPatrolRecord;
import com.greathiit.bwc.module.patrol.entity.BwcPatrolRoute;
import com.greathiit.bwc.module.patrol.mapper.BwcPatrolPointMapper;
import com.greathiit.bwc.module.patrol.mapper.BwcPatrolRouteMapper;
import com.greathiit.bwc.module.patrol.service.PatrolService;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 巡逻：路线/点位、扫码/GPS 打卡、异常上报、统计。
 */
@RestController
@RequestMapping("/api/patrol")
@RequiredArgsConstructor
public class PatrolController {

    private final PatrolService patrolService;
    private final BwcPatrolRouteMapper routeMapper;
    private final BwcPatrolPointMapper pointMapper;

    @GetMapping("/routes")
    @RequirePermission("patrol:read")
    public R<List<BwcPatrolRoute>> routes() {
        return R.ok(routeMapper.selectList(new LambdaQueryWrapper<BwcPatrolRoute>()
                .orderByAsc(BwcPatrolRoute::getId)));
    }

    @GetMapping("/points")
    @RequirePermission("patrol:read")
    public R<List<BwcPatrolPoint>> points(@RequestParam(required = false) Long routeId) {
        LambdaQueryWrapper<BwcPatrolPoint> wrapper = new LambdaQueryWrapper<>();
        if (routeId != null) {
            wrapper.eq(BwcPatrolPoint::getRouteId, routeId);
        }
        wrapper.orderByAsc(BwcPatrolPoint::getSort);
        return R.ok(pointMapper.selectList(wrapper));
    }

    @GetMapping(value = "/points/{id}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @RequirePermission("patrol:read")
    public byte[] pointQr(@PathVariable Long id) {
        BwcPatrolPoint point = pointMapper.selectById(id);
        return QrCodeUtil.png(point == null ? "unknown" : point.getQrCode(), 320);
    }

    @PostMapping("/checkin")
    @RequirePermission("patrol:write")
    public R<BwcPatrolRecord> checkin(@RequestBody Map<String, Object> body) {
        Long pointId = Long.valueOf(String.valueOf(body.get("pointId")));
        java.math.BigDecimal lon = body.get("longitude") == null ? null
                : new java.math.BigDecimal(String.valueOf(body.get("longitude")));
        java.math.BigDecimal lat = body.get("latitude") == null ? null
                : new java.math.BigDecimal(String.valueOf(body.get("latitude")));
        return R.ok(patrolService.checkin(pointId, (String) body.get("method"), lon, lat,
                (String) body.get("abnormalDesc"), (String) body.get("photos"),
                AuthContext.userId(), AuthContext.get() == null ? null : AuthContext.get().getRealName()));
    }

    @GetMapping("/records")
    @RequirePermission("patrol:read")
    public R<IPage<BwcPatrolRecord>> records(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(required = false) Long guardId,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(patrolService.page(page, size, status, guardId, date));
    }

    @GetMapping("/stats")
    @RequirePermission("patrol:read")
    public R<Map<String, Object>> stats(@RequestParam(required = false)
                                        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return R.ok(patrolService.stats(date));
    }
}
