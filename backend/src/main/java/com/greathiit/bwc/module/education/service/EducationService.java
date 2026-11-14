package com.greathiit.bwc.module.education.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.greathiit.bwc.common.BizException;
import com.greathiit.bwc.module.education.entity.BwcCourseStudy;
import com.greathiit.bwc.module.education.entity.BwcExam;
import com.greathiit.bwc.module.education.entity.BwcExamRecord;
import com.greathiit.bwc.module.education.entity.BwcWarning;
import com.greathiit.bwc.module.education.mapper.BwcCourseStudyMapper;
import com.greathiit.bwc.module.education.mapper.BwcExamMapper;
import com.greathiit.bwc.module.education.mapper.BwcExamRecordMapper;
import com.greathiit.bwc.module.education.mapper.BwcWarningMapper;
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
 * 安全教育：课程学习、考试自动判分、预警推送。
 */
@Service
@RequiredArgsConstructor
public class EducationService {

    private final BwcCourseStudyMapper studyMapper;
    private final BwcExamMapper examMapper;
    private final BwcExamRecordMapper recordMapper;
    private final BwcWarningMapper warningMapper;
    private final NotifyService notifyService;
    private final AuditService auditService;

    @Transactional
    public BwcCourseStudy study(Long courseId, Integer progress) {
        Long userId = AuthContext.userId();
        BwcCourseStudy study = studyMapper.selectOne(new LambdaQueryWrapper<BwcCourseStudy>()
                .eq(BwcCourseStudy::getCourseId, courseId)
                .eq(BwcCourseStudy::getUserId, userId)
                .last("limit 1"));
        if (study == null) {
            study = new BwcCourseStudy();
            study.setCourseId(courseId);
            study.setUserId(userId);
            study.setUserName(AuthContext.get().getRealName());
        }
        study.setProgress(progress == null ? 0 : Math.min(progress, 100));
        study.setStudyTime(LocalDateTime.now());
        study.setFinishFlag(study.getProgress() >= 100 ? 1 : 0);
        if (study.getId() == null) {
            studyMapper.insert(study);
        } else {
            studyMapper.updateById(study);
        }
        return study;
    }

    @Transactional
    public BwcExamRecord submitExam(Long examId, Map<String, String> answers) {
        BwcExam exam = examMapper.selectById(examId);
        if (exam == null || !"PUBLISHED".equals(exam.getStatus())) {
            throw BizException.notFound("考试不存在或未发布");
        }
        LocalDateTime now = LocalDateTime.now();
        if (exam.getStartTime() != null && now.isBefore(exam.getStartTime())
                || exam.getEndTime() != null && now.isAfter(exam.getEndTime())) {
            throw new BizException(400, "不在考试时间范围内");
        }
        Long userId = AuthContext.userId();
        Long exists = recordMapper.selectCount(new LambdaQueryWrapper<BwcExamRecord>()
                .eq(BwcExamRecord::getExamId, examId)
                .eq(BwcExamRecord::getUserId, userId));
        if (exists != null && exists > 0) {
            throw new BizException(400, "您已提交过该考试，请勿重复提交");
        }
        int score = grade(exam.getQuestionsJson(), answers);
        BwcExamRecord record = new BwcExamRecord();
        record.setExamId(examId);
        record.setUserId(userId);
        record.setUserName(AuthContext.get().getRealName());
        record.setScore(score);
        record.setPassFlag(score >= exam.getPassScore() ? 1 : 0);
        record.setAnswersJson(JSONUtil.toJsonStr(answers));
        record.setSubmitTime(now);
        recordMapper.insert(record);
        auditService.record("education", "EXAM_SUBMIT", String.valueOf(examId),
                "考试成绩：" + score);
        return record;
    }

    private int grade(String questionsJson, Map<String, String> answers) {
        if (questionsJson == null || questionsJson.isBlank()) {
            return 0;
        }
        int score = 0;
        JSONArray questions = JSONUtil.parseArray(questionsJson);
        for (Object obj : questions) {
            JSONObject question = (JSONObject) obj;
            String id = question.getStr("id");
            String answer = question.getStr("answer");
            String userAnswer = answers == null ? null : answers.get(id);
            if (answer != null && answer.equalsIgnoreCase(userAnswer)) {
                score += question.getInt("score", 0);
            }
        }
        return score;
    }

    @Transactional
    public BwcWarning publishWarning(BwcWarning warning) {
        warning.setId(null);
        warning.setPublishTime(LocalDateTime.now());
        warning.setPublisher(AuthContext.username());
        warning.setStatus("PUBLISHED");
        warningMapper.insert(warning);
        notifyService.sendByTemplate(warning.getTenantId(), null, "WARNING", "安全预警：" + warning.getType(),
                warning.getTitle() + " - " + warning.getContent(), warning.getId());
        auditService.record("education", "WARNING", String.valueOf(warning.getId()), "预警推送");
        return warning;
    }

    public IPage<BwcExamRecord> records(long page, long size, Long examId, Long userId) {
        LambdaQueryWrapper<BwcExamRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(BwcExamRecord::getUserId, userId);
        }
        if (examId != null) {
            wrapper.eq(BwcExamRecord::getExamId, examId);
        }
        wrapper.orderByDesc(BwcExamRecord::getId);
        return recordMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        long examCount = examMapper.selectCount(null);
        List<BwcExamRecord> records = recordMapper.selectList(null);
        long passCount = records.stream().filter(r -> r.getPassFlag() != null && r.getPassFlag() == 1).count();
        double avg = records.isEmpty() ? 0
                : records.stream().mapToInt(BwcExamRecord::getScore).average().orElse(0);
        result.put("examCount", examCount);
        result.put("submitCount", records.size());
        result.put("passCount", passCount);
        result.put("passRate", records.isEmpty() ? 0
                : Math.round(passCount * 1000.0 / records.size()) / 10.0);
        result.put("avgScore", Math.round(avg * 10.0) / 10.0);
        return result;
    }
}
