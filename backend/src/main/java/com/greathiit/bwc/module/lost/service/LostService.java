package com.greathiit.bwc.module.lost.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.NoGenerator;
import com.greathiit.bwc.module.lost.entity.BwcLostClaim;
import com.greathiit.bwc.module.lost.entity.BwcLostDisposal;
import com.greathiit.bwc.module.lost.entity.BwcLostItem;
import com.greathiit.bwc.module.lost.entity.BwcLostNotice;
import com.greathiit.bwc.module.lost.mapper.BwcLostClaimMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostDisposalMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostItemMapper;
import com.greathiit.bwc.module.lost.mapper.BwcLostNoticeMapper;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 失物招领：登记 → 公告 → 自动匹配 → 认领 → 未认领处置审批。
 */
@Service
@RequiredArgsConstructor
public class LostService {

    private final BwcLostItemMapper itemMapper;
    private final BwcLostNoticeMapper noticeMapper;
    private final BwcLostClaimMapper claimMapper;
    private final BwcLostDisposalMapper disposalMapper;
    private final NoGenerator noGenerator;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public Map<String, Object> register(BwcLostItem item) {
        item.setId(null);
        item.setItemNo(item.getItemNo() == null || item.getItemNo().isBlank()
                ? noGenerator.next("LF") : item.getItemNo());
        item.setStatus("PUBLISHED");
        item.setFoundTime(item.getFoundTime() == null ? LocalDateTime.now() : item.getFoundTime());
        itemMapper.insert(item);
        List<BwcLostItem> matches = match(item);
        Map<String, Object> result = new HashMap<>();
        result.put("item", item);
        result.put("matches", matches);
        auditService.record("lost", "REGISTER", item.getItemNo(), "失物登记");
        return result;
    }

    /**
     * 自动匹配：同分类 + 名称/地点相似，且已发布。
     */
    public List<BwcLostItem> match(BwcLostItem item) {
        LambdaQueryWrapper<BwcLostItem> wrapper = new LambdaQueryWrapper<BwcLostItem>()
                .eq(BwcLostItem::getStatus, "PUBLISHED")
                .ne(item.getId() != null, BwcLostItem::getId, item.getId());
        if (item.getCategory() == null && item.getLocation() == null && item.getName() == null) {
            return List.of();
        }
        wrapper.and(w -> {
            boolean first = true;
            if (item.getCategory() != null) {
                w.eq(BwcLostItem::getCategory, item.getCategory());
                first = false;
            }
            if (item.getLocation() != null) {
                if (!first) {
                    w.or();
                }
                w.like(BwcLostItem::getLocation, item.getLocation());
                first = false;
            }
            if (item.getName() != null) {
                if (!first) {
                    w.or();
                }
                w.like(BwcLostItem::getName, item.getName());
            }
        });
        wrapper.last("limit 10");
        return itemMapper.selectList(wrapper);
    }

