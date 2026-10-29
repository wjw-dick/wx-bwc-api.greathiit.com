package com.greathiit.bwc.module.visitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.common.NoGenerator;
import com.greathiit.bwc.common.RateLimitService;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.module.system.service.ParamService;
import com.greathiit.bwc.module.visitor.dto.VisitorCreateRequest;
import com.greathiit.bwc.module.visitor.entity.BwcBlacklist;
import com.greathiit.bwc.module.visitor.entity.BwcLateReturn;
import com.greathiit.bwc.module.visitor.entity.BwcTempEntry;
import com.greathiit.bwc.module.visitor.entity.BwcVisitRecord;
import com.greathiit.bwc.module.visitor.entity.BwcVisitorAppointment;
import com.greathiit.bwc.module.visitor.mapper.BwcBlacklistMapper;
import com.greathiit.bwc.module.visitor.mapper.BwcLateReturnMapper;
import com.greathiit.bwc.module.visitor.mapper.BwcTempEntryMapper;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitRecordMapper;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitorAppointmentMapper;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门禁与访客核心流程：预约 → 审批 → 通行码 → 扫码通行 → 台账/统计。
 */
@Service
@RequiredArgsConstructor
public class VisitorService {

    private final BwcVisitorAppointmentMapper appointmentMapper;
    private final BwcVisitRecordMapper visitRecordMapper;
    private final BwcBlacklistMapper blacklistMapper;
    private final BwcTempEntryMapper tempEntryMapper;
    private final BwcLateReturnMapper lateReturnMapper;
    private final CryptoService cryptoService;
    private final NoGenerator noGenerator;
    private final RateLimitService rateLimitService;
    private final ParamService paramService;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public BwcVisitorAppointment create(VisitorCreateRequest request, String source) {
        String phone = request.getPhone() == null ? "" : request.getPhone();
        rateLimitService.check("visitor:create:" + phone, 5, Duration.ofMinutes(10));
        rateLimitService.dedup("visitor:" + phone + ":" + request.getVisitTime(), Duration.ofMinutes(1));
        int advance = paramService.getInt("visitor_advance_hours", 2);
        if (request.getVisitTime().isBefore(LocalDateTime.now().plusHours(advance))) {
            throw new BizException(400, "访客预约需至少提前 " + advance + " 小时");
        }
        if (isBlacklisted(request.getVisitorName(), request.getIdCard())) {
            auditService.record("visitor", "BLOCKED", request.getVisitorName(), "黑名单拦截");
            throw BizException.forbidden("该访客在黑名单中，禁止入校");
        }
        BwcVisitorAppointment entity = new BwcVisitorAppointment();
        entity.setVisitorName(request.getVisitorName());
        entity.setIdCardMask(cryptoService.maskIdCard(request.getIdCard()));
        entity.setIdCardEnc(cryptoService.encrypt(request.getIdCard()));
        entity.setPhoneMask(cryptoService.maskPhone(request.getPhone()));
        entity.setPhoneEnc(cryptoService.encrypt(request.getPhone()));
        entity.setReason(request.getReason());
        entity.setVisitTime(request.getVisitTime());
        entity.setLeaveTime(request.getLeaveTime());
        entity.setAccompanyCount(request.getAccompanyCount() == null ? 0 : request.getAccompanyCount());
        entity.setPlateNo(request.getPlateNo());
        entity.setVisitUserId(request.getVisitUserId());
        entity.setVisitUserName(request.getVisitUserName());
        entity.setStatus("PENDING");
        entity.setSource(source == null ? "PC" : source);
        entity.setRemark(request.getRemark());
        appointmentMapper.insert(entity);
        if (entity.getVisitUserId() != null) {
            notifyService.sendByTemplate(entity.getTenantId(), entity.getVisitUserId(), "TODO",
                    "待办：访客预约审批",
                    "访客 " + entity.getVisitorName() + " 申请于 " + entity.getVisitTime() + " 来访，请及时审批。",
                    entity.getId());
        }
        auditService.record("visitor", "CREATE", String.valueOf(entity.getId()), "访客预约");
        return entity;
    }

