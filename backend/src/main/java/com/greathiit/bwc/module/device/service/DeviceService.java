package com.greathiit.bwc.module.device.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.NoGenerator;
import com.greathiit.bwc.module.device.entity.BwcDevice;
import com.greathiit.bwc.module.device.entity.BwcDeviceFollow;
import com.greathiit.bwc.module.device.entity.BwcDeviceRepair;
import com.greathiit.bwc.module.device.mapper.BwcDeviceFollowMapper;
import com.greathiit.bwc.module.device.mapper.BwcDeviceMapper;
import com.greathiit.bwc.module.device.mapper.BwcDeviceRepairMapper;
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
 * 安防设备运维：报修工单、分派、维修跟进、巡检。
 */
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final BwcDeviceMapper deviceMapper;
    private final BwcDeviceRepairMapper repairMapper;
    private final BwcDeviceFollowMapper followMapper;
    private final NoGenerator noGenerator;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public BwcDeviceRepair report(BwcDeviceRepair repair) {
        repair.setId(null);
        repair.setRepairNo(noGenerator.next("DR"));
        repair.setStatus("PENDING");
        repair.setReportTime(LocalDateTime.now());
        if (repair.getReporter() == null) {
            repair.setReporter(AuthContext.username());
        }
        repairMapper.insert(repair);
        if (repair.getDeviceId() != null) {
            BwcDevice device = deviceMapper.selectById(repair.getDeviceId());
            if (device != null) {
                device.setStatus("FAULT");
                deviceMapper.updateById(device);
            }
        }
        auditService.record("device", "REPAIR", repair.getRepairNo(), "设备报修");
        return repair;
    }

    @Transactional
    public BwcDeviceRepair assign(Long id, String assignTo) {
        BwcDeviceRepair repair = repairMapper.selectById(id);
        if (repair == null) {
            throw BizException.notFound("报修单不存在");
        }
        repair.setAssignTo(assignTo);
        repair.setStatus("ASSIGNED");
        repairMapper.updateById(repair);
        notifyService.sendByTemplate(repair.getTenantId(), null, "TODO", "维修工单分派",
                "报修单 " + repair.getRepairNo() + " 已分派给 " + assignTo, id);
        auditService.record("device", "ASSIGN", repair.getRepairNo(), "分派维修");
        return repair;
    }

    @Transactional
    public BwcDeviceFollow follow(Long repairId, BwcDeviceFollow follow) {
        BwcDeviceRepair repair = repairMapper.selectById(repairId);
        if (repair == null) {
            throw BizException.notFound("报修单不存在");
        }
        follow.setId(null);
        follow.setRepairId(repairId);
        follow.setFollowTime(LocalDateTime.now());
        if (follow.getOperator() == null) {
            follow.setOperator(AuthContext.username());
        }
        followMapper.insert(follow);
        repair.setStatus("REPAIRING");
        repairMapper.updateById(repair);
        auditService.record("device", "FOLLOW", repair.getRepairNo(), "维修跟进");
        return follow;
    }

    @Transactional
    public BwcDeviceRepair finish(Long id, String result) {
        BwcDeviceRepair repair = repairMapper.selectById(id);
        if (repair == null) {
            throw BizException.notFound("报修单不存在");
        }
        if ("DONE".equals(repair.getStatus())) {
            throw new BizException("报修单已完成，请勿重复操作");
        }
        repair.setStatus("DONE");
        repair.setFinishTime(LocalDateTime.now());
        repair.setResult(result);
        repairMapper.updateById(repair);
        if (repair.getDeviceId() != null) {
            BwcDevice device = deviceMapper.selectById(repair.getDeviceId());
            if (device != null) {
                device.setStatus("NORMAL");
                deviceMapper.updateById(device);
            }
        }
        auditService.record("device", "FINISH", repair.getRepairNo(), "维修完成");
        return repair;
    }

    public IPage<BwcDeviceRepair> page(long page, long size, String status) {
        LambdaQueryWrapper<BwcDeviceRepair> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcDeviceRepair::getStatus, status);
        }
        wrapper.orderByDesc(BwcDeviceRepair::getId);
        return repairMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> stats() {
        long total = deviceMapper.selectCount(null);
        long fault = deviceMapper.selectCount(new LambdaQueryWrapper<BwcDevice>().eq(BwcDevice::getStatus, "FAULT"));
        long repairs = repairMapper.selectCount(new LambdaQueryWrapper<BwcDeviceRepair>()
                .ne(BwcDeviceRepair::getStatus, "CLOSED"));
        Map<String, Object> result = new HashMap<>();
        result.put("deviceTotal", total);
        result.put("fault", fault);
        result.put("openRepairs", repairs);
        result.put("intactRate", total == 0 ? 100 : Math.round((total - fault) * 1000.0 / total) / 10.0);
        return result;
    }
}
