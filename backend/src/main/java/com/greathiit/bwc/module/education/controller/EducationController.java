package com.greathiit.bwc.module.education.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.education.entity.BwcExam;
import com.greathiit.bwc.module.education.entity.BwcExamRecord;
import com.greathiit.bwc.module.education.entity.BwcWarning;
import com.greathiit.bwc.module.education.mapper.BwcExamMapper;
import com.greathiit.bwc.module.education.mapper.BwcExamRecordMapper;
import com.greathiit.bwc.module.education.service.EducationService;
import com.greathiit.bwc.security.AuthContext;
import com.greathiit.bwc.security.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 安全教育：课程学习、考试提交与判分、预警推送、成绩查询。
 */
@RestController
@RequestMapping("/api/education")
@RequiredArgsConstructor
public class EducationController {

    private final EducationService educationService;
    private final BwcExamMapper examMapper;
    private final BwcExamRecordMapper recordMapper;

    @GetMapping("/exams")
    @RequirePermission("education:read")
    public R<IPage<BwcExam>> exams(@RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "10") long size,
                                   @RequestParam(required = false) String status) {
        LambdaQueryWrapper<BwcExam> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(BwcExam::getStatus, status);
        }
        wrapper.orderByDesc(BwcExam::getId);
        return R.ok(examMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/courses/{courseId}/study")
    @RequirePermission("education:write")
    public R<Object> study(@PathVariable Long courseId, @RequestBody Map<String, Object> body) {
        Integer progress = body.get("progress") == null ? 100
                : Integer.valueOf(String.valueOf(body.get("progress")));
        return R.ok(educationService.study(courseId, progress));
    }

    @PostMapping("/exams/{id}/submit")
    @RequirePermission("education:write")
    @SuppressWarnings("unchecked")
    public R<BwcExamRecord> submit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Map<String, String> answers = (Map<String, String>) body.get("answers");
        return R.ok(educationService.submitExam(id, answers));
    }

    @GetMapping("/records")
    @RequirePermission("education:read")
    public R<IPage<BwcExamRecord>> records(@RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long size,
                                           @RequestParam(required = false) Long examId) {
        return R.ok(educationService.records(page, size, examId));
    }

    @GetMapping("/my-records")
    public R<List<BwcExamRecord>> myRecords() {
        return R.ok(recordMapper.selectList(new LambdaQueryWrapper<BwcExamRecord>()
                .eq(BwcExamRecord::getUserId, AuthContext.userId())
                .orderByDesc(BwcExamRecord::getId)));
    }

    @PostMapping("/warnings")
    @RequirePermission("education:write")
    public R<BwcWarning> publishWarning(@RequestBody BwcWarning warning) {
        return R.ok(educationService.publishWarning(warning));
    }

    @GetMapping("/stats")
    @RequirePermission("education:read")
    public R<Map<String, Object>> stats() {
        return R.ok(educationService.stats());
    }
}
