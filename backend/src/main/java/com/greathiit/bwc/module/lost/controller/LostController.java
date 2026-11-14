package com.greathiit.bwc.module.lost.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.CryptoService;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.lost.entity.BwcLostClaim;
import com.greathiit.bwc.module.lost.entity.BwcLostDisposal;
import com.greathiit.bwc.module.lost.entity.BwcLostItem;
import com.greathiit.bwc.module.lost.entity.BwcLostNotice;
import com.greathiit.bwc.module.lost.mapper.BwcLostClaimMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostDisposalMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostNoticeMapper;
import com.greathiit.bwc.module.lost.service.LostService;
import com.greathiit.bwc.security.AccessGuard;
import com.greathiit.bwc.security.AuthContext;
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
    private final CryptoService cryptoService;
    private final AccessGuard accessGuard;

    @GetMapping("/items")
    @RequirePermission("lost:read")
    public R<IPage<BwcLostItem>> items(@RequestParam(defaultValue = "1") long page,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) String status,
                                       @RequestParam(required = false) String keyword) {
        accessGuard.requireRead();
        return R.ok(lostService.itemPage(page, size, status, keyword));
    }

    @PostMapping("/items")
    @RequirePermission("lost:write")
    public R<Map<String, Object>> register(@RequestBody BwcLostItem item) {
        accessGuard.requireSelfService();
        item.setId(null);
        if (item.getFinder() == null || item.getFinder().isBlank()) {
            item.setFinder(AuthContext.get() == null ? null : AuthContext.get().getRealName());
        }
        return R.ok(lostService.register(item));
    }

    @PostMapping("/items/{id}/publish")
    @RequirePermission("lost:write")
    public R<BwcLostNotice> publish(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        accessGuard.requireManager();
        return R.ok(lostService.publish(id, body == null ? null : body.get("title"),
                body == null ? null : body.get("content")));
    }

    @GetMapping("/notices")
    @RequirePermission("lost:read")
    public R<IPage<BwcLostNotice>> notices(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size) {
        accessGuard.requireRead();
        return R.ok(noticeMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BwcLostNotice>().orderByDesc(BwcLostNotice::getId)));
    }

    @GetMapping("/claims")
    @RequirePermission("lost:read")
    public R<IPage<BwcLostClaim>> claims(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BwcLostClaim> wrapper = new LambdaQueryWrapper<>();
        if (accessGuard.isSelfRole()) {
            wrapper.eq(BwcLostClaim::getClaimantId, AuthContext.userId());
        } else {
            accessGuard.requireRead();
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcLostClaim::getStatus, status);
        }
        wrapper.orderByDesc(BwcLostClaim::getId);
        return R.ok(claimMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/claims")
    @RequirePermission("lost:write")
    public R<BwcLostClaim> claim(@RequestBody BwcLostClaim claim) {
        accessGuard.requireManager();
        return R.ok(lostService.claim(claim));
    }

    /**
     * 小程序/登录用户自助认领：不依赖 lost:write，提交后只属于自己的认领记录。
     */
    @PostMapping("/claims/self")
    @RequirePermission("lost:write")
    public R<BwcLostClaim> selfClaim(@RequestBody Map<String, String> body) {
        accessGuard.requireSelfService();
        String phone = body.getOrDefault("phone", "").trim();
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new BizException(400, "请输入正确的手机号");
        }
        Long itemId;
        try {
            itemId = Long.valueOf(body.getOrDefault("itemId", ""));
        } catch (Exception e) {
            throw new BizException(400, "请选择要认领的物品");
        }
        BwcLostClaim claim = new BwcLostClaim();
        claim.setItemId(itemId);
        claim.setClaimant(body.getOrDefault("claimant", "").trim());
        claim.setPhoneMask(cryptoService.maskPhone(phone));
        claim.setIdCardMask(cryptoService.maskIdCard(body.get("idCard")));
        claim.setRemark(body.get("remark"));
        return R.ok(lostService.claim(claim));
    }

    @GetMapping("/claims/self")
    public R<List<BwcLostClaim>> myClaims() {
        return R.ok(claimMapper.selectList(new LambdaQueryWrapper<BwcLostClaim>()
                .eq(BwcLostClaim::getClaimantId, AuthContext.userId())
                .orderByDesc(BwcLostClaim::getId)));
    }

    @PostMapping("/claims/{id}/approve")
    @RequirePermission("lost:write")
    public R<BwcLostClaim> approveClaim(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        accessGuard.requireManager();
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", true)));
        return R.ok(lostService.approveClaim(id, approved,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @GetMapping("/disposals")
    @RequirePermission("lost:read")
    public R<IPage<BwcLostDisposal>> disposals(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "10") long size,
                                               @RequestParam(required = false) String status) {
        accessGuard.requireRead();
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
        accessGuard.requireManager();
        Long itemId = Long.valueOf(String.valueOf(body.get("itemId")));
        return R.ok(lostService.applyDisposal(itemId, (String) body.get("disposalType"),
                (String) body.get("reason")));
    }

    @PostMapping("/disposals/{id}/approve")
    @RequirePermission("lost:write")
    public R<BwcLostDisposal> approveDisposal(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        accessGuard.requireManager();
        boolean approved = Boolean.parseBoolean(String.valueOf(body.getOrDefault("approved", true)));
        return R.ok(lostService.approveDisposal(id, approved,
                body.get("remark") == null ? null : String.valueOf(body.get("remark"))));
    }

    @GetMapping("/stats")
    @RequirePermission("lost:read")
    public R<Map<String, Object>> stats() {
        accessGuard.requireRead();
        return R.ok(lostService.stats());
    }
}
