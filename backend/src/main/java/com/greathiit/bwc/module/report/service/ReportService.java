package com.greathiit.bwc.module.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.NoGenerator;
import com.greathiit.bwc.module.report.entity.BwcMonitorAccess;
import com.greathiit.bwc.module.report.entity.BwcReportApply;
import com.greathiit.bwc.module.report.mapper.BwcMonitorAccessMapper;
import com.greathiit.bwc.module.report.mapper.BwcReportApplyMapper;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 报备审批（活动/施工动火/外出/危化品/宣传点位）与调取监控分级审批。
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final BwcReportApplyMapper applyMapper;
    private final BwcMonitorAccessMapper monitorMapper;
    private final NoGenerator noGenerator;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public BwcReportApply submit(BwcReportApply apply) {
        apply.setId(null);
        apply.setReportNo(noGenerator.next("RA"));
        apply.setStatus("PENDING");
        apply.setOwnerId(AuthContext.userId());
        if (apply.getApplicant() == null) {
            apply.setApplicant(AuthContext.username());
        }
        applyMapper.insert(apply);
        auditService.record("report", "SUBMIT", apply.getReportNo(), "报备提交：" + apply.getType());
        return apply;
    }

    @Transactional
    public BwcReportApply approve(Long id, boolean approved, String remark) {
        BwcReportApply apply = applyMapper.selectById(id);
        if (apply == null) {
            throw BizException.notFound("报备不存在");
        }
        if (!"PENDING".equals(apply.getStatus())) {
            throw new BizException(400, "该报备已审批");
        }
        apply.setStatus(approved ? "APPROVED" : "REJECTED");
        apply.setApprover(AuthContext.username());
        apply.setApproveTime(LocalDateTime.now());
        apply.setApproveRemark(remark);
        if (approved) {
            apply.setReceiptNo(noGenerator.next("RCPT"));
        }
        applyMapper.updateById(apply);
        notifyService.sendByTemplate(apply.getTenantId(), null, "TODO", "报备审批结果",
                apply.getTitle() + " 已" + (approved ? "通过，电子回执：" + apply.getReceiptNo() : "驳回"), id);
        auditService.record("report", approved ? "APPROVE" : "REJECT", apply.getReportNo(), "报备审批");
        return apply;
    }

    @Transactional
    public BwcMonitorAccess submitMonitor(BwcMonitorAccess access) {
        access.setId(null);
        access.setApplyNo(noGenerator.next("MA"));
        access.setStatus("PENDING");
        access.setApplicantId(AuthContext.userId());
        if (access.getApplicant() == null) {
            access.setApplicant(AuthContext.username());
        }
        monitorMapper.insert(access);
        auditService.record("report", "MONITOR_APPLY", access.getApplyNo(), "调取监控申请");
        return access;
    }

    @Transactional
    public BwcMonitorAccess approveMonitor(Long id, String stage, boolean approved, String remark) {
        BwcMonitorAccess access = monitorMapper.selectById(id);
        if (access == null) {
            throw BizException.notFound("申请不存在");
        }
        if (!approved) {
            access.setStatus("REJECTED");
            access.setFeedback(remark);
        } else if ("LEADER".equalsIgnoreCase(stage)) {
            if (!"DEPT_APPROVED".equals(access.getStatus())) {
                throw new BizException(400, "需先完成保卫处初审");
            }
            access.setStatus("APPROVED");
            access.setApprover2(AuthContext.username());
            access.setApprove2Time(LocalDateTime.now());
        } else {
            access.setStatus("DEPT_APPROVED");
            access.setApprover1(AuthContext.username());
            access.setApprove1Time(LocalDateTime.now());
        }
        monitorMapper.updateById(access);
        auditService.record("report", "MONITOR_APPROVE", access.getApplyNo(),
                "调监控审批：" + stage + " / " + (approved ? "通过" : "驳回"));
        return access;
    }

    @Transactional
    public BwcMonitorAccess recordAccess(Long id, String record, String result) {
        BwcMonitorAccess access = monitorMapper.selectById(id);
        if (access == null) {
            throw BizException.notFound("申请不存在");
        }
        if (!"APPROVED".equals(access.getStatus())) {
            throw new BizException(403, "未完成审批，禁止调取监控");
        }
        access.setAccessRecord(record);
        access.setResult(result);
        monitorMapper.updateById(access);
        auditService.record("report", "MONITOR_ACCESS", access.getApplyNo(), "监控调取登记：" + record);
        return access;
    }

    public IPage<BwcReportApply> applyPage(long page, long size, String type, String status, Long ownerId) {
        LambdaQueryWrapper<BwcReportApply> wrapper = new LambdaQueryWrapper<>();
        if (ownerId != null) {
            wrapper.eq(BwcReportApply::getOwnerId, ownerId);
        }
        if (type != null && !type.isBlank()) {
            wrapper.eq(BwcReportApply::getType, type);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcReportApply::getStatus, status);
        }
        wrapper.orderByDesc(BwcReportApply::getId);
        return applyMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public IPage<BwcMonitorAccess> monitorPage(long page, long size, String status, Long applicantId) {
        LambdaQueryWrapper<BwcMonitorAccess> wrapper = new LambdaQueryWrapper<>();
        if (applicantId != null) {
            wrapper.eq(BwcMonitorAccess::getApplicantId, applicantId);
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcMonitorAccess::getStatus, status);
        }
        wrapper.orderByDesc(BwcMonitorAccess::getId);
        return monitorMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("reportTotal", applyMapper.selectCount(null));
        result.put("reportPending", applyMapper.selectCount(new LambdaQueryWrapper<BwcReportApply>()
                .eq(BwcReportApply::getStatus, "PENDING")));
        result.put("monitorTotal", monitorMapper.selectCount(null));
        result.put("monitorPending", monitorMapper.selectCount(new LambdaQueryWrapper<BwcMonitorAccess>()
                .in(BwcMonitorAccess::getStatus, java.util.List.of("PENDING", "DEPT_APPROVED"))));
        return result;
    }
}
