package com.greathiit.bwc.module.patrol.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.module.patrol.entity.BwcPatrolPoint;
import com.greathiit.bwc.module.patrol.entity.BwcPatrolRecord;
import com.greathiit.bwc.module.patrol.mapper.BwcPatrolPointMapper;
import com.greathiit.bwc.module.patrol.mapper.BwcPatrolRecordMapper;
import com.greathiit.bwc.module.system.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 巡逻：二维码/GPS 打卡、时间窗判定、异常上报、完成率统计。
 */
@Service
@RequiredArgsConstructor
public class PatrolService {

    private final BwcPatrolPointMapper pointMapper;
    private final BwcPatrolRecordMapper recordMapper;
    private final AuditService auditService;

    @Transactional
    public BwcPatrolRecord checkin(Long pointId, String method, java.math.BigDecimal lon,
                                   java.math.BigDecimal lat, String abnormalDesc, String photos,
                                   Long guardId, String guardName) {
        BwcPatrolPoint point = pointMapper.selectById(pointId);
        if (point == null) {
            throw BizException.notFound("巡逻点位不存在");
        }
        BwcPatrolRecord record = new BwcPatrolRecord();
        record.setRouteId(point.getRouteId());
        record.setPointId(pointId);
        record.setGuardId(guardId);
        record.setGuardName(guardName);
        record.setCheckTime(LocalDateTime.now());
        record.setMethod(method == null ? "QR" : method);
        record.setLongitude(lon);
        record.setLatitude(lat);
        record.setAbnormalDesc(abnormalDesc);
        record.setPhotos(photos);
        if (abnormalDesc != null && !abnormalDesc.isBlank()) {
            record.setStatus("ABNORMAL");
        } else if (!inWindow(point)) {
            record.setStatus("LATE");
        } else {
            record.setStatus("NORMAL");
        }
        recordMapper.insert(record);
        auditService.record("patrol", "CHECKIN", point.getName(), "巡逻打卡：" + record.getStatus());
        return record;
    }

    private boolean inWindow(BwcPatrolPoint point) {
        if (point.getTimeWindowStart() == null || point.getTimeWindowEnd() == null) {
            return true;
        }
        LocalTime now = LocalTime.now();
        try {
            LocalTime start = LocalTime.parse(point.getTimeWindowStart());
            LocalTime end = LocalTime.parse(point.getTimeWindowEnd());
            if (end.isBefore(start)) {
                return now.isAfter(start) || now.isBefore(end);
            }
            return !now.isBefore(start) && !now.isAfter(end);
        } catch (Exception e) {
            return true;
        }
    }

    public IPage<BwcPatrolRecord> page(long page, long size, String status, Long guardId,
                                       LocalDate date) {
        LambdaQueryWrapper<BwcPatrolRecord> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcPatrolRecord::getStatus, status);
        }
        if (guardId != null) {
            wrapper.eq(BwcPatrolRecord::getGuardId, guardId);
        }
        if (date != null) {
            wrapper.ge(BwcPatrolRecord::getCheckTime, date.atStartOfDay())
                    .le(BwcPatrolRecord::getCheckTime, date.atTime(23, 59, 59));
        }
        wrapper.orderByDesc(BwcPatrolRecord::getId);
        return recordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> stats(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        List<BwcPatrolRecord> records = recordMapper.selectList(new LambdaQueryWrapper<BwcPatrolRecord>()
                .ge(BwcPatrolRecord::getCheckTime, day.atStartOfDay())
                .le(BwcPatrolRecord::getCheckTime, day.atTime(23, 59, 59)));
        long normal = records.stream().filter(r -> "NORMAL".equals(r.getStatus())).count();
        long abnormal = records.stream().filter(r -> "ABNORMAL".equals(r.getStatus())).count();
        long late = records.stream().filter(r -> "LATE".equals(r.getStatus())).count();
        long total = records.size();
        Map<String, Object> result = new HashMap<>();
        result.put("date", day);
        result.put("total", total);
        result.put("normal", normal);
        result.put("abnormal", abnormal);
        result.put("late", late);
        result.put("onTimeRate", total == 0 ? 100 : Math.round(normal * 1000.0 / total) / 10.0);
        long pointCount = pointMapper.selectCount(null);
        long covered = records.stream().map(BwcPatrolRecord::getPointId).distinct().count();
        result.put("pointTotal", pointCount);
        result.put("pointCovered", covered);
        result.put("completeRate", pointCount == 0 ? 100
                : Math.round(Math.min(covered, pointCount) * 1000.0 / pointCount) / 10.0);
        return result;
    }
}
