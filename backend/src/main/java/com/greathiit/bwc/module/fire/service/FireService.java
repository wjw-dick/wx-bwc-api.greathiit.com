package com.greathiit.bwc.module.fire.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.NoGenerator;
import com.greathiit.bwc.module.fire.entity.BwcFireFacility;
import com.greathiit.bwc.module.fire.entity.BwcFireInspection;
import com.greathiit.bwc.module.fire.entity.BwcHazard;
import com.greathiit.bwc.module.fire.mapper.BwcFireFacilityMapper;
import com.greathiit.bwc.module.fire.mapper.BwcFireInspectionMapper;
import com.greathiit.bwc.module.fire.mapper.BwcHazardMapper;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.module.system.service.ParamService;
import com.greathiit.bwc.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消防：设施台账（一物一码）、扫码巡检、隐患上报 → 分派 → 整改 → 验收销号。
 */
@Service
@RequiredArgsConstructor
public class FireService {

    private final BwcFireFacilityMapper facilityMapper;
    private final BwcFireInspectionMapper inspectionMapper;
    private final BwcHazardMapper hazardMapper;
    private final NoGenerator noGenerator;
    private final ParamService paramService;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public BwcFireFacility saveFacility(BwcFireFacility facility) {
        if (facility.getId() == null) {
            if (facility.getFacilityNo() == null || facility.getFacilityNo().isBlank()) {
                facility.setFacilityNo(noGenerator.next("FF"));
            }
            if (facility.getQrCode() == null || facility.getQrCode().isBlank()) {
                facility.setQrCode("QR-" + facility.getFacilityNo());
            }
            if (facility.getStatus() == null) {
                facility.setStatus("NORMAL");
            }
            facilityMapper.insert(facility);
        } else {
            facilityMapper.updateById(facility);
        }
        auditService.record("fire", "FACILITY_SAVE", facility.getFacilityNo(), "消防设施台账");
        return facility;
    }

    /**
     * 扫码巡检：异常自动转隐患。
     */
    @Transactional
    public Map<String, Object> inspect(String qrCode, Long facilityId, String result, String photos,
                                       String remark, String inspector) {
        BwcFireFacility facility = null;
        if (facilityId != null) {
            facility = facilityMapper.selectById(facilityId);
        } else if (qrCode != null) {
            facility = facilityMapper.selectOne(new LambdaQueryWrapper<BwcFireFacility>()
                    .eq(BwcFireFacility::getQrCode, qrCode));
        }
        if (facility == null) {
            throw BizException.notFound("设施不存在，请核对二维码");
        }
        BwcFireInspection inspection = new BwcFireInspection();
        inspection.setFacilityId(facility.getId());
        inspection.setFacilityNo(facility.getFacilityNo());
        inspection.setInspector(inspector);
        inspection.setInspectTime(LocalDateTime.now());
        inspection.setResult(result == null ? "NORMAL" : result);
        inspection.setPhotos(photos);
        inspection.setRemark(remark);
        boolean abnormal = "ABNORMAL".equalsIgnoreCase(inspection.getResult());
        inspection.setAbnormalFlag(abnormal ? 1 : 0);
        BwcHazard hazard = null;
        if (abnormal) {
            facility.setStatus("ABNORMAL");
            hazard = createHazardFromFacility(facility, "巡检异常：" + (remark == null ? "设施状态异常" : remark));
            inspection.setHazardId(hazard.getId());
        } else {
            facility.setStatus("NORMAL");
            facility.setLastCheckDate(LocalDate.now());
            facility.setNextCheckDate(LocalDate.now().plusMonths(1));
        }
        facilityMapper.updateById(facility);
        inspectionMapper.insert(inspection);
        Map<String, Object> map = new HashMap<>();
        map.put("inspection", inspection);
        map.put("hazard", hazard);
        auditService.record("fire", "INSPECT", facility.getFacilityNo(), "扫码巡检：" + inspection.getResult());
        return map;
    }

    @Transactional
    public BwcHazard createHazardFromFacility(BwcFireFacility facility, String description) {
        BwcHazard hazard = new BwcHazard();
        hazard.setHazardNo(noGenerator.next("HZ"));
        hazard.setSource("INSPECTION");
        hazard.setHazardLevel("GENERAL");
        hazard.setLocation(facility.getLocation());
        hazard.setDescription(description);
        hazard.setReporter(AuthContext.username());
        hazard.setReportTime(LocalDateTime.now());
        hazard.setStatus("REPORTED");
        hazardMapper.insert(hazard);
        return hazard;
    }

    @Transactional
    public BwcHazard reportHazard(BwcHazard hazard) {
        hazard.setId(null);
        hazard.setHazardNo(noGenerator.next("HZ"));
        hazard.setSource(hazard.getSource() == null ? "REPORT" : hazard.getSource());
        hazard.setReportTime(LocalDateTime.now());
        hazard.setStatus("REPORTED");
        hazardMapper.insert(hazard);
        if ("CRITICAL".equals(hazard.getHazardLevel())) {
            notifyService.sendByTemplate(hazard.getTenantId(), null, "WARNING", "重大隐患预警",
                    "发现重大隐患：" + hazard.getLocation() + "，" + hazard.getDescription(), hazard.getId());
        }
        auditService.record("hazard", "REPORT", hazard.getHazardNo(), "隐患上报");
        return hazard;
    }