    @Transactional
    public BwcVisitorAppointment approve(Long id, boolean approved, String remark) {
        BwcVisitorAppointment entity = appointmentMapper.selectById(id);
        if (entity == null) {
            throw BizException.notFound("预约不存在或无权访问");
        }
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BizException(400, "该预约已处理，请勿重复审批");
        }
        if (approved) {
            entity.setStatus("APPROVED");
            int validHours = paramService.getInt("pass_code_valid_hours", 24);
            entity.setPassCode(noGenerator.next("PASS"));
            LocalDateTime base = entity.getVisitTime().isAfter(LocalDateTime.now())
                    ? entity.getVisitTime() : LocalDateTime.now();
            entity.setPassExpireTime(base.plusHours(validHours));
        } else {
            entity.setStatus("REJECTED");
        }
        entity.setApproveBy(com.greathiit.bwc.security.AuthContext.userId());
        entity.setApproveTime(LocalDateTime.now());
        entity.setApproveRemark(remark);
        appointmentMapper.updateById(entity);
        notifyService.sendByTemplate(entity.getTenantId(), entity.getVisitUserId(), "VISITOR_APPROVE",
                "访客审批结果",
                "访客 " + entity.getVisitorName() + " 的预约已" + (approved ? "通过" : "驳回")
                        + (approved ? "，通行码：" + entity.getPassCode() : ""),
                entity.getId());
        auditService.record("visitor", approved ? "APPROVE" : "REJECT", String.valueOf(id),
                "访客审批：" + entity.getVisitorName());
        return entity;
    }

    @Transactional
    public Map<String, Object> checkPass(String passCode, String gate, String operator, String method) {
        BwcVisitorAppointment entity = appointmentMapper.selectOne(
                new LambdaQueryWrapper<BwcVisitorAppointment>().eq(BwcVisitorAppointment::getPassCode, passCode));
        if (entity == null) {
            throw new BizException(400, "通行码无效");
        }
        if ("EXPIRED".equals(entity.getStatus()) || entity.getPassExpireTime() == null
                || entity.getPassExpireTime().isBefore(LocalDateTime.now())) {
            entity.setStatus("EXPIRED");
            appointmentMapper.updateById(entity);
            throw new BizException(400, "通行码已过期");
        }
        if (!"APPROVED".equals(entity.getStatus())) {
            throw new BizException(400, "预约未通过审批，禁止通行");
        }
        if (isBlacklisted(entity.getVisitorName(), null)) {
            auditService.record("visitor", "BLOCKED", passCode, "黑名单通行拦截");
            throw BizException.forbidden("黑名单人员，禁止通行");
        }
        BwcVisitRecord record = visitRecordMapper.selectOne(new LambdaQueryWrapper<BwcVisitRecord>()
                .eq(BwcVisitRecord::getAppointmentId, entity.getId())
                .isNull(BwcVisitRecord::getOutTime)
                .last("limit 1"));
        String direction;
        if (record == null) {
            record = new BwcVisitRecord();
            record.setAppointmentId(entity.getId());
            record.setVisitorName(entity.getVisitorName());
            record.setPassCode(passCode);
            record.setGate(gate);
            record.setInTime(LocalDateTime.now());
            record.setOperator(operator);
            record.setMethod(method == null ? "QR" : method);
            record.setStatus("IN");
            visitRecordMapper.insert(record);
            direction = "IN";
        } else {
            record.setOutTime(LocalDateTime.now());
            record.setStatus("OUT");
            visitRecordMapper.updateById(record);
            direction = "OUT";
        }
        auditService.record("visitor", "PASS", passCode, "通行记录：" + direction);
        Map<String, Object> result = new HashMap<>();
        result.put("direction", direction);
        result.put("visitorName", entity.getVisitorName());
        result.put("visitUserName", entity.getVisitUserName());
        result.put("passExpireTime", entity.getPassExpireTime());
        result.put("record", record);
        return result;
    }

    @Transactional
    public BwcTempEntry createTempEntry(BwcTempEntry entry) {
        if (isBlacklisted(entry.getVisitorName(), null)) {
            throw BizException.forbidden("该访客在黑名单中，禁止入校");
        }
        entry.setIdCardMask(cryptoService.maskIdCard(entry.getIdCardMask()));
        entry.setPhoneMask(cryptoService.maskPhone(entry.getPhoneMask()));
        entry.setInTime(entry.getInTime() == null ? LocalDateTime.now() : entry.getInTime());
        entry.setStatus("IN");
        tempEntryMapper.insert(entry);
        auditService.record("visitor", "TEMP_ENTRY", entry.getVisitorName(), "临时入校登记");
        return entry;
    }

    @Transactional
    public BwcLateReturn confirmLateReturn(Long id, String confirmBy) {
        BwcLateReturn entity = lateReturnMapper.selectById(id);
        if (entity == null) {
            throw BizException.notFound("记录不存在");
        }
        entity.setConfirmStatus("CONFIRMED");
        entity.setConfirmBy(confirmBy);
        entity.setConfirmTime(LocalDateTime.now());
        lateReturnMapper.updateById(entity);
        auditService.record("visitor", "CONFIRM", String.valueOf(id), "晚归确认");
        return entity;
    }

    public boolean isBlacklisted(String name, String idCard) {
        LambdaQueryWrapper<BwcBlacklist> wrapper = new LambdaQueryWrapper<BwcBlacklist>()
                .eq(BwcBlacklist::getStatus, "ACTIVE");
        if (name != null && !name.isBlank()) {
            wrapper.and(w -> {
                w.eq(BwcBlacklist::getName, name);
                if (idCard != null && !idCard.isBlank()) {
                    w.or().eq(BwcBlacklist::getIdCardMask, cryptoService.maskIdCard(idCard));
                }
            });
        } else if (idCard != null && !idCard.isBlank()) {
            wrapper.eq(BwcBlacklist::getIdCardMask, cryptoService.maskIdCard(idCard));
        } else {
            return false;
        }
        return blacklistMapper.selectCount(wrapper) > 0;
    }

    public IPage<BwcVisitorAppointment> page(long page, long size, String status, String keyword,
                                             LocalDateTime start, LocalDateTime end) {
        LambdaQueryWrapper<BwcVisitorAppointment> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcVisitorAppointment::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcVisitorAppointment::getVisitorName, keyword)
                    .or().like(BwcVisitorAppointment::getPhoneMask, keyword)
                    .or().like(BwcVisitorAppointment::getVisitUserName, keyword));
        }
        if (start != null) {
            wrapper.ge(BwcVisitorAppointment::getVisitTime, start);
        }
        if (end != null) {
            wrapper.le(BwcVisitorAppointment::getVisitTime, end);
        }
        wrapper.orderByDesc(BwcVisitorAppointment::getId);
        return appointmentMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> stats(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> result = new HashMap<>();
        long total = appointmentMapper.selectCount(null);
        long pending = countByStatus("PENDING");
        long approved = countByStatus("APPROVED");
        long rejected = countByStatus("REJECTED");
        result.put("total", total);
        result.put("pending", pending);
        result.put("approved", approved);
        result.put("rejected", rejected);
        result.put("passRate", total == 0 ? 0 : Math.round(approved * 1000.0 / total) / 10.0);

        QueryWrapper<BwcVisitorAppointment> dayWrapper = new QueryWrapper<>();
        dayWrapper.select("DATE(visit_time) AS day", "COUNT(*) AS total")
                .ge(start != null, "visit_time", start)
                .le(end != null, "visit_time", end)
                .groupBy("DATE(visit_time)")
                .orderByAsc("day");
        result.put("byDay", appointmentMapper.selectMaps(dayWrapper));

        QueryWrapper<BwcVisitorAppointment> statusWrapper = new QueryWrapper<>();
        statusWrapper.select("status", "COUNT(*) AS total").groupBy("status");
        result.put("byStatus", appointmentMapper.selectMaps(statusWrapper));

        QueryWrapper<BwcVisitRecord> todayWrapper = new QueryWrapper<>();
        todayWrapper.select("COUNT(*) AS total").ge("in_time", LocalDate.now().atStartOfDay());
        result.put("todayVisits", visitRecordMapper.selectMaps(todayWrapper));
        return result;
    }

    private long countByStatus(String status) {
        return appointmentMapper.selectCount(
                new LambdaQueryWrapper<BwcVisitorAppointment>().eq(BwcVisitorAppointment::getStatus, status));
    }

    public void expirePassCodes() {
        List<BwcVisitorAppointment> list = appointmentMapper.selectList(
                new LambdaQueryWrapper<BwcVisitorAppointment>()
                        .eq(BwcVisitorAppointment::getStatus, "APPROVED")
                        .lt(BwcVisitorAppointment::getPassExpireTime, LocalDateTime.now()));
        for (BwcVisitorAppointment item : list) {
            item.setStatus("EXPIRED");
            appointmentMapper.updateById(item);
        }
    }

    public List<BwcVisitorAppointment> listForExport(String status) {
        LambdaQueryWrapper<BwcVisitorAppointment> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcVisitorAppointment::getStatus, status);
        }
        wrapper.orderByDesc(BwcVisitorAppointment::getId).last("limit 5000");
        return appointmentMapper.selectList(wrapper);
    }
}
