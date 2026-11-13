package com.greathiit.bwc.module.vehicle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateAuthRecord;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateLog;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicle;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicleAppointment;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicleLog;
import com.greathiit.bwc.module.vehicle.entity.BwcViolation;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateAuthRecordMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateLogMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleAppointmentMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleLogMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcViolationMapper;
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
    private final BwcViolationMapper violationMapper;
    private final PlateAuthService plateAuthService;
    private final NotifyService notifyService;
    private final AuditService auditService;
    private final CryptoService cryptoService;

    /**
     * 校内人员自助车辆备案：提交后进入待审核，由后台备案通过后下发固定车牌。
     */
    @Transactional
    public BwcVehicle registerVehicle(Map<String, String> body) {
        String plate = normalizePlate(body.get("plateNo"));
        if (plate == null) {
            throw new BizException(400, "请输入正确的车牌号，如 京A12345");
        }
        Long exists = vehicleMapper.selectCount(new LambdaQueryWrapper<BwcVehicle>()
                .eq(BwcVehicle::getPlateNo, plate));
        if (exists != null && exists > 0) {
            throw new BizException(409, "该车牌已备案，请勿重复提交");
        }
        BwcVehicle vehicle = new BwcVehicle();
        vehicle.setPlateNo(plate);
        vehicle.setOwnerId(AuthContext.userId());
        String ownerName = body.get("ownerName");
        if (ownerName == null || ownerName.isBlank()) {
            ownerName = AuthContext.get() == null ? null : AuthContext.get().getRealName();
        }
        vehicle.setOwnerName(ownerName);
        vehicle.setOwnerPhoneMask(cryptoService.maskPhone(body.get("phone")));
        vehicle.setVehicleType(body.getOrDefault("vehicleType", "小型车"));
        vehicle.setValidStart(parseDate(body.get("validStart")));
        vehicle.setValidEnd(parseDate(body.get("validEnd")));
        vehicle.setStatus("PENDING");
        vehicle.setAuthStatus("NONE");
        vehicle.setRemark(body.get("remark"));
        vehicleMapper.insert(vehicle);
        auditService.record("vehicle", "REGISTER", plate, "提交车辆备案：" + (ownerName == null ? "" : ownerName));
        return vehicle;
    }

    public List<BwcVehicle> myVehicles() {
        return vehicleMapper.selectList(new LambdaQueryWrapper<BwcVehicle>()
                .eq(BwcVehicle::getOwnerId, AuthContext.userId())
                .orderByDesc(BwcVehicle::getId));
    }

    /**
     * 后台备案审核：通过后自动尝试下发固定车牌。
     */
    @Transactional
    public BwcVehicle approveRegistration(Long id, boolean approved, String remark) {
        BwcVehicle vehicle = vehicleMapper.selectById(id);
        if (vehicle == null) {
            throw BizException.notFound("车辆备案不存在");
        }
        if (!approved) {
            vehicle.setStatus("REJECTED");
            vehicle.setRemark(remark);
            vehicleMapper.updateById(vehicle);
            auditService.record("vehicle", "REJECT", vehicle.getPlateNo(), "车辆备案驳回：" + remark);
            return vehicle;
        }
        vehicle.setStatus("NORMAL");
        if (remark != null && !remark.isBlank()) {
            vehicle.setRemark(remark);
        }
        vehicleMapper.updateById(vehicle);
        return issueFixed(id);
    }

    /**
     * 违停上报：校内人员/保安均可提交，后台处理。
     */
    @Transactional
    public BwcViolation reportViolation(Map<String, String> body) {
        String plate = normalizePlate(body.get("plateNo"));
        if (plate == null) {
            throw new BizException(400, "请输入正确的车牌号");
        }
        String location = body.get("location");
        if (location == null || location.isBlank()) {
            throw new BizException(400, "请填写违停地点");
        }
        BwcViolation violation = new BwcViolation();
        violation.setPlateNo(plate);
        violation.setLocation(location);
        violation.setViolateTime(parseDateTime(body.get("violateTime")));
        if (violation.getViolateTime() == null) {
            violation.setViolateTime(LocalDateTime.now());
        }
        violation.setDescription(body.get("description"));
        violation.setPhotos(body.get("photos"));
        violation.setHandleStatus("PENDING");
        violation.setOperator(AuthContext.username());
        violationMapper.insert(violation);
        auditService.record("vehicle", "VIOLATION", plate, "违停上报：" + location);
        return violation;
    }

    public List<BwcViolation> myViolations() {
        return violationMapper.selectList(new LambdaQueryWrapper<BwcViolation>()
                .eq(BwcViolation::getOperator, AuthContext.username())
                .orderByDesc(BwcViolation::getId));
    }

    @Transactional
    public BwcViolation handleViolation(Long id, String result) {
        BwcViolation violation = violationMapper.selectById(id);
        if (violation == null) {
            throw BizException.notFound("违停记录不存在");
        }
        violation.setHandleStatus("DONE");
        violation.setHandleResult(result);
        violation.setOperator(AuthContext.username());
        violationMapper.updateById(violation);
        auditService.record("vehicle", "VIOLATION_HANDLE", violation.getPlateNo(), "违停处理：" + result);
        return violation;
    }

    private String normalizePlate(String raw) {
        if (raw == null) {
            return null;
        }
        String plate = raw.replaceAll("\\s", "").toUpperCase();
        boolean ok = plate.matches("^[\\u4e00-\\u9fa5][A-Z][A-Z0-9]{5,6}$") || plate.matches("^[A-Z0-9]{6,8}$");
        return ok ? plate : null;
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return java.time.LocalDate.parse(value.substring(0, 10)).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

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

    /**
     * 访客预约携带车牌并审批通过后，自动生成车辆预约并下发临时车牌，避免门岗二次录入。
     */
    @Transactional
    public void createVisitorVehicle(String plateNo, String visitorName, String phoneMask, String reason,
                                     java.time.LocalDateTime visitTime, java.time.LocalDateTime leaveTime) {
        if (plateNo == null || plateNo.isBlank()) {
            return;
        }
        java.time.LocalDateTime start = visitTime == null ? java.time.LocalDateTime.now() : visitTime;
        java.time.LocalDateTime end = leaveTime == null ? start.plusHours(4) : leaveTime;
        Long exists = appointmentMapper.selectCount(new LambdaQueryWrapper<BwcVehicleAppointment>()
                .eq(BwcVehicleAppointment::getPlateNo, plateNo)
                .eq(BwcVehicleAppointment::getVisitTime, start)
                .in(BwcVehicleAppointment::getStatus, List.of("PENDING", "APPROVED")));
        if (exists != null && exists > 0) {
            return;
        }
        BwcVehicleAppointment appointment = new BwcVehicleAppointment();
        appointment.setPlateNo(plateNo);
        appointment.setVisitorName(visitorName);
        appointment.setPhoneMask(phoneMask);
        appointment.setReason(reason);
        appointment.setVisitTime(start);
        appointment.setLeaveTime(end);
        appointment.setStatus("APPROVED");
        appointment.setAuthStatus("PENDING");
        appointment.setApproveTime(LocalDateTime.now());
        appointment.setRemark("访客预约审批通过自动同步");
        appointmentMapper.insert(appointment);
        try {
            Map<String, Object> result = plateAuthService.addTemp(plateNo, start, end, "访客预约");
            appointment.setAuthStatus(toAuthStatus(result));
        } catch (Exception e) {
            recordFailure(plateNo, "TEMP", "TEMP", e.getMessage());
            appointment.setAuthStatus("FAILED");
        }
        appointmentMapper.updateById(appointment);
        auditService.record("vehicle", "VISITOR_SYNC", plateNo, "访客预约自动同步临时车牌");
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
