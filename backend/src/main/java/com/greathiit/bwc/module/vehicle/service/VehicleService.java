package com.greathiit.bwc.module.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateAuthRecord;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateLog;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicle;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicleAppointment;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicleLog;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateAuthRecordMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateLogMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleAppointmentMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleLogMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleMapper;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 车辆备案、外来车辆预约审批、固定/临时车牌下发、进出日志同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final BwcVehicleMapper vehicleMapper;
    private final BwcVehicleAppointmentMapper appointmentMapper;
    private final BwcVehicleLogMapper vehicleLogMapper;
    private final BwcPlateLogMapper plateLogMapper;
    private final BwcPlateAuthRecordMapper authRecordMapper;
    private final PlateAuthService plateAuthService;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public BwcVehicle issueFixed(Long vehicleId) {
        BwcVehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw BizException.notFound("车辆不存在");
        }
        vehicle.setAuthStatus("PENDING");
        vehicleMapper.updateById(vehicle);
        try {
            Map<String, Object> result = plateAuthService.addAll(vehicle.getPlateNo(),
                    AuthContext.username(), "ADD_ALL");
            vehicle.setAuthStatus(toAuthStatus(result));
        } catch (Exception e) {
            vehicle.setAuthStatus("FAILED");
            recordFailure(vehicle.getPlateNo(), "FIXED", "ADD_ALL", e.getMessage());
        }
        vehicleMapper.updateById(vehicle);
        auditService.record("vehicle", "PLATE_ADD", vehicle.getPlateNo(), "固定车牌下发：" + vehicle.getAuthStatus());
        return vehicle;
    }

    @Transactional
    public BwcVehicle removeFixed(Long vehicleId) {
        BwcVehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw BizException.notFound("车辆不存在");
        }
        try {
            Map<String, Object> result = plateAuthService.deleteAll(vehicle.getPlateNo(), AuthContext.username());
            vehicle.setAuthStatus(toAuthStatus(result));
        } catch (Exception e) {
            recordFailure(vehicle.getPlateNo(), "FIXED", "DEL_ALL", e.getMessage());
            vehicle.setAuthStatus("FAILED");
        }
        vehicleMapper.updateById(vehicle);
        auditService.record("vehicle", "PLATE_DEL", vehicle.getPlateNo(), "车牌注销：" + vehicle.getAuthStatus());
        return vehicle;
    }

    @Transactional
    public BwcVehicleAppointment approveAppointment(Long id, boolean approved, String remark) {
        BwcVehicleAppointment appointment = appointmentMapper.selectById(id);
        if (appointment == null) {
            throw BizException.notFound("车辆预约不存在");
        }
        if (!"PENDING".equals(appointment.getStatus())) {
            throw new BizException(400, "该预约已处理");
        }
        appointment.setStatus(approved ? "APPROVED" : "REJECTED");
        appointment.setApproveBy(AuthContext.userId());
        appointment.setApproveTime(LocalDateTime.now());
        appointment.setRemark(remark);
        if (approved) {
            try {
                Map<String, Object> result = plateAuthService.addTemp(appointment.getPlateNo(),
                        appointment.getVisitTime(), appointment.getLeaveTime(), AuthContext.username());
                appointment.setAuthStatus(toAuthStatus(result));
            } catch (Exception e) {
                recordFailure(appointment.getPlateNo(), "TEMP", "TEMP", e.getMessage());
                appointment.setAuthStatus("FAILED");
            }
        }
        appointmentMapper.updateById(appointment);
        notifyService.sendByTemplate(appointment.getTenantId(), null, "VISITOR_APPROVE",
                "车辆预约审批结果",
                "车牌 " + appointment.getPlateNo() + " 的入校预约已" + (approved ? "通过" : "驳回"), id);
        auditService.record("vehicle", approved ? "APPROVE" : "REJECT", String.valueOf(id),
                "车辆预约审批：" + appointment.getPlateNo());
        return appointment;
    }

    @Transactional
    public BwcVehicleAppointment retryAppointment(Long id) {
        BwcVehicleAppointment appointment = appointmentMapper.selectById(id);
        if (appointment == null || !"APPROVED".equals(appointment.getStatus())) {
            throw BizException.notFound("预约不存在或未通过审批");
        }
        appointment.setAuthStatus("PENDING");
        appointmentMapper.updateById(appointment);
        try {
            Map<String, Object> result = plateAuthService.addTemp(appointment.getPlateNo(),
                    appointment.getVisitTime(), appointment.getLeaveTime(), AuthContext.username());
            appointment.setAuthStatus(toAuthStatus(result));
        } catch (Exception e) {
            recordFailure(appointment.getPlateNo(), "TEMP", "TEMP", e.getMessage());
            appointment.setAuthStatus("FAILED");
        }
        appointmentMapper.updateById(appointment);
        return appointment;
    }

    public IPage<BwcVehicle> pageVehicle(long page, long size, String keyword, String authStatus) {
        LambdaQueryWrapper<BwcVehicle> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcVehicle::getPlateNo, keyword)
                    .or().like(BwcVehicle::getOwnerName, keyword));
        }
        if (authStatus != null && !authStatus.isBlank()) {
            wrapper.eq(BwcVehicle::getAuthStatus, authStatus);
        }
        wrapper.orderByDesc(BwcVehicle::getId);
        return vehicleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public int syncLogs() {
        Map<?, ?> response = plateAuthService.logs(null, null, null, null, null);
        List<Map<String, Object>> rows = extract(response, "logs");
        int count = 0;
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Map<String, Object> row : rows) {
            String plateNo = str(row.get("plate_num"));
            String cameraId = str(row.get("camera_id"));
            String trigger = str(row.get("trigger_time"));
            if (plateNo == null || trigger == null) {
                continue;
            }
            LocalDateTime triggerTime;
            try {
                triggerTime = LocalDateTime.parse(trigger.replace("T", " ").substring(0, 19), fmt);
            } catch (Exception e) {
                triggerTime = LocalDateTime.now();
            }
            Long exists = plateLogMapper.selectCount(new LambdaQueryWrapper<BwcPlateLog>()
                    .eq(BwcPlateLog::getPlateNo, plateNo)
                    .eq(BwcPlateLog::getTriggerTime, triggerTime)
                    .eq(cameraId != null, BwcPlateLog::getCameraId, cameraId));
            if (exists != null && exists > 0) {
                continue;
            }
            BwcPlateLog plateLog = new BwcPlateLog();
            plateLog.setTenantId(TenantContext.getTenantId());
            plateLog.setPlateNo(plateNo);
            plateLog.setCameraId(cameraId);
            plateLog.setCameraName(str(row.get("camera_name")));
            plateLog.setInOut(str(row.get("in_out")) == null ? "in" : str(row.get("in_out")));
            plateLog.setTriggerTime(triggerTime);
            plateLog.setIsWhitelist(parseBool(row.get("is_whitelist")) ? 1 : 0);
            plateLog.setImageUrl(str(row.get("image_url")));
            plateLogMapper.insert(plateLog);

            BwcVehicleLog vehicleLog = new BwcVehicleLog();
            vehicleLog.setTenantId(TenantContext.getTenantId());
            vehicleLog.setPlateNo(plateNo);
            vehicleLog.setCameraId(cameraId);
            vehicleLog.setCameraName(plateLog.getCameraName());
            vehicleLog.setInOut(plateLog.getInOut());
            vehicleLog.setTriggerTime(triggerTime);
            vehicleLog.setIsWhitelist(plateLog.getIsWhitelist());
            vehicleLog.setImageUrl(plateLog.getImageUrl());
            vehicleLogMapper.insert(vehicleLog);
            count++;
        }
        auditService.record("vehicle", "SYNC_LOGS", "plate", "同步进出日志 " + count + " 条");
        return count;
    }

    private String toAuthStatus(Map<String, Object> result) {
        int failed = Integer.parseInt(String.valueOf(result.getOrDefault("failed", 0)));
        int success = Integer.parseInt(String.valueOf(result.getOrDefault("success", 0)));
        if (failed == 0 && success > 0) {
            return "ACTIVE";
        }
        return failed > 0 ? "FAILED" : "PENDING";
    }

    private void recordFailure(String plateNo, String type, String action, String msg) {
        BwcPlateAuthRecord record = new BwcPlateAuthRecord();
        record.setTenantId(TenantContext.getTenantId());
        record.setPlateNo(plateNo);
        record.setPlateType(type);
        record.setAction(action);
        record.setStatus("failed");
        record.setMessage(msg);
        record.setOperator(AuthContext.username());
        authRecordMapper.insert(record);
    }

    private List<Map<String, Object>> extract(Map<?, ?> response, String key) {
        Object data = response == null ? null : response.get(key);
        if (data instanceof List<?> list) {
            return list.stream().filter(o -> o instanceof Map).map(o -> {
                Map<String, Object> map = new java.util.LinkedHashMap<>();
                ((Map<?, ?>) o).forEach((k, v) -> map.put(String.valueOf(k), v));
                return map;
            }).toList();
        }
        return List.of();
    }

    private String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private boolean parseBool(Object value) {
        return value != null && (Boolean.parseBoolean(String.valueOf(value))
                || "1".equals(String.valueOf(value)));
    }
}
