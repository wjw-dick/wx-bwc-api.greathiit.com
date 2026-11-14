package com.greathiit.bwc.module.system.controller;

import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.common.PdfUtil;
import com.greathiit.bwc.common.QrCodeUtil;
import com.greathiit.bwc.module.fire.entity.BwcHazard;
import com.greathiit.bwc.module.fire.mapper.BwcHazardMapper;
import com.greathiit.bwc.module.logistics.service.LogisticsService;
import com.greathiit.bwc.module.lost.entity.BwcLostItem;
import com.greathiit.bwc.module.lost.mapper.BwcLostItemMapper;
import com.greathiit.bwc.module.report.entity.BwcReportApply;
import com.greathiit.bwc.module.report.mapper.BwcReportApplyMapper;
import com.greathiit.bwc.module.visitor.entity.BwcVisitorAppointment;
import com.greathiit.bwc.module.visitor.mapper.BwcVisitorAppointmentMapper;
import com.greathiit.bwc.security.AccessGuard;
import com.greathiit.bwc.security.RequirePermission;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 打印 PDF：访客通行码、报备审批单、隐患整改通知单、失物标签、月报。
 */
@RestController
@RequestMapping("/api/print")
@RequiredArgsConstructor
public class PrintController {

    private final BwcVisitorAppointmentMapper visitorMapper;
    private final BwcReportApplyMapper reportMapper;
    private final BwcHazardMapper hazardMapper;
    private final BwcLostItemMapper lostItemMapper;
    private final LogisticsService logisticsService;
    private final AccessGuard accessGuard;

    @Value("${bwc.pdf-font:/usr/share/fonts/chinese/stsong.ttf}")
    private String pdfFont;

    @GetMapping("/visitor/{id}/pass")
    @RequirePermission("visitor:read")
    public void visitorPass(@PathVariable Long id, HttpServletResponse response) throws IOException {
        accessGuard.requireRead();
        BwcVisitorAppointment appointment = visitorMapper.selectById(id);
        if (appointment == null) {
            throw BizException.notFound("预约不存在");
        }
        List<String> lines = new ArrayList<>();
        lines.add("访客姓名：" + appointment.getVisitorName());
        lines.add("手机号：" + appointment.getPhoneMask());
        lines.add("身份证号：" + appointment.getIdCardMask());
        lines.add("来访事由：" + appointment.getReason());
        lines.add("被访人：" + appointment.getVisitUserName());
        lines.add("来访时间：" + appointment.getVisitTime());
        lines.add("通行码：" + appointment.getPassCode());
        lines.add("有效期至：" + appointment.getPassExpireTime());
        lines.add("提示：请在校门闸机扫码或出示本页，配合门卫核验。");
        byte[] qr = appointment.getPassCode() == null ? null
                : QrCodeUtil.png(appointment.getPassCode(), 320);
        write(response, "访客通行码-" + id, PdfUtil.generate("访客临时通行码", lines, qr, pdfFont));
    }

    @GetMapping("/report/{id}")
    @RequirePermission("report:read")
    public void report(@PathVariable Long id, HttpServletResponse response) throws IOException {
        accessGuard.requireRead();
        BwcReportApply apply = reportMapper.selectById(id);
        if (apply == null) {
            throw BizException.notFound("报备不存在");
        }
        List<String> lines = new ArrayList<>();
        lines.add("报备编号：" + apply.getReportNo());
        lines.add("报备类型：" + apply.getType());
        lines.add("报备标题：" + apply.getTitle());
        lines.add("申请人：" + apply.getApplicant() + " / " + apply.getApplicantPhone());
        lines.add("活动地点：" + apply.getLocation());
        lines.add("起止时间：" + apply.getStartTime() + " ~ " + apply.getEndTime());
        lines.add("安全方案：" + apply.getSafetyPlan());
        lines.add("审批状态：" + apply.getStatus());
        lines.add("审批人：" + apply.getApprover() + "  " + apply.getApproveTime());
        lines.add("审批意见：" + apply.getApproveRemark());
        lines.add("电子回执号：" + apply.getReceiptNo());
        write(response, "报备审批单-" + apply.getReportNo(),
                PdfUtil.generate("校园安全报备审批单", lines, pdfFont));
    }

    @GetMapping("/hazard/{id}")
    @RequirePermission("hazard:read")
    public void hazard(@PathVariable Long id, HttpServletResponse response) throws IOException {
        accessGuard.requireRead();
        BwcHazard hazard = hazardMapper.selectById(id);
        if (hazard == null) {
            throw BizException.notFound("隐患不存在");
        }
        List<String> lines = new ArrayList<>();
        lines.add("隐患编号：" + hazard.getHazardNo());
        lines.add("隐患等级：" + hazard.getHazardLevel());
        lines.add("隐患位置：" + hazard.getLocation());
        lines.add("隐患描述：" + hazard.getDescription());
        lines.add("上报人：" + hazard.getReporter() + "  " + hazard.getReportTime());
        lines.add("责任人：" + hazard.getAssignToName());
        lines.add("整改期限：" + hazard.getDeadline());
        lines.add("整改方案：" + hazard.getRectifyPlan());
        lines.add("当前状态：" + hazard.getStatus());
        lines.add("要求：请责任人按期限完成整改，并提交整改前后照片，保卫处验收销号。");
        write(response, "隐患整改通知单-" + hazard.getHazardNo(),
                PdfUtil.generate("隐患整改通知单", lines, pdfFont));
    }

    @GetMapping("/lost/{id}/label")
    @RequirePermission("lost:read")
    public void lostLabel(@PathVariable Long id, HttpServletResponse response) throws IOException {
        accessGuard.requireRead();
        BwcLostItem item = lostItemMapper.selectById(id);
        if (item == null) {
            throw BizException.notFound("失物不存在");
        }
        List<String> lines = new ArrayList<>();
        lines.add("失物编号：" + item.getItemNo());
        lines.add("物品名称：" + item.getName());
        lines.add("分类：" + item.getCategory());
        lines.add("拾获地点：" + item.getLocation());
        lines.add("拾获时间：" + item.getFoundTime());
        lines.add("保管位置：" + item.getStorageLocation());
        lines.add("拾获人：" + item.getFinder());
        lines.add("认领提示：请携带有效证件到保卫处失物招领点认领。");
        byte[] qr = QrCodeUtil.png(item.getItemNo(), 320);
        write(response, "失物标签-" + item.getItemNo(),
                PdfUtil.generate("失物招领标签", lines, qr, pdfFont));
    }

    @GetMapping("/monthly/{month}")
    @RequirePermission("report:read")
    public void monthly(@PathVariable String month, HttpServletResponse response) throws IOException {
        accessGuard.requireRead();
        write(response, "校园保卫月报-" + month, logisticsService.monthlyPdf(month));
    }

    private void write(HttpServletResponse response, String fileName, byte[] pdf) throws IOException {
        response.setContentType("application/pdf");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded + ".pdf");
        response.getOutputStream().write(pdf);
    }
}
