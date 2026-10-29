package com.greathiit.bwc.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.greathiit.bwc.module.device.entity.BwcDeviceRepair;
import com.greathiit.bwc.module.device.mapper.BwcDeviceMapper;
import com.greathiit.bwc.module.device.mapper.BwcDeviceRepairMapper;
import com.greathiit.bwc.module.emergency.entity.BwcEmergencyEvent;
import com.greathiit.bwc.module.emergency.mapper.BwcEmergencyEventMapper;
import com.greathiit.bwc.module.event.entity.BwcSecurityEvent;
import com.greathiit.bwc.module.event.entity.BwcWorkOrder;
import com.greathiit.bwc.module.event.mapper.BwcSecurityEventMapper;
import com.greathiit.bwc.module.event.mapper.BwcWorkOrderMapper;
import com.greathiit.bwc.module.fire.entity.BwcFireAlarm;
import com.greathiit.bwc.module.fire.entity.BwcFireFacility;
import com.greathiit.bwc.module.fire.entity.BwcHazard;
import com.greathiit.bwc.module.fire.mapper.BwcFireAlarmMapper;
import com.greathiit.bwc.module.fire.mapper.BwcFireFacilityMapper;
import com.greathiit.bwc.module.fire.mapper.BwcHazardMapper;
import com.greathiit.bwc.module.logistics.entity.BwcAttendance;
import com.greathiit.bwc.module.logistics.entity.BwcFeedback;
import com.greathiit.bwc.module.logistics.mapper.BwcAttendanceMapper;
import com.greathiit.bwc.module.logistics.mapper.BwcFeedbackMapper;
import com.greathiit.bwc.module.lost.entity.BwcLostClaim;
import com.greathiit.bwc.module.lost.entity.BwcLostItem;
import com.greathiit.bwc.module.lost.mapper.BwcLostClaimMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostItemMapper;
import com.greathiit.bwc.module.patrol.entity.BwcPatrolRecord;
import com.greathiit.bwc.module.patrol.entity.BwcPatrolTask;
import com.greathiit.bwc.module.patrol.mapper.BwcPatrolRecordMapper;
import com.greathiit.bwc.module.patrol.mapper.BwcPatrolTaskMapper;
import com.greathiit.bwc.module.report.entity.BwcReportApply;
import com.greathiit.bwc.module.report.mapper.BwcReportApplyMapper;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.entity.SysTenantUser;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.mapper.SysTenantUserMapper;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateLog;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicle;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicleLog;
import com.greathiit.bwc.module.vehicle.entity.BwcViolation;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateLogMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleLogMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcViolationMapper;
import com.greathiit.bwc.module.visitor.entity.BwcVisitorAppointment;
import com.greathiit.bwc.module.visitor.entity.BwcVisitRecord;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitRecordMapper;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitorAppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报表统计：租户数据看板、全部学校看板、月报数据源。
 * 平台跨租户统计显式按 tenant_id 过滤，避免下钻/跨租户串数据。
 */
@Service
@RequiredArgsConstructor
public class StatsService {

    private final SysTenantMapper tenantMapper;
    private final SysTenantUserMapper tenantUserMapper;
    private final BwcVisitorAppointmentMapper visitorAppointmentMapper;
    private final BwcVisitRecordMapper visitRecordMapper;
    private final BwcVehicleMapper vehicleMapper;
    private final BwcVehicleLogMapper vehicleLogMapper;
    private final BwcPlateLogMapper plateLogMapper;
    private final BwcViolationMapper violationMapper;
    private final BwcSecurityEventMapper securityEventMapper;
    private final BwcWorkOrderMapper workOrderMapper;
    private final BwcHazardMapper hazardMapper;
    private final BwcFireFacilityMapper fireFacilityMapper;
    private final BwcFireAlarmMapper fireAlarmMapper;
    private final BwcPatrolTaskMapper patrolTaskMapper;
    private final BwcPatrolRecordMapper patrolRecordMapper;
    private final BwcLostItemMapper lostItemMapper;
    private final BwcLostClaimMapper lostClaimMapper;
    private final BwcReportApplyMapper reportApplyMapper;
    private final BwcDeviceMapper deviceMapper;
    private final BwcDeviceRepairMapper deviceRepairMapper;
    private final BwcEmergencyEventMapper emergencyEventMapper;
    private final BwcAttendanceMapper attendanceMapper;
    private final BwcFeedbackMapper feedbackMapper;

