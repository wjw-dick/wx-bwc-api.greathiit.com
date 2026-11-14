package com.greathiit.bwc.module.emergency.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.module.emergency.entity.BwcEmergencyEvent;
import com.greathiit.bwc.module.emergency.entity.BwcEmergencySupply;
import com.greathiit.bwc.module.emergency.entity.BwcSupplyRecord;
import com.greathiit.bwc.module.emergency.mapper.BwcEmergencyEventMapper;
import com.greathiit.bwc.module.emergency.mapper.BwcEmergencySupplyMapper;
import com.greathiit.bwc.module.emergency.mapper.BwcSupplyRecordMapper;
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
 * 应急管理：物资领用归还、突发事件上报与响应。
 */
@Service
@RequiredArgsConstructor
public class EmergencyService {

    private final BwcEmergencySupplyMapper supplyMapper;
    private final BwcSupplyRecordMapper recordMapper;
    private final BwcEmergencyEventMapper eventMapper;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public BwcSupplyRecord borrow(Long supplyId, int quantity, String userName, String remark) {
        BwcEmergencySupply supply = supplyMapper.selectById(supplyId);
        if (supply == null) {
            throw BizException.notFound("物资不存在");
        }
        if (supply.getQuantity() == null || supply.getQuantity() < quantity) {
            throw new BizException(400, "库存不足");
        }
        supply.setQuantity(supply.getQuantity() - quantity);
        supplyMapper.updateById(supply);
        BwcSupplyRecord record = new BwcSupplyRecord();
        record.setSupplyId(supplyId);
        record.setType("BORROW");
        record.setQuantity(quantity);
        record.setUserName(userName == null ? AuthContext.username() : userName);
        record.setRecordTime(LocalDateTime.now());
        record.setOperator(AuthContext.username());
        record.setRemark(remark);
        recordMapper.insert(record);
        auditService.record("emergency", "BORROW", supply.getName(), "领用数量：" + quantity);
        return record;
    }

    @Transactional
    public BwcSupplyRecord giveBack(Long supplyId, int quantity, String userName, String remark) {
        BwcEmergencySupply supply = supplyMapper.selectById(supplyId);
        if (supply == null) {
            throw BizException.notFound("物资不存在");
        }
        supply.setQuantity((supply.getQuantity() == null ? 0 : supply.getQuantity()) + quantity);
        supplyMapper.updateById(supply);
        BwcSupplyRecord record = new BwcSupplyRecord();
        record.setSupplyId(supplyId);
        record.setType("RETURN");
        record.setQuantity(quantity);
        record.setUserName(userName == null ? AuthContext.username() : userName);
        record.setRecordTime(LocalDateTime.now());
        record.setOperator(AuthContext.username());
        record.setRemark(remark);
        recordMapper.insert(record);
        auditService.record("emergency", "RETURN", supply.getName(), "归还数量：" + quantity);
        return record;
    }

    @Transactional
    public BwcEmergencyEvent report(BwcEmergencyEvent event) {
        event.setId(null);
        event.setOccurTime(event.getOccurTime() == null ? LocalDateTime.now() : event.getOccurTime());
        event.setStatus("REPORTED");
        event.setReporterId(AuthContext.userId());
        if (event.getReporter() == null) {
            event.setReporter(AuthContext.username());
        }
        eventMapper.insert(event);
        notifyService.sendByTemplate(event.getTenantId(), null, "WARNING", "突发事件上报",
                event.getTitle() + " - " + event.getLocation(), event.getId());
        auditService.record("emergency", "EVENT", String.valueOf(event.getId()), "突发事件上报");
        return event;
    }

    @Transactional
    public BwcEmergencyEvent respond(Long id, String response) {
        BwcEmergencyEvent event = eventMapper.selectById(id);
        if (event == null) {
            throw BizException.notFound("事件不存在");
        }
        event.setResponse(response);
        event.setStatus("RESPONDING");
        eventMapper.updateById(event);
        auditService.record("emergency", "RESPOND", String.valueOf(id), "应急响应");
        return event;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("supplyCount", supplyMapper.selectCount(null));
        result.put("lowStock", supplyMapper.selectCount(new LambdaQueryWrapper<BwcEmergencySupply>()
                .lt(BwcEmergencySupply::getQuantity, 5)));
        result.put("eventCount", eventMapper.selectCount(null));
        result.put("openEvents", eventMapper.selectCount(new LambdaQueryWrapper<BwcEmergencyEvent>()
                .ne(BwcEmergencyEvent::getStatus, "CLOSED")));
        return result;
    }
}
