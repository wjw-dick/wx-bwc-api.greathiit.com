package com.greathiit.bwc.module.event.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.event.entity.BwcAlarmLink;
import com.greathiit.bwc.module.event.entity.BwcDispute;
import com.greathiit.bwc.module.event.entity.BwcMediation;
import com.greathiit.bwc.module.event.entity.BwcSecurityEvent;
import com.greathiit.bwc.module.event.mapper.BwcAlarmLinkMapper;
import com.greathiit.bwc.module.event.service.EventService;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 治安事件：一键报警、上报、分派、处置、结案；纠纷调解与报警联动。
 */
@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final com.greathiit.bwc.module.event.mapper.BwcSecurityEventMapper eventMapper;
    private final BwcAlarmLinkMapper alarmLinkMapper;

    @GetMapping("/events")
    @RequirePermission("event:read")
    public R<IPage<BwcSecurityEvent>> events(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String eventType,
                                             @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BwcSecurityEvent> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcSecurityEvent::getStatus, status);
        }
        if (eventType != null && !eventType.isBlank()) {
            wrapper.eq(BwcSecurityEvent::getEventType, eventType);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcSecurityEvent::getEventNo, keyword)
                    .or().like(BwcSecurityEvent::getLocation, keyword)
                    .or().like(BwcSecurityEvent::getDescription, keyword));
        }
        wrapper.orderByDesc(BwcSecurityEvent::getId);
        return R.ok(eventMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/report")
    @RequirePermission("event:write")
    public R<BwcSecurityEvent> report(@RequestBody BwcSecurityEvent event) {
        return R.ok(eventService.report(event));
    }

    @PostMapping("/alarm")
    @RequirePermission("event:write")
    public R<BwcSecurityEvent> alarm(@RequestBody BwcSecurityEvent event) {
        return R.ok(eventService.alarm(event));
    }

    @PostMapping("/{id}/assign")
    @RequirePermission("event:write")
    public R<BwcSecurityEvent> assign(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long assignTo = body.get("assignTo") == null ? null : Long.valueOf(String.valueOf(body.get("assignTo")));
        String name = body.get("assignToName") == null ? null : String.valueOf(body.get("assignToName"));
        String requirement = body.get("requirement") == null ? null : String.valueOf(body.get("requirement"));
        LocalDateTime deadline = body.get("deadline") == null ? null
                : LocalDateTime.parse(String.valueOf(body.get("deadline")).replace("T", " "),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return R.ok(eventService.assign(id, assignTo, name, requirement, deadline));
    }

    @PostMapping("/{id}/handle")
    @RequirePermission("event:write")
    public R<BwcSecurityEvent> handle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(eventService.handle(id, body.get("result"), body.get("photos")));
    }

    @PostMapping("/{id}/close")
    @RequirePermission("event:write")
    public R<BwcSecurityEvent> close(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return R.ok(eventService.close(id, body == null ? null : body.get("result")));
    }

    @GetMapping("/stats")
    @RequirePermission("event:read")
    public R<Map<String, Object>> stats() {
        return R.ok(eventService.stats());
    }

    @GetMapping("/disputes")
    @RequirePermission("event:read")
    public R<IPage<BwcDispute>> disputes(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String status) {
        return R.ok(eventService.disputePage(page, size, status));
    }

    @PostMapping("/disputes")
    @RequirePermission("event:write")
    public R<BwcDispute> createDispute(@RequestBody BwcDispute dispute) {
        return R.ok(eventService.createDispute(dispute));
    }

    @PostMapping("/disputes/{id}/mediations")
    @RequirePermission("event:write")
    public R<BwcMediation> mediation(@PathVariable Long id, @RequestBody BwcMediation mediation) {
        return R.ok(eventService.addMediation(id, mediation));
    }

    @GetMapping("/alarms")
    @RequirePermission("event:read")
    public R<IPage<BwcAlarmLink>> alarms(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size) {
        return R.ok(alarmLinkMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BwcAlarmLink>().orderByDesc(BwcAlarmLink::getId)));
    }

    @PostMapping("/alarms")
    @RequirePermission("event:write")
    public R<BwcAlarmLink> linkAlarm(@RequestBody BwcAlarmLink link) {
        return R.ok(eventService.linkAlarm(link));
    }
}
