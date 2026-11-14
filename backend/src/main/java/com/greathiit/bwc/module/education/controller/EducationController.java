package com.greathiit.bwc.module.education.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.R;
import com.greathiit.bwc.module.education.entity.BwcExam;
import com.greathiit.bwc.module.education.entity.BwcExamRecord;
import com.greathiit.bwc.module.education.entity.BwcSafetyCourse;
import com.greathiit.bwc.module.education.entity.BwcWarning;
import com.greathiit.bwc.module.education.mapper.BwcExamMapper;
import com.greathiit.bwc.module.education.mapper.BwcExamRecordMapper;
import com.greathiit.bwc.module.education.mapper.BwcSafetyCourseMapper;
import com.greathiit.bwc.module.education.service.EducationService;
import com.greathiit.bwc.security.AccessGuard;
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
    private final BwcSafetyCourseMapper courseMapper;
    private final AccessGuard accessGuard;

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
        IPage<BwcExam> result = examMapper.selectPage(new Page<>(page, size), wrapper);
        if (accessGuard.isSelfRole()) {
            result.getRecords().forEach(this::sanitizeExam);
        }
        return R.ok(result);
    }

    /** C 端考试详情：不返回标准答案 */
    @GetMapping("/exams/{id}")
    @RequirePermission("education:read")
    public R<BwcExam> examDetail(@PathVariable Long id) {
        BwcExam exam = examMapper.selectById(id);
        if (exam == null) {
            throw com.greathiit.bwc.common.BizException.notFound("考试不存在");
        }
        if (accessGuard.isSelfRole()) {
            if (!"PUBLISHED".equals(exam.getStatus())) {
                throw com.greathiit.bwc.common.BizException.notFound("考试不存在");
            }
            sanitizeExam(exam);
        }
        return R.ok(exam);
    }

    /** C 端课程列表：仅返回已发布课程 */
    @GetMapping("/courses")
    @RequirePermission("education:read")
    public R<IPage<BwcSafetyCourse>> courses(@RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size,
                                             @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<BwcSafetyCourse> wrapper = new LambdaQueryWrapper<>();
        if (accessGuard.isSelfRole()) {
            wrapper.eq(BwcSafetyCourse::getStatus, "PUBLISHED");
        }
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(BwcSafetyCourse::getTitle, keyword);
        }
        wrapper.orderByDesc(BwcSafetyCourse::getId);
        return R.ok(courseMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @GetMapping("/courses/{id}")
    @RequirePermission("education:read")
    public R<BwcSafetyCourse> courseDetail(@PathVariable Long id) {
        BwcSafetyCourse course = courseMapper.selectById(id);
        if (course == null || (accessGuard.isSelfRole() && !"PUBLISHED".equals(course.getStatus()))) {
            throw com.greathiit.bwc.common.BizException.notFound("课程不存在");
        }
        return R.ok(course);
    }

    private void sanitizeExam(BwcExam exam) {
        if (exam.getQuestionsJson() == null || exam.getQuestionsJson().isBlank()) {
            return;
        }
        try {
            cn.hutool.json.JSONArray questions = cn.hutool.json.JSONUtil.parseArray(exam.getQuestionsJson());
            questions.forEach(item -> {
                if (item instanceof cn.hutool.json.JSONObject obj) {
                    obj.remove("answer");
                }
            });
            exam.setQuestionsJson(questions.toString());
        } catch (Exception ignored) {
            exam.setQuestionsJson("[]");
        }
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
        Long userId = accessGuard.isSelfRole() ? AuthContext.userId() : null;
        if (userId == null) {
            accessGuard.requireRead();
        }
        return R.ok(educationService.records(page, size, examId, userId));
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
        accessGuard.requireManager();
        return R.ok(educationService.publishWarning(warning));
    }

    @GetMapping("/stats")
    @RequirePermission("education:read")
    public R<Map<String, Object>> stats() {
        accessGuard.requireRead();
        return R.ok(educationService.stats());
    }
}
