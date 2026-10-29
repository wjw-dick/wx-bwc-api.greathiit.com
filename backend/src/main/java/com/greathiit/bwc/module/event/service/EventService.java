package com.greathiit.bwc.module.event.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.NoGenerator;
import com.greathiit.bwc.common.RateLimitService;
import com.greathiit.bwc.module.event.entity.BwcAlarmLink;
import com.greathiit.bwc.module.event.entity.BwcDispute;
import com.greathiit.bwc.module.event.entity.BwcMediation;
import com.greathiit.bwc.module.event.entity.BwcSecurityEvent;
import com.greathiit.bwc.module.event.entity.BwcWorkOrder;
import com.greathiit.bwc.module.event.mapper.BwcAlarmLinkMapper;
import com.greathiit.bwc.module.event.mapper.BwcDisputeMapper;
import com.greathiit.bwc.module.event.mapper.BwcMediationMapper;
import com.greathiit.bwc.module.event.mapper.BwcSecurityEventMapper;
import com.greathiit.bwc.module.event.mapper.BwcWorkOrderMapper;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 治安事件与工单：上报/一键报警 → 分派 → 处置 → 结案归档，纠纷调解与报警联动。
 */
@Service
@RequiredArgsConstructor
public class EventService {

    private final BwcSecurityEventMapper eventMapper;
    private final BwcWorkOrderMapper workOrderMapper;
    private final BwcDisputeMapper disputeMapper;
    private final BwcMediationMapper mediationMapper;
    private final BwcAlarmLinkMapper alarmLinkMapper;
    private final NoGenerator noGenerator;
    private final RateLimitService rateLimitService;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public BwcSecurityEvent report(BwcSecurityEvent event) {
        rateLimitService.check("event:report:" + AuthContext.userId(), 10, Duration.ofMinutes(1));
        rateLimitService.dedup("event:" + AuthContext.userId() + ":" + event.getEventType()
                + ":" + event.getLocation(), Duration.ofSeconds(30));
        event.setId(null);
        event.setEventNo(noGenerator.next("SE"));
        event.setStatus("REPORTED");
        event.setSource(event.getSource() == null ? "REPORT" : event.getSource());
        eventMapper.insert(event);
        if ("CRITICAL".equals(event.getUrgency())) {
            notifyService.sendByTemplate(event.getTenantId(), null, "WARNING", "重大事件预警",
                    "发生" + event.getEventType() + "事件：" + event.getLocation(), event.getId());
        }
        auditService.record("event", "REPORT", event.getEventNo(), "事件上报");
        return event;
    }

    @Transactional
    public BwcSecurityEvent alarm(BwcSecurityEvent event) {
        event.setEventType(event.getEventType() == null ? "求助" : event.getEventType());
        event.setUrgency("CRITICAL");
        event.setSource("ALARM");
        BwcSecurityEvent created = report(event);
        BwcAlarmLink link = new BwcAlarmLink();
        link.setTenantId(created.getTenantId());
        link.setAlarmType("ACCESS");
        link.setSource("ONE_CLICK");
        link.setEventId(created.getId());
        link.setContent("一键报警：" + created.getLocation());
        link.setLinkTime(LocalDateTime.now());
        link.setStatus("PENDING");
        alarmLinkMapper.insert(link);
        return created;
    }

    @Transactional
    public BwcSecurityEvent assign(Long id, Long assignTo, String assignToName, String requirement,
                                   LocalDateTime deadline) {
        BwcSecurityEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw BizException.notFound("事件不存在");
        }
        if ("CLOSED".equals(event.getStatus())) {
            throw new BizException(400, "事件已结案，无法分派");
        }
        event.setStatus("ASSIGNED");
        event.setAssignTo(assignTo);
        event.setAssignToName(assignToName);
        event.setAssignTime(LocalDateTime.now());
        eventMapper.updateById(event);