    public Map<String, Object> tenantOverview() {
        Long tenantId = com.greathiit.bwc.security.TenantContext.getTenantId();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("tenantId", tenantId);
        data.put("visitorTotal", visitorAppointmentMapper.selectCount(null));
        data.put("visitorToday", visitRecordMapper.selectCount(new LambdaQueryWrapper<BwcVisitRecord>()
                .ge(BwcVisitRecord::getInTime, LocalDate.now().atStartOfDay())));
        data.put("visitorPending", visitorAppointmentMapper.selectCount(
                new LambdaQueryWrapper<BwcVisitorAppointment>().eq(BwcVisitorAppointment::getStatus, "PENDING")));
        data.put("vehicleTotal", vehicleMapper.selectCount(null));
        data.put("vehicleLogToday", vehicleLogMapper.selectCount(new LambdaQueryWrapper<BwcVehicleLog>()
                .ge(BwcVehicleLog::getTriggerTime, LocalDate.now().atStartOfDay())));
        data.put("plateLogToday", plateLogMapper.selectCount(new LambdaQueryWrapper<BwcPlateLog>()
                .ge(BwcPlateLog::getTriggerTime, LocalDate.now().atStartOfDay())));
        data.put("inPark", plateLogMapper.selectCount(new LambdaQueryWrapper<BwcPlateLog>()
                .eq(BwcPlateLog::getInOut, "IN")));
        data.put("violationOpen", violationMapper.selectCount(new LambdaQueryWrapper<BwcViolation>()
                .ne(BwcViolation::getHandleStatus, "CLOSED")));
        data.put("eventTotal", securityEventMapper.selectCount(null));
        data.put("eventOpen", securityEventMapper.selectCount(new LambdaQueryWrapper<BwcSecurityEvent>()
                .ne(BwcSecurityEvent::getStatus, "CLOSED")));
        data.put("workOrderOpen", workOrderMapper.selectCount(new LambdaQueryWrapper<BwcWorkOrder>()
                .ne(BwcWorkOrder::getStatus, "DONE")));
        data.put("hazardTotal", hazardMapper.selectCount(null));
        data.put("hazardOpen", hazardMapper.selectCount(new LambdaQueryWrapper<BwcHazard>()
                .notIn(BwcHazard::getStatus, List.of("RECTIFIED", "ACCEPTED", "CLOSED"))));
        data.put("hazardRate", rate(count(hazardMapper, new LambdaQueryWrapper<BwcHazard>()
                        .in(BwcHazard::getStatus, List.of("RECTIFIED", "ACCEPTED", "CLOSED"))),
                hazardMapper.selectCount(null)));
        data.put("fireFacilityTotal", fireFacilityMapper.selectCount(null));
        data.put("fireFacilityIntact", fireFacilityMapper.selectCount(new LambdaQueryWrapper<BwcFireFacility>()
                .eq(BwcFireFacility::getStatus, "NORMAL")));
        data.put("fireIntactRate", rate(fireFacilityMapper.selectCount(new LambdaQueryWrapper<BwcFireFacility>()
                .eq(BwcFireFacility::getStatus, "NORMAL")), fireFacilityMapper.selectCount(null)));
        data.put("fireAlarmToday", fireAlarmMapper.selectCount(new LambdaQueryWrapper<BwcFireAlarm>()
                .ge(BwcFireAlarm::getAlarmTime, LocalDate.now().atStartOfDay())));
        data.put("patrolTaskTotal", patrolTaskMapper.selectCount(null));
        data.put("patrolTaskDone", patrolTaskMapper.selectCount(new LambdaQueryWrapper<BwcPatrolTask>()
                .eq(BwcPatrolTask::getStatus, "DONE")));
        data.put("patrolRate", rate(count(patrolTaskMapper, new LambdaQueryWrapper<BwcPatrolTask>()
                        .eq(BwcPatrolTask::getStatus, "DONE")), patrolTaskMapper.selectCount(null)));
        data.put("patrolRecordToday", patrolRecordMapper.selectCount(new LambdaQueryWrapper<BwcPatrolRecord>()
                .ge(BwcPatrolRecord::getCheckTime, LocalDate.now().atStartOfDay())));
        data.put("lostTotal", lostItemMapper.selectCount(null));
        data.put("lostOpen", lostItemMapper.selectCount(new LambdaQueryWrapper<BwcLostItem>()
                .in(BwcLostItem::getStatus, List.of("FOUND", "PUBLISHED"))));
        data.put("lostClaimed", lostItemMapper.selectCount(new LambdaQueryWrapper<BwcLostItem>()
                .eq(BwcLostItem::getStatus, "CLAIMED")));
        data.put("lostClaimRate", rate(count(lostItemMapper, new LambdaQueryWrapper<BwcLostItem>()
                        .eq(BwcLostItem::getStatus, "CLAIMED")), lostItemMapper.selectCount(null)));
        data.put("claimTotal", lostClaimMapper.selectCount(null));
        data.put("reportPending", reportApplyMapper.selectCount(new LambdaQueryWrapper<BwcReportApply>()
                .eq(BwcReportApply::getStatus, "PENDING")));
        data.put("deviceTotal", deviceMapper.selectCount(null));
        data.put("deviceRepairOpen", deviceRepairMapper.selectCount(new LambdaQueryWrapper<BwcDeviceRepair>()
                .ne(BwcDeviceRepair::getStatus, "DONE")));
        data.put("emergencyOpen", emergencyEventMapper.selectCount(new LambdaQueryWrapper<BwcEmergencyEvent>()
                .ne(BwcEmergencyEvent::getStatus, "CLOSED")));
        data.put("attendanceToday", attendanceMapper.selectCount(new LambdaQueryWrapper<BwcAttendance>()
                .ge(BwcAttendance::getCheckTime, LocalDate.now().atStartOfDay())));
        data.put("feedbackPending", feedbackMapper.selectCount(new LambdaQueryWrapper<BwcFeedback>()
                .eq(BwcFeedback::getStatus, "PENDING")));
        data.put("generatedAt", LocalDateTime.now());
        return data;
    }

