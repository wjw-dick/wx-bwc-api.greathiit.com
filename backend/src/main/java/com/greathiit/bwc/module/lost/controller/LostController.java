package com.greathiit.bwc.module.lost.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.lost.entity.BwcLostClaim;
import com.greathiit.bwc.module.lost.entity.BwcLostDisposal;
import com.greathiit.bwc.module.lost.entity.BwcLostItem;
import com.greathiit.bwc.module.lost.entity.BwcLostNotice;
import com.greathiit.bwc.module.lost.mapper.BwcLostClaimMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostDisposalMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostNoticeMapper;
import com.greathiit.bwc.module.lost.service.LostService;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 失物招领：登记、公告、自动匹配、认领、处置审批。
 */
@RestController
@RequestMapping("/api/lost")
@RequiredArgsConstructor
public class LostController {

    private final LostService lostService;
    private final BwcLostNoticeMapper noticeMapper;
    private final BwcLostClaimMapper claimMapper;
    private final BwcLostDisposalMapper disposalMapper;

    @GetMapping("/items")
    @RequirePermission("lost:read")
    public R<IPage<BwcLostItem>> items(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String keyword) {
        return R.ok(lostService.itemPage(page, size, status, keyword));
    }

    @PostMapping("/items")
    @RequirePermission("lost:write")
    public R<Map<String, Object>> register(@RequestBody BwcLostItem item) {
        return R.ok(lostService.register(item));
    }

    @PostMapping("/items/{id}/publish")
    @RequirePermission("lost:write")
    public R<BwcLostNotice> publish(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        return R.ok(lostService.publish(id, body == null ? null : body.get("title"),
                body == null ? null : body.get("content")));
    }

    @GetMapping("/notices")
    @RequirePermission("lost:read")
    public R<IPage<BwcLostNotice>> notices(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        return R.ok(noticeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BwcLostNotice>().orderByDesc(BwcLostNotice::getId)));
    }

    @GetMapping("/claims")
    @RequirePermission("lost:read")
    public R<IPage<BwcLostClaim>> claims(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BwcLostClaim> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcLostClaim::getStatus, status);
        }
        wrapper.orderByDesc(BwcLostClaim::getId);
        return R.ok(claimMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/claims")
    @RequirePermission("lost:write")
    public R<BwcLostClaim> claim(@RequestBody BwcLostClaim claim) {
        return R.ok(lostService.claim(claim));
    }

    @PostMapping("/claims/{id}/approve")
    @RequirePermission("lost:write")
    public R<BwcLostClaim> approveClaim(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", true)));
        return R.ok(lostService.approveClaim(id, approved,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @GetMapping("/disposals")
    @RequirePermission("lost:read")
    public R<IPage<BwcLostDisposal>> disposals(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BwcLostDisposal> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcLostDisposal::getStatus, status);
        }
        wrapper.orderByDesc(BwcLostDisposal::getId);
        return R.ok(disposalMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/disposals")
    @RequirePermission("lost:write")
    public R<BwcLostDisposal> applyDisposal(@RequestBody Map<String, Object> body) {
        Long itemId = Long.valueOf(String.valueOf(body.get("itemId")));
        return R.ok(lostService.applyDisposal(itemId, (String) body.get("disposalType"),
                (String) body.get("reason")));
    }

    @PostMapping("/disposals/{id}/approve")
    @RequirePermission("lost:write")
    public R<BwcLostDisposal> approveDisposal(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", true)));
        return R.ok(lostService.approveDisposal(id, approved,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @GetMapping("/stats")
    @RequirePermission("lost:read")
    public R<Map<String, Object>> stats() {
        return R.ok(lostService.stats());
    }
}