    @Transactional
    public BwcHazard assignHazard(Long id, Long assignTo, String assignToName, Integer days) {
        BwcHazard hazard = hazardMapper.selectById(id);
        if (hazard == null) {
            throw BizException.notFound("隐患不存在");
        }
        int deadlineDays = days == null ? paramService.getInt("hazard_default_days", 7) : days;
        hazard.setAssignTo(assignTo);
        hazard.setAssignToName(assignToName);
        hazard.setAssignTime(LocalDateTime.now());
        hazard.setDeadline(LocalDateTime.now().plusDays(deadlineDays));
        hazard.setStatus("ASSIGNED");
        hazardMapper.updateById(hazard);
        notifyService.sendByTemplate(hazard.getTenantId(), assignTo, "HAZARD", "隐患整改督办",
                "隐患 " + hazard.getHazardNo() + " 已分派，请于 " + hazard.getDeadline() + " 前完成整改",
                hazard.getId());
        auditService.record("hazard", "ASSIGN", hazard.getHazardNo(), "分派责任人：" + assignToName);
        return hazard;
    }

    @Transactional
    public BwcHazard rectify(Long id, String plan, String photos) {
        BwcHazard hazard = hazardMapper.selectById(id);
        if (hazard == null) {
            throw BizException.notFound("隐患不存在");
        }
        if ("CLOSED".equals(hazard.getStatus())) {
            throw new BizException(400, "隐患已销号");
        }
        hazard.setRectifyPlan(plan);
        hazard.setRectifyPhotos(photos);
        hazard.setRectifyTime(LocalDateTime.now());
        hazard.setStatus("PENDING_ACCEPT");
        hazardMapper.updateById(hazard);
        auditService.record("hazard", "RECTIFY", hazard.getHazardNo(), "提交整改");
        return hazard;
    }

    @Transactional
    public BwcHazard accept(Long id, boolean approved, String remark) {
        BwcHazard hazard = hazardMapper.selectById(id);
        if (hazard == null) {
            throw BizException.notFound("隐患不存在");
        }
        if (!"PENDING_ACCEPT".equals(hazard.getStatus())) {
            throw new BizException(400, "当前状态不可验收");
        }
        if (approved) {
            hazard.setStatus("CLOSED");
            hazard.setAcceptTime(LocalDateTime.now());
            hazard.setAcceptBy(AuthContext.username());
            hazard.setAcceptRemark(remark);
        } else {
            hazard.setStatus("RECTIFYING");
            hazard.setAcceptRemark("验收不通过：" + remark);
            hazard.setDeadline(LocalDateTime.now().plusDays(3));
        }
        hazardMapper.updateById(hazard);
        auditService.record("hazard", approved ? "ACCEPT" : "REJECT", hazard.getHazardNo(),
                "整改验收：" + (approved ? "销号" : "退回"));
        return hazard;
    }

    public IPage<BwcHazard> hazardPage(long page, long size, String status, String level, String keyword) {
        LambdaQueryWrapper<BwcHazard> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcHazard::getStatus, status);
        }
        if (level != null && !level.isBlank()) {
            wrapper.eq(BwcHazard::getHazardLevel, level);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcHazard::getHazardNo, keyword)
                    .or().like(BwcHazard::getLocation, keyword));
        }
        wrapper.orderByDesc(BwcHazard::getId);
        return hazardMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        long facilityTotal = facilityMapper.selectCount(null);
        long facilityAbnormal = facilityMapper.selectCount(new LambdaQueryWrapper<BwcFireFacility>()
                .ne(BwcFireFacility::getStatus, "NORMAL"));
        long hazardTotal = hazardMapper.selectCount(null);
        long hazardClosed = hazardMapper.selectCount(new LambdaQueryWrapper<BwcHazard>()
                .eq(BwcHazard::getStatus, "CLOSED"));
        long overdue = hazardMapper.selectCount(new LambdaQueryWrapper<BwcHazard>()
                .ne(BwcHazard::getStatus, "CLOSED").lt(BwcHazard::getDeadline, LocalDateTime.now()));
        result.put("facilityTotal", facilityTotal);
        result.put("facilityAbnormal", facilityAbnormal);
        result.put("facilityIntactRate", facilityTotal == 0 ? 100
                : Math.round((facilityTotal - facilityAbnormal) * 1000.0 / facilityTotal) / 10.0);
        result.put("hazardTotal", hazardTotal);
        result.put("hazardClosed", hazardClosed);
        result.put("hazardClosedRate", hazardTotal == 0 ? 100
                : Math.round(hazardClosed * 1000.0 / hazardTotal) / 10.0);
        result.put("overdue", overdue);
        return result;
    }

    public List<BwcFireFacility> expiring(int days) {
        LocalDate deadline = LocalDate.now().plusDays(days);
        return facilityMapper.selectList(new LambdaQueryWrapper<BwcFireFacility>()
                .le(BwcFireFacility::getExpireDate, deadline)
                .orderByAsc(BwcFireFacility::getExpireDate));
    }

    @Transactional
    public void markExpiredFacilities() {
        List<BwcFireFacility> list = facilityMapper.selectList(new LambdaQueryWrapper<BwcFireFacility>()
                .lt(BwcFireFacility::getExpireDate, LocalDate.now())
                .ne(BwcFireFacility::getStatus, "EXPIRED"));
        for (BwcFireFacility facility : list) {
            facility.setStatus("EXPIRED");
            facilityMapper.updateById(facility);
            notifyService.sendByTemplate(facility.getTenantId(), null, "EXPIRE", "消防设施到期提醒",
                    "设施 " + facility.getFacilityNo() + " 已过期，请及时更换", facility.getId());
        }
    }
}
