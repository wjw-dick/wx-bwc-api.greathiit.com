package com.greathiit.bwc.module.system.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.greathiit.bwc.module.fire.entity.BwcFireFacility;
import com.greathiit.bwc.module.fire.entity.BwcHazard;
import com.greathiit.bwc.module.fire.mapper.BwcFireFacilityMapper;
import com.greathiit.bwc.module.fire.mapper.BwcHazardMapper;
import com.greathiit.bwc.module.system.entity.SysTenant;
import com.greathiit.bwc.module.system.mapper.SysTenantMapper;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.module.system.service.ParamService;
import com.greathiit.bwc.module.vehicle.entity.BwcPlateAuthRecord;
import com.greathiit.bwc.module.vehicle.entity.BwcVehicle;
import com.greathiit.bwc.module.vehicle.mapper.BwcPlateAuthRecordMapper;
import com.greathiit.bwc.module.vehicle.mapper.BwcVehicleMapper;
import com.greathiit.bwc.module.visitor.entity.BwcVisitorAppointment;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitorAppointmentMapper;
import com.greathiit.bwc.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时任务：通行码过期、临时车牌到期、消防设施到期、隐患逾期督办、车辆有效期、租户到期。
 * 按租户逐个设置 TenantContext，确保租户插件生效且不跨租户更新。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiryJob {

    private final SysTenantMapper tenantMapper;
    private final BwcVisitorAppointmentMapper visitorAppointmentMapper;
    private final BwcPlateAuthRecordMapper plateAuthRecordMapper;
    private final BwcVehicleMapper vehicleMapper;
    private final BwcFireFacilityMapper fireFacilityMapper;
    private final BwcHazardMapper hazardMapper;
    private final NotifyService notifyService;
    private final ParamService paramService;

    /** 每 5 分钟：通行码过期 + 临时车牌到期。 */
    @Scheduled(cron = "0 */5 * * * ?")
    public void expirePassAndPlates() {
        forEachTenant("pass-and-plate", tenantId -> {
            List<BwcVisitorAppointment> passes = visitorAppointmentMapper.selectList(
                    new LambdaQueryWrapper<BwcVisitorAppointment>()
                            .eq(BwcVisitorAppointment::getStatus, "APPROVED")
                            .lt(BwcVisitorAppointment::getPassExpireTime, LocalDateTime.now()));
            for (BwcVisitorAppointment pass : passes) {
                pass.setStatus("EXPIRED");
                visitorAppointmentMapper.updateById(pass);
            }
            List<BwcPlateAuthRecord> plates = plateAuthRecordMapper.selectList(
                    new LambdaQueryWrapper<BwcPlateAuthRecord>()
                            .eq(BwcPlateAuthRecord::getPlateType, "TEMP")
                            .eq(BwcPlateAuthRecord::getStatus, "ACTIVE")
                            .lt(BwcPlateAuthRecord::getEndTime, LocalDateTime.now()));
            for (BwcPlateAuthRecord plate : plates) {
                plate.setStatus("EXPIRED");
                plateAuthRecordMapper.updateById(plate);
            }
            if (!passes.isEmpty() || !plates.isEmpty()) {
                log.info("租户 {} 过期任务：通行码 {} 条，临时车牌 {} 条", tenantId, passes.size(), plates.size());
            }
        });
    }

    /** 每天 02:10：消防设施到期、车辆有效期、隐患逾期督办。 */
    @Scheduled(cron = "0 10 2 * * ?")
    public void dailyExpiry() {
        forEachTenant("daily-expiry", tenantId -> {
            List<BwcFireFacility> facilities = fireFacilityMapper.selectList(
                    new LambdaQueryWrapper<BwcFireFacility>()
                            .lt(BwcFireFacility::getExpireDate, LocalDate.now())
                            .ne(BwcFireFacility::getStatus, "EXPIRED"));
            for (BwcFireFacility facility : facilities) {
                facility.setStatus("EXPIRED");
                fireFacilityMapper.updateById(facility);
                notifyService.sendByTemplate(tenantId, null, "EXPIRE", "消防设施到期提醒",
                        "设施 " + facility.getFacilityNo() + "（" + facility.getLocation() + "）已过期，请及时更换",
                        facility.getId());
            }
            List<BwcVehicle> vehicles = vehicleMapper.selectList(new LambdaQueryWrapper<BwcVehicle>()
                    .lt(BwcVehicle::getValidEnd, LocalDate.now())
                    .ne(BwcVehicle::getStatus, "EXPIRED"));
            for (BwcVehicle vehicle : vehicles) {
                vehicle.setStatus("EXPIRED");
                vehicleMapper.updateById(vehicle);
                notifyService.sendByTemplate(tenantId, null, "EXPIRE", "车辆备案到期",
                        "车牌 " + vehicle.getPlateNo() + " 备案已到期，请续期", vehicle.getId());
            }
            List<BwcHazard> hazads = hazardMapper.selectList(new LambdaQueryWrapper<BwcHazard>()
                    .lt(BwcHazard::getDeadline, LocalDateTime.now())
                    .in(BwcHazard::getStatus, List.of("ASSIGNED", "RECTIFYING", "REPORTED")));
            for (BwcHazard hazard : hazads) {
                hazard.setStatus("OVERDUE");
                hazardMapper.updateById(hazard);
                notifyService.sendByTemplate(tenantId, hazard.getAssignTo(), "HAZARD", "隐患整改逾期督办",
                        "隐患 " + hazard.getHazardNo() + " 已逾期，请立即整改", hazard.getId());
            }
            log.info("租户 {} 每日到期任务：设施 {}，车辆 {}，隐患 {}", tenantId,
                    facilities.size(), vehicles.size(), hazads.size());
        });
        expireTenants();
    }

    /** 租户到期策略：默认到期自动停用。 */
    public void expireTenants() {
        String policy = paramService.get("tenant_expire_policy", "disable");
        if (!"disable".equalsIgnoreCase(policy)) {
            return;
        }
        List<SysTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .in(SysTenant::getStatus, List.of("NORMAL", "TRIAL"))
                .lt(SysTenant::getExpireTime, LocalDateTime.now()));
        for (SysTenant tenant : tenants) {
            tenant.setStatus("DISABLED");
            tenantMapper.updateById(tenant);
            log.info("租户 {} 已到期自动停用", tenant.getCode());
        }
    }

    private void forEachTenant(String task, TenantTask action) {
        List<SysTenant> tenants = tenantMapper.selectList(new LambdaQueryWrapper<SysTenant>()
                .in(SysTenant::getStatus, List.of("NORMAL", "TRIAL")));
        for (SysTenant tenant : tenants) {
            TenantContext.set(tenant.getId(), false, false, false);
            try {
                action.run(tenant.getId());
            } catch (Exception e) {
                log.warn("租户 {} 定时任务 {} 执行失败: {}", tenant.getId(), task, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }

    @FunctionalInterface
    private interface TenantTask {
        void run(Long tenantId);
    }
}