        BwcWorkOrder order = new BwcWorkOrder();
        order.setTenantId(event.getTenantId());
        order.setOrderNo(noGenerator.next("WO"));
        order.setEventId(id);
        order.setTitle(event.getEventType() + "事件处置 - " + event.getLocation());
        order.setAssignTo(assignTo);
        order.setAssignToName(assignToName);
        order.setStatus("PENDING");
        order.setDeadline(deadline == null ? LocalDateTime.now().plusHours(2) : deadline);
        workOrderMapper.insert(order);
        notifyService.sendByTemplate(event.getTenantId(), assignTo, "EVENT", "事件分派通知",
                "您有一条" + event.getEventType() + "事件待处置：" + event.getLocation()
                        + (requirement == null ? "" : "；要求：" + requirement), id);
        auditService.record("event", "ASSIGN", String.valueOf(id), "分派给：" + assignToName);
        return event;
    }

    @Transactional
    public BwcSecurityEvent handle(Long id, String result, String photos) {
        BwcSecurityEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw BizException.notFound("事件不存在");
        }
        event.setStatus("HANDLING");
        event.setHandleResult(result);
        event.setHandlePhotos(photos);
        eventMapper.updateById(event);
        List<BwcWorkOrder> orders = workOrderMapper.selectList(
                new LambdaQueryWrapper<BwcWorkOrder>().eq(BwcWorkOrder::getEventId, id));
        for (BwcWorkOrder order : orders) {
            order.setStatus("HANDLING");
            order.setResult(result);
            order.setPhotos(photos);
            workOrderMapper.updateById(order);
        }
        auditService.record("event", "HANDLE", String.valueOf(id), "现场处置");
        return event;
    }

    @Transactional
    public BwcSecurityEvent close(Long id, String result) {
        BwcSecurityEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw BizException.notFound("事件不存在");
        }
        event.setStatus("CLOSED");
        event.setCloseTime(LocalDateTime.now());
        if (result != null && !result.isBlank()) {
            event.setHandleResult(result);
        }
        eventMapper.updateById(event);
        List<BwcWorkOrder> orders = workOrderMapper.selectList(
                new LambdaQueryWrapper<BwcWorkOrder>().eq(BwcWorkOrder::getEventId, id));
        for (BwcWorkOrder order : orders) {
            order.setStatus("CLOSED");
            order.setFinishTime(LocalDateTime.now());
            workOrderMapper.updateById(order);
        }
        auditService.record("event", "CLOSE", String.valueOf(id), "事件结案归档");
        return event;
    }

    @Transactional
    public BwcDispute createDispute(BwcDispute dispute) {
        dispute.setId(null);
        dispute.setStatus("PENDING");
        disputeMapper.insert(dispute);
        auditService.record("event", "DISPUTE", String.valueOf(dispute.getId()), "纠纷登记");
        return dispute;
    }

    @Transactional
    public BwcMediation addMediation(Long disputeId, BwcMediation mediation) {
        BwcDispute dispute = disputeMapper.selectById(disputeId);
        if (dispute == null) {
            throw BizException.notFound("纠纷不存在");
        }
        mediation.setId(null);
        mediation.setDisputeId(disputeId);
        mediation.setMediationTime(mediation.getMediationTime() == null ? LocalDateTime.now() : mediation.getMediationTime());
        mediationMapper.insert(mediation);
        dispute.setStatus("MEDIATED");
        dispute.setMediator(mediation.getMediator());
        disputeMapper.updateById(dispute);
        auditService.record("event", "MEDIATION", String.valueOf(disputeId), "调解记录");
        return mediation;
    }

    public IPage<BwcDispute> disputePage(long page, long size, String status) {
        LambdaQueryWrapper<BwcDispute> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcDispute::getStatus, status);
        }
        wrapper.orderByDesc(BwcDispute::getId);
        return disputeMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public BwcAlarmLink linkAlarm(BwcAlarmLink link) {
        link.setId(null);
        link.setLinkTime(link.getLinkTime() == null ? LocalDateTime.now() : link.getLinkTime());
        link.setStatus("PENDING");
        alarmLinkMapper.insert(link);
        auditService.record("event", "ALARM_LINK", String.valueOf(link.getId()), "报警联动");
        return link;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("total", eventMapper.selectCount(null));
        result.put("reported", eventMapper.selectCount(new LambdaQueryWrapper<BwcSecurityEvent>()
                .eq(BwcSecurityEvent::getStatus, "REPORTED")));
        result.put("assigned", eventMapper.selectCount(new LambdaQueryWrapper<BwcSecurityEvent>()
                .eq(BwcSecurityEvent::getStatus, "ASSIGNED")));
        result.put("closed", eventMapper.selectCount(new LambdaQueryWrapper<BwcSecurityEvent>()
                .eq(BwcSecurityEvent::getStatus, "CLOSED")));
        List<Map<String, Object>> byType = eventMapper.selectMaps(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<BwcSecurityEvent>()
                        .select("event_type AS type", "COUNT(*) AS total").groupBy("event_type"));
        result.put("byType", byType);
        return result;
    }
}
