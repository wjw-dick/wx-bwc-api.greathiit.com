package com.greathiit.bwc.module.logistics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.PdfUtil;
import com.greathiit.bwc.module.logistics.entity.BwcAttendance;
import com.greathiit.bwc.module.logistics.entity.BwcFeedback;
import com.greathiit.bwc.module.logistics.entity.BwcMonthlyReport;
import com.greathiit.bwc.module.logistics.entity.BwcPerformance;
import com.greathiit.bwc.module.logistics.entity.BwcSchedule;
import com.greathiit.bwc.module.logistics.mapper.BwcAttendanceMapper;
import com.greathiit.bwc.module.logistics.mapper.BwcFeedbackMapper;
import com.greathiit.bwc.module.logistics.mapper.BwcMonthlyReportMapper;
import com.greathiit.bwc.module.logistics.mapper.BwcPerformanceMapper;
import com.greathiit.bwc.module.logistics.mapper.BwcScheduleMapper;
import com.greathiit.bwc.module.system.service.AuditService;
import com.greathiit.bwc.module.system.service.NotifyService;
import com.greathiit.bwc.module.system.service.StatsService;
import com.greathiit.bwc.security.AuthContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 后勤：排班、考勤、绩效、投诉反馈、月报季报。
 */
@Service
@RequiredArgsConstructor
public class LogisticsService {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BwcScheduleMapper scheduleMapper;
    private final BwcAttendanceMapper attendanceMapper;
    private final BwcPerformanceMapper performanceMapper;
    private final BwcFeedbackMapper feedbackMapper;
    private final BwcMonthlyReportMapper monthlyReportMapper;
    private final StatsService statsService;
    private final NotifyService notifyService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${bwc.pdf-font:/usr/share/fonts/chinese/stsong.ttf}")
    private String pdfFont;