    @Transactional
    public BwcLostNotice publish(Long itemId, String title, String content) {
        BwcLostItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw BizException.notFound("失物不存在");
        }
        BwcLostNotice notice = new BwcLostNotice();
        notice.setItemId(itemId);
        notice.setTitle(title == null ? "失物招领：" + item.getName() : title);
        notice.setContent(content == null ? "请失主携带有效证件到" + item.getStorageLocation() + "认领。" : content);
        notice.setPublishTime(LocalDateTime.now());
        notice.setPublisher(AuthContext.username());
        notice.setStatus("PUBLISHED");
        notice.setViews(0);
        noticeMapper.insert(notice);
        auditService.record("lost", "PUBLISH", item.getItemNo(), "失物公告发布");
        return notice;
    }

    @Transactional
    public BwcLostClaim claim(BwcLostClaim claim) {
        BwcLostItem item = itemMapper.selectById(claim.getItemId());
        if (item == null) {
            throw BizException.notFound("失物不存在");
        }
        if ("CLAIMED".equals(item.getStatus()) || "DISPOSED".equals(item.getStatus())) {
            throw new BizException(400, "该物品已认领或已处置");
        }
        claim.setId(null);
        claim.setClaimTime(LocalDateTime.now());
        claim.setStatus("PENDING");
        claimMapper.insert(claim);
        notifyService.sendByTemplate(item.getTenantId(), null, "LOST", "失物认领申请",
                claim.getClaimant() + " 申请认领：" + item.getName(), item.getId());
        auditService.record("lost", "CLAIM", item.getItemNo(), "认领申请");
        return claim;
    }

    @Transactional
    public BwcLostClaim approveClaim(Long id, boolean approved, String remark) {
        BwcLostClaim claim = claimMapper.selectById(id);
        if (claim == null) {
            throw BizException.notFound("认领记录不存在");
        }
        if (!"PENDING".equals(claim.getStatus())) {
            throw new BizException(400, "该认领已处理");
        }
        claim.setStatus(approved ? "APPROVED" : "REJECTED");
        claim.setVerifyBy(AuthContext.username());
        claim.setRemark(remark);
        claimMapper.updateById(claim);
        if (approved) {
            BwcLostItem item = itemMapper.selectById(claim.getItemId());
            if (item != null) {
                item.setStatus("CLAIMED");
                itemMapper.updateById(item);
            }
        }
        auditService.record("lost", "CLAIM_APPROVE", String.valueOf(id),
                "认领审核：" + (approved ? "通过" : "驳回"));
        return claim;
    }

    @Transactional
    public BwcLostDisposal applyDisposal(Long itemId, String type, String reason) {
        BwcLostItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw BizException.notFound("失物不存在");
        }
        BwcLostDisposal disposal = new BwcLostDisposal();
        disposal.setItemId(itemId);
        disposal.setDisposalType(type == null ? "ARCHIVE" : type);
        disposal.setReason(reason);
        disposal.setApplyBy(AuthContext.username());
        disposal.setApplyTime(LocalDateTime.now());
        disposal.setStatus("PENDING");
        disposalMapper.insert(disposal);
        auditService.record("lost", "DISPOSAL_APPLY", item.getItemNo(), "处置申请");
        return disposal;
    }

    @Transactional
    public BwcLostDisposal approveDisposal(Long id, boolean approved, String remark) {
        BwcLostDisposal disposal = disposalMapper.selectById(id);
        if (disposal == null) {
            throw BizException.notFound("处置申请不存在");
        }
        disposal.setStatus(approved ? "APPROVED" : "REJECTED");
        disposal.setApproveBy(AuthContext.username());
        disposal.setApproveTime(LocalDateTime.now());
        disposalMapper.updateById(disposal);
        if (approved) {
            BwcLostItem item = itemMapper.selectById(disposal.getItemId());
            if (item != null) {
                item.setStatus("DESTROY".equals(disposal.getDisposalType()) ? "DISPOSED" : "ARCHIVED");
                itemMapper.updateById(item);
            }
        }
        auditService.record("lost", "DISPOSAL_APPROVE", String.valueOf(id),
                "处置审批：" + (approved ? "通过" : "驳回"));
        return disposal;
    }

    public IPage<BwcLostItem> itemPage(long page, long size, String status, String keyword) {
        LambdaQueryWrapper<BwcLostItem> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcLostItem::getStatus, status);
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(BwcLostItem::getName, keyword)
                    .or().like(BwcLostItem::getLocation, keyword)
                    .or().like(BwcLostItem::getItemNo, keyword));
        }
        wrapper.orderByDesc(BwcLostItem::getId);
        return itemMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> stats() {
        long total = itemMapper.selectCount(null);
        long claimed = itemMapper.selectCount(new LambdaQueryWrapper<BwcLostItem>()
                .eq(BwcLostItem::getStatus, "CLAIMED"));
        long published = itemMapper.selectCount(new LambdaQueryWrapper<BwcLostItem>()
                .eq(BwcLostItem::getStatus, "PUBLISHED"));
        long disposed = itemMapper.selectCount(new LambdaQueryWrapper<BwcLostItem>()
                .in(BwcLostItem::getStatus, List.of("DISPOSED", "ARCHIVED")));
        Map<String, Object> result = new HashMap<>();
        result.put("total", total);
        result.put("claimed", claimed);
        result.put("published", published);
        result.put("disposed", disposed);
        result.put("claimRate", total == 0 ? 0 : Math.round(claimed * 1000.0 / total) / 10.0);
        return result;
    }
}
