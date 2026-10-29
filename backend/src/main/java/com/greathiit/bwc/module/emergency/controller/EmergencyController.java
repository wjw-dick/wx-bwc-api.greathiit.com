package com.greathiit.bwc.module.emergency.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.emergency.entity.BwcEmergencyContact;
import com.greathiit.bwc.module.emergency.entity.BwcEmergencyEvent;
import com.greathiit.bwc.module.emergency.entity.BwcEmergencyPlan;
import com.greathiit.bwc.module.emergency.entity.BwcEmergencySupply;
import com.greathiit.bwc.module.emergency.entity.BwcSupplyRecord;
import com.greathiit.bwc.module.emergency.mapper.BwcEmergencyContactMapper;
import com.greathiit.bwc.module.emergency.mapper.BwcEmergencyEventMapper;
import com.greathiit.bwc.module.emergency.mapper.BwcEmergencyPlanMapper;
import com.greathiit.bwc.module.emergency.mapper.BwcEmergencySupplyMapper;
import com.greathiit.bwc.module.emergency.mapper.BwcSupplyRecordMapper;
import com.greathiit.bwc.module.emergency.service.EmergencyService;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 应急管理：预案库、物资台账（领用/归还）、通讯录、突发事件上报与响应。
 */
@RestController
@RequestMapping("/api/emergency")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyService emergencyService;
    private final BwcEmergencyPlanMapper planMapper;
    private final BwcEmergencySupplyMapper supplyMapper;
    private final BwcSupplyRecordMapper recordMapper;
    private final BwcEmergencyContactMapper contactMapper;
    private final BwcEmergencyEventMapper eventMapper;

    @GetMapping("/plans")
    @RequirePermission("emergency:read")
    public R<IPage<BwcEmergencyPlan>> plans(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BwcEmergencyPlan> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcEmergencyPlan::getTitle, keyword)
                    .or().like(BwcEmergencyPlan::getType, keyword));
        }
        wrapper.orderByDesc(BwcEmergencyPlan::getId);
        return R.ok(planMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/plans")
    @RequirePermission("emergency:write")
    public R<BwcEmergencyPlan> createPlan(@RequestBody BwcEmergencyPlan plan) {
        plan.setId(null);
        if (plan.getStatus() == null) {
            plan.setStatus("DRAFT");
        }
        planMapper.insert(plan);
        return R.ok(plan);
    }

    @PutMapping("/plans/{id}")
    @RequirePermission("emergency:write")
    public R<BwcEmergencyPlan> updatePlan(@PathVariable Long id, @RequestBody BwcEmergencyPlan plan) {
        plan.setId(id);
        planMapper.updateById(plan);
        return R.ok(planMapper.selectById(id));
    }

    @PostMapping("/plans/{id}/publish")
    @RequirePermission("emergency:write")
    public R<BwcEmergencyPlan> publishPlan(@PathVariable Long id) {
        BwcEmergencyPlan plan = planMapper.selectById(id);
        if (plan == null) {
            return R.fail(404, "预案不存在");
        }
        plan.setStatus("PUBLISHED");
        plan.setPublishTime(LocalDateTime.now());
        planMapper.updateById(plan);
        return R.ok(plan);
    }

    @GetMapping("/supplies")
    @RequirePermission("emergency:read")
    public R<IPage<BwcEmergencySupply>> supplies(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "10") long size,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) Boolean lowStock) {
        LambdaQueryWrapper<BwcEmergencySupply> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcEmergencySupply::getName, keyword)
                    .or().like(BwcEmergencySupply::getCategory, keyword));
        }
        if (Boolean.TRUE.equals(lowStock)) {
            wrapper.lt(BwcEmergencySupply::getQuantity, 5);
        }
        wrapper.orderByDesc(BwcEmergencySupply::getId);
        return R.ok(supplyMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/supplies")
    @RequirePermission("emergency:write")
    public R<BwcEmergencySupply> createSupply(@RequestBody BwcEmergencySupply supply) {
        supply.setId(null);
        if (supply.getQuantity() == null) {
            supply.setQuantity(0);
        }
        if (supply.getStatus() == null) {
            supply.setStatus("NORMAL");
        }
        supplyMapper.insert(supply);
        return R.ok(supply);
    }

    @PutMapping("/supplies/{id}")
    @RequirePermission("emergency:write")
    public R<BwcEmergencySupply> updateSupply(@PathVariable Long id, @RequestBody BwcEmergencySupply supply) {
        supply.setId(id);
        supplyMapper.updateById(supply);
        return R.ok(supplyMapper.selectById(id));
    }

    @PostMapping("/supplies/{id}/borrow")
    @RequirePermission("emergency:write")
    public R<BwcSupplyRecord> borrow(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int quantity = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", 1)));
        String userName = body.get("userName") == null ? null : String.valueOf(body.get("userName"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(emergencyService.borrow(id, quantity, userName, remark));
    }

    @PostMapping("/supplies/{id}/return")
    @RequirePermission("emergency:write")
    public R<BwcSupplyRecord> giveBack(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        int quantity = Integer.parseInt(String.valueOf(body.getOrDefault("quantity", 1)));
        String userName = body.get("userName") == null ? null : String.valueOf(body.get("userName"));
        String remark = body.get("remark") == null ? null : String.valueOf(body.get("remark"));
        return R.ok(emergencyService.giveBack(id, quantity, userName, remark));
    }

    @GetMapping("/supply-records")
    @RequirePermission("emergency:read")
    public R<IPage<BwcSupplyRecord>> supplyRecords(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Long supplyId) {
        LambdaQueryWrapper<BwcSupplyRecord> wrapper = new LambdaQueryWrapper<>();
        if (supplyId != null) {
            wrapper.eq(BwcSupplyRecord::getSupplyId, supplyId);
        }
        wrapper.orderByDesc(BwcSupplyRecord::getId);
        return R.ok(recordMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/contacts")
    @RequirePermission("emergency:read")
    public R<List<BwcEmergencyContact>> contacts() {
        return R.ok(contactMapper.selectList(new LambdaQueryWrapper<BwcEmergencyContact>()
                .orderByAsc(BwcEmergencyContact::getSort)
                .orderByAsc(BwcEmergencyContact::getId)));
    }

    @PostMapping("/contacts")
    @RequirePermission("emergency:write")
    public R<BwcEmergencyContact> createContact(@RequestBody BwcEmergencyContact contact) {
        contact.setId(null);
        contactMapper.insert(contact);
        return R.ok(contact);
    }

    @PutMapping("/contacts/{id}")
    @RequirePermission("emergency:write")
    public R<BwcEmergencyContact> updateContact(@PathVariable Long id, @RequestBody BwcEmergencyContact contact) {
        contact.setId(id);
        contactMapper.updateById(contact);
        return R.ok(contactMapper.selectById(id));
    }

    @DeleteMapping("/contacts/{id}")
    @RequirePermission("emergency:write")
    public R<Void> deleteContact(@PathVariable Long id) {
        contactMapper.deleteById(id);
        return R.ok();
    }

    @GetMapping("/events")
    @RequirePermission("emergency:read")
    public R<IPage<BwcEmergencyEvent>> events(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BwcEmergencyEvent> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcEmergencyEvent::getStatus, status);
        }
        wrapper.orderByDesc(BwcEmergencyEvent::getId);
        return R.ok(eventMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/events")
    @RequirePermission("emergency:write")
    public R<BwcEmergencyEvent> report(@RequestBody BwcEmergencyEvent event) {
        return R.ok(emergencyService.report(event));
    }

    @PostMapping("/events/{id}/respond")
    @RequirePermission("emergency:write")
    public R<BwcEmergencyEvent> respond(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return R.ok(emergencyService.respond(id, body.get("response")));
    }

    @GetMapping("/stats")
    @RequirePermission("emergency:read")
    public R<Map<String, Object>> stats() {
        return R.ok(emergencyService.stats());
    }
}