    public IPage<BwcSchedule> schedules(long page, long size, LocalDate start, LocalDate end, Long guardId) {
        LambdaQueryWrapper<BwcSchedule> wrapper = new LambdaQueryWrapper<>();
        if (start != null) {
            wrapper.ge(BwcSchedule::getShiftDate, start);
        }
        if (end != null) {
            wrapper.le(BwcSchedule::getShiftDate, end);
        }
        if (guardId != null) {
            wrapper.eq(BwcSchedule::getGuardId, guardId);
        }
        wrapper.orderByAsc(BwcSchedule::getShiftDate).orderByAsc(BwcSchedule::getStartTime);
        return scheduleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public BwcSchedule saveSchedule(BwcSchedule schedule, Long id) {
        if (id == null && schedule.getShiftDate() == null) {
            throw new BizException(400, "排班日期不能为空");
        }
        if (id != null) {
            BwcSchedule existing = scheduleMapper.selectById(id);
            if (existing == null) {
                throw BizException.notFound("排班不存在");
            }
            if (schedule.getShiftDate() == null) {
                schedule.setShiftDate(existing.getShiftDate());
            }
            if (schedule.getShiftType() == null) {
                schedule.setShiftType(existing.getShiftType());
            }
        }
        schedule.setId(id);
        if (id == null) {
            scheduleMapper.insert(schedule);
        } else {
            scheduleMapper.updateById(schedule);
        }
        auditService.record("logistics", id == null ? "SCHEDULE_ADD" : "SCHEDULE_EDIT",
                String.valueOf(schedule.getId()), "保安排班");
        return scheduleMapper.selectById(schedule.getId());
    }

    @Transactional
    public void deleteSchedule(Long id) {
        scheduleMapper.deleteById(id);
        auditService.record("logistics", "SCHEDULE_DELETE", String.valueOf(id), "删除排班");
    }

    public IPage<BwcAttendance> attendance(long page, long size, Long guardId, LocalDate date) {
        LambdaQueryWrapper<BwcAttendance> wrapper = new LambdaQueryWrapper<>();
        if (guardId != null) {
            wrapper.eq(BwcAttendance::getGuardId, guardId);
        }
        if (date != null) {
            wrapper.between(BwcAttendance::getCheckTime, date.atStartOfDay(), date.atTime(23, 59, 59));
        }
        wrapper.orderByDesc(BwcAttendance::getId);
        return attendanceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public BwcAttendance checkIn(Long guardId, String guardName, String type, String method, String status) {
        BwcAttendance attendance = new BwcAttendance();
        attendance.setGuardId(guardId == null ? AuthContext.userId() : guardId);
        attendance.setGuardName(guardName == null ? AuthContext.username() : guardName);
        attendance.setCheckTime(LocalDateTime.now());
        attendance.setType(type == null ? "ON" : type);
        attendance.setMethod(method == null ? "APP" : method);
        attendance.setStatus(status == null ? "NORMAL" : status);
        attendanceMapper.insert(attendance);
        auditService.record("logistics", "ATTENDANCE", String.valueOf(attendance.getId()), "考勤打卡：" + attendance.getType());
        return attendance;
    }

    public IPage<BwcPerformance> performance(long page, long size, String month) {
        LambdaQueryWrapper<BwcPerformance> wrapper = new LambdaQueryWrapper<>();
        if (month != null && !month.isBlank()) {
            wrapper.eq(BwcPerformance::getMonth, month);
        }
        wrapper.orderByDesc(BwcPerformance::getId);
        return performanceMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Transactional
    public BwcPerformance savePerformance(BwcPerformance performance) {
        if (performance.getMonth() == null || performance.getMonth().isBlank()) {
            performance.setMonth(YearMonth.now().format(MONTH));
        }
        if (performance.getId() == null) {
            performanceMapper.insert(performance);
        } else {
            performanceMapper.updateById(performance);
        }
        auditService.record("logistics", "PERFORMANCE", String.valueOf(performance.getId()),
                "绩效登记：" + performance.getScore());
        return performanceMapper.selectById(performance.getId());
    }

    @Transactional
    public BwcFeedback submitFeedback(BwcFeedback feedback) {
        feedback.setId(null);
        feedback.setSubmitTime(feedback.getSubmitTime() == null ? LocalDateTime.now() : feedback.getSubmitTime());
        feedback.setStatus("PENDING");
        if (feedback.getType() == null) {
            feedback.setType("SUGGESTION");
        }
        feedbackMapper.insert(feedback);
        notifyService.sendByTemplate(feedback.getTenantId(), null, "TODO", "新投诉/建议",
                feedback.getTitle(), feedback.getId());
        auditService.record("logistics", "FEEDBACK_SUBMIT", String.valueOf(feedback.getId()), "投诉建议提交");
        return feedback;
    }

    @Transactional
    public BwcFeedback handleFeedback(Long id, String status, String result) {
        BwcFeedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw BizException.notFound("反馈不存在");
        }
        feedback.setStatus(status == null ? "DONE" : status);
        feedback.setResult(result);
        feedback.setHandler(AuthContext.username());
        feedback.setHandleTime(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
        auditService.record("logistics", "FEEDBACK_HANDLE", String.valueOf(id), "反馈处理");
        return feedback;
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scheduleToday", scheduleMapper.selectCount(new LambdaQueryWrapper<BwcSchedule>()
                .eq(BwcSchedule::getShiftDate, LocalDate.now())));
        result.put("attendanceToday", attendanceMapper.selectCount(new LambdaQueryWrapper<BwcAttendance>()
                .ge(BwcAttendance::getCheckTime, LocalDate.now().atStartOfDay())));
        result.put("performanceMonth", performanceMapper.selectCount(new LambdaQueryWrapper<BwcPerformance>()
                .eq(BwcPerformance::getMonth, YearMonth.now().format(MONTH))));
        result.put("feedbackPending", feedbackMapper.selectCount(new LambdaQueryWrapper<BwcFeedback>()
                .eq(BwcFeedback::getStatus, "PENDING")));
        result.put("feedbackTotal", feedbackMapper.selectCount(null));
        return result;
    }

    /**
     * 生成月报（治安/消防/访客等统计），同租户同月幂等覆盖。
     */
    @Transactional
    public BwcMonthlyReport generateMonthly(String month) {
        String target = month == null || month.isBlank() ? YearMonth.now().format(MONTH) : month;
        YearMonth ym = YearMonth.parse(target, MONTH);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("month", target);
        content.put("overview", statsService.tenantOverview());
        content.put("visitor", statsService.moduleStats("visitor", start, end));
        content.put("vehicle", statsService.moduleStats("vehicle", start, end));
        content.put("event", statsService.moduleStats("event", start, end));
        content.put("fire", statsService.moduleStats("fire", start, end));
        content.put("patrol", statsService.moduleStats("patrol", start, end));
        content.put("lost", statsService.moduleStats("lost", start, end));
        content.put("report", statsService.moduleStats("report", start, end));
        BwcMonthlyReport report = monthlyReportMapper.selectOne(
                new LambdaQueryWrapper<BwcMonthlyReport>().eq(BwcMonthlyReport::getMonth, target));
        if (report == null) {
            report = new BwcMonthlyReport();
            report.setMonth(target);
            report.setContentJson(toJson(content));
            monthlyReportMapper.insert(report);
        } else {
            report.setContentJson(toJson(content));
            monthlyReportMapper.updateById(report);
        }
        auditService.record("logistics", "MONTHLY_REPORT", target, "生成月报");
        return report;
    }

    public List<BwcMonthlyReport> monthlyReports() {
        return monthlyReportMapper.selectList(new LambdaQueryWrapper<BwcMonthlyReport>()
                .orderByDesc(BwcMonthlyReport::getMonth));
    }

    public byte[] monthlyPdf(String month) {
        String target = month == null || month.isBlank() ? YearMonth.now().format(MONTH) : month;
        BwcMonthlyReport report = monthlyReportMapper.selectOne(
                new LambdaQueryWrapper<BwcMonthlyReport>().eq(BwcMonthlyReport::getMonth, target));
        if (report == null) {
            report = generateMonthly(target);
        }
        List<String> lines = new ArrayList<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> content = objectMapper.readValue(report.getContentJson(), Map.class);
            lines.add("报表月份：" + target);
            Object overview = content.get("overview");
            if (overview instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    String key = String.valueOf(e.getKey());
                    if ("generatedAt".equals(key) || "tenantId".equals(key)) {
                        continue;
                    }
                    lines.add(label(key) + "：" + e.getValue());
                }
            }
            lines.add("");
            lines.add("说明：本报表由校园保卫安防管理系统自动生成，数据来源为各业务模块实时台账。");
        } catch (Exception e) {
            lines.add("报表内容解析失败：" + e.getMessage());
        }
        try {
            return PdfUtil.generate("校园保卫月报（" + target + "）", lines, pdfFont);
        } catch (Exception e) {
            throw new BizException(500, "PDF 生成失败：" + e.getMessage());
        }
    }

    private String label(String key) {
        return switch (key) {
            case "visitorTotal" -> "访客预约总量";
            case "visitorToday" -> "今日访客";
            case "visitorPending" -> "待审批访客";
            case "vehicleTotal" -> "备案车辆";
            case "vehicleLogToday" -> "今日车辆进出";
            case "plateLogToday" -> "今日车牌识别";
            case "inPark" -> "在场车辆";
            case "violationOpen" -> "未处理违停";
            case "eventTotal" -> "治安事件";
            case "eventOpen" -> "未结案事件";
            case "workOrderOpen" -> "未完成工单";
            case "hazardTotal" -> "隐患总数";
            case "hazardOpen" -> "未闭环隐患";
            case "hazardRate" -> "隐患整改率(%)";
            case "fireFacilityTotal" -> "消防设施";
            case "fireIntactRate" -> "消防设施完好率(%)";
            case "fireAlarmToday" -> "今日消防报警";
            case "patrolTaskTotal" -> "巡逻任务";
            case "patrolRate" -> "巡逻完成率(%)";
            case "patrolRecordToday" -> "今日巡逻打卡";
            case "lostTotal" -> "失物登记";
            case "lostOpen" -> "待认领失物";
            case "lostClaimRate" -> "失物认领率(%)";
            case "reportPending" -> "待审批报备";
            case "deviceTotal" -> "安防设备";
            case "deviceRepairOpen" -> "未完成报修";
            case "emergencyOpen" -> "未闭环应急事件";
            case "feedbackPending" -> "待处理反馈";
            default -> key;
        };
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }
}