    /**
     * 平台全部学校看板：逐租户统计，支持下钻。
     */
    public List<Map<String, Object>> crossSchool() {
        List<SysTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .orderByAsc(SysTenant::getId));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysTenant tenant : tenants) {
            Long id = tenant.getId();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("tenantId", id);
            row.put("tenantName", tenant.getName());
            row.put("tenantCode", tenant.getCode());
            row.put("status", tenant.getStatus());
            row.put("expireTime", tenant.getExpireTime());
            row.put("userCount", tenantUserMapper.selectCount(
                    new QueryWrapper<SysTenantUser>().eq("tenant_id", id)));
            row.put("visitorCount", countOf("bwc_visitor_appointment", id));
            row.put("visitToday", visitRecordMapper.selectCount(new LambdaQueryWrapper<BwcVisitRecord>()
                    .eq(BwcVisitRecord::getTenantId, id)
                    .ge(BwcVisitRecord::getInTime, LocalDate.now().atStartOfDay())));
            row.put("vehicleCount", vehicleMapper.selectCount(new LambdaQueryWrapper<BwcVehicle>()
                    .eq(BwcVehicle::getTenantId, id)));
            row.put("plateLogCount", plateLogMapper.selectCount(new LambdaQueryWrapper<BwcPlateLog>()
                    .eq(BwcPlateLog::getTenantId, id)));
            long eventCount = securityEventMapper.selectCount(new LambdaQueryWrapper<BwcSecurityEvent>()
                    .eq(BwcSecurityEvent::getTenantId, id));
            row.put("eventCount", eventCount);
            long hazardTotal = hazardMapper.selectCount(new LambdaQueryWrapper<BwcHazard>()
                    .eq(BwcHazard::getTenantId, id));
            long hazardDone = hazardMapper.selectCount(new LambdaQueryWrapper<BwcHazard>()
                    .eq(BwcHazard::getTenantId, id)
                    .in(BwcHazard::getStatus, List.of("RECTIFIED", "ACCEPTED", "CLOSED")));
            row.put("hazardCount", hazardTotal);
            row.put("hazardRate", rate(hazardDone, hazardTotal));
            long patrolTotal = patrolTaskMapper.selectCount(new LambdaQueryWrapper<BwcPatrolTask>()
                    .eq(BwcPatrolTask::getTenantId, id));
            long patrolDone = patrolTaskMapper.selectCount(new LambdaQueryWrapper<BwcPatrolTask>()
                    .eq(BwcPatrolTask::getTenantId, id)
                    .eq(BwcPatrolTask::getStatus, "DONE"));
            row.put("patrolRate", rate(patrolDone, patrolTotal));
            long fireTotal = fireFacilityMapper.selectCount(new LambdaQueryWrapper<BwcFireFacility>()
                    .eq(BwcFireFacility::getTenantId, id));
            long fireOk = fireFacilityMapper.selectCount(new LambdaQueryWrapper<BwcFireFacility>()
                    .eq(BwcFireFacility::getTenantId, id)
                    .eq(BwcFireFacility::getStatus, "NORMAL"));
            row.put("fireIntactRate", rate(fireOk, fireTotal));
            long lostTotal = lostItemMapper.selectCount(new LambdaQueryWrapper<BwcLostItem>()
                    .eq(BwcLostItem::getTenantId, id));
            long lostClaimed = lostItemMapper.selectCount(new LambdaQueryWrapper<BwcLostItem>()
                    .eq(BwcLostItem::getTenantId, id)
                    .eq(BwcLostItem::getStatus, "CLAIMED"));
            row.put("lostClaimRate", rate(lostClaimed, lostTotal));
            row.put("alarmCount", fireAlarmMapper.selectCount(new LambdaQueryWrapper<BwcFireAlarm>()
                    .eq(BwcFireAlarm::getTenantId, id)));
            rows.add(row);
        }
        return rows;
    }

    /**
     * 报表数据：按模块汇总 + 最近趋势。
     */
    public Map<String, Object> moduleStats(String module, LocalDate start, LocalDate end) {
        LocalDateTime from = (start == null ? LocalDate.now().minusDays(29) : start).atStartOfDay();
        LocalDateTime to = (end == null ? LocalDate.now() : end).atTime(23, 59, 59);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("module", module);
        result.put("start", from);
        result.put("end", to);
        switch (module == null ? "" : module) {
            case "visitor" -> {
                result.put("total", visitorAppointmentMapper.selectCount(
                        new LambdaQueryWrapper<BwcVisitorAppointment>()
                                .between(BwcVisitorAppointment::getCreateTime, from, to)));
                result.put("approved", visitorAppointmentMapper.selectCount(
                        new LambdaQueryWrapper<BwcVisitorAppointment>()
                                .between(BwcVisitorAppointment::getCreateTime, from, to)
                                .eq(BwcVisitorAppointment::getStatus, "APPROVED")));
                result.put("records", visitRecordMapper.selectCount(new LambdaQueryWrapper<BwcVisitRecord>()
                        .between(BwcVisitRecord::getInTime, from, to)));
            }
            case "vehicle" -> {
                result.put("total", vehicleMapper.selectCount(null));
                result.put("logs", vehicleLogMapper.selectCount(new LambdaQueryWrapper<BwcVehicleLog>()
                        .between(BwcVehicleLog::getTriggerTime, from, to)));
                result.put("plateLogs", plateLogMapper.selectCount(new LambdaQueryWrapper<BwcPlateLog>()
                        .between(BwcPlateLog::getTriggerTime, from, to)));
                result.put("violations", violationMapper.selectCount(new LambdaQueryWrapper<BwcViolation>()
                        .between(BwcViolation::getCreateTime, from, to)));
            }
            case "event" -> {
                result.put("total", securityEventMapper.selectCount(new LambdaQueryWrapper<BwcSecurityEvent>()
                        .between(BwcSecurityEvent::getCreateTime, from, to)));
                result.put("closed", securityEventMapper.selectCount(new LambdaQueryWrapper<BwcSecurityEvent>()
                        .between(BwcSecurityEvent::getCreateTime, from, to)
                        .eq(BwcSecurityEvent::getStatus, "CLOSED")));
            }
            case "fire" -> {
                result.put("facilities", fireFacilityMapper.selectCount(null));
                result.put("hazards", hazardMapper.selectCount(new LambdaQueryWrapper<BwcHazard>()
                        .between(BwcHazard::getReportTime, from, to)));
                result.put("alarms", fireAlarmMapper.selectCount(new LambdaQueryWrapper<BwcFireAlarm>()
                        .between(BwcFireAlarm::getAlarmTime, from, to)));
            }
            case "patrol" -> {
                result.put("tasks", patrolTaskMapper.selectCount(new LambdaQueryWrapper<BwcPatrolTask>()
                        .between(BwcPatrolTask::getPlanDate, from.toLocalDate(), to.toLocalDate())));
                result.put("records", patrolRecordMapper.selectCount(new LambdaQueryWrapper<BwcPatrolRecord>()
                        .between(BwcPatrolRecord::getCheckTime, from, to)));
            }
            case "lost" -> {
                result.put("items", lostItemMapper.selectCount(new LambdaQueryWrapper<BwcLostItem>()
                        .between(BwcLostItem::getFoundTime, from, to)));
                result.put("claims", lostClaimMapper.selectCount(new LambdaQueryWrapper<BwcLostClaim>()
                        .between(BwcLostClaim::getClaimTime, from, to)));
            }
            case "report" -> {
                result.put("total", reportApplyMapper.selectCount(new LambdaQueryWrapper<BwcReportApply>()
                        .between(BwcReportApply::getCreateTime, from, to)));
                result.put("approved", reportApplyMapper.selectCount(new LambdaQueryWrapper<BwcReportApply>()
                        .between(BwcReportApply::getCreateTime, from, to)
                        .eq(BwcReportApply::getStatus, "APPROVED")));
            }
            default -> result.put("overview", tenantOverview());
        }
        return result;
    }

    private long countOf(String table, Long tenantId) {
        if ("bwc_visitor_appointment".equals(table)) {
            return visitorAppointmentMapper.selectCount(
                    new QueryWrapper<BwcVisitorAppointment>().eq("tenant_id", tenantId));
        }
        return 0;
    }

    private long count(com.baomidou.mybatisplus.core.mapper.BaseMapper<?> mapper,
                       com.baomidou.mybatisplus.core.conditions.Wrapper<?> wrapper) {
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.core.mapper.BaseMapper<Object> m =
                (com.baomidou.mybatisplus.core.mapper.BaseMapper<Object>) mapper;
        @SuppressWarnings("unchecked")
        com.baomidou.mybatisplus.core.conditions.Wrapper<Object> w =
                (com.baomidou.mybatisplus.core.conditions.Wrapper<Object>) wrapper;
        Long value = m.selectCount(w);
        return value == null ? 0 : value;
    }

    private double rate(long part, long total) {
        if (total <= 0) {
            return 0D;
        }
        return Math.round(part * 10000D / total) / 100D;
    }
}
