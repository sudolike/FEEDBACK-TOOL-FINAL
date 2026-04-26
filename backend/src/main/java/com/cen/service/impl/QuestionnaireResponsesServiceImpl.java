package com.cen.service.impl;

import com.cen.entity.QuestionnaireResponses;
import com.cen.entity.Questionnaires;
import com.cen.mapper.QuestionnaireResponsesMapper;
import com.cen.service.IQuestionnaireResponsesService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.cen.controller.dto.AnonymousResponseDTO;
import com.cen.controller.dto.QuestionnaireResponseDetailDTO;
import com.cen.entity.User;
import com.cen.mapper.UserMapper;
import com.cen.utils.AnonymizeUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.controller.dto.QuestionnaireResponseSummaryDTO;
import com.cen.mapper.QuestionnairesMapper;
import com.cen.mapper.CoursesMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import javax.annotation.Resource;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;


@Service
public class QuestionnaireResponsesServiceImpl extends ServiceImpl<QuestionnaireResponsesMapper, QuestionnaireResponses> implements IQuestionnaireResponsesService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private QuestionnairesMapper questionnairesMapper;

    @Resource
    private CoursesMapper coursesMapper;

    @Override
    public List<QuestionnaireResponseDetailDTO> getQuestionnaireFillinDetails(Long courseId, Long questionnaireId) {
        // 1. 查询该课程问卷的所有答案记录
        QueryWrapper<QuestionnaireResponses> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                .eq("questionnaire_id", questionnaireId)
                .orderByDesc("submitted_at");  // 按提交时间倒序
        List<QuestionnaireResponses> responses = this.list(queryWrapper);

        if (responses.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 获取所有学生ID
        List<Long> studentIds = responses.stream()
                .map(QuestionnaireResponses::getStudentId)
                .distinct()
                .collect(Collectors.toList());

        // 3. 批量查询学生信息
        List<User> students = userMapper.selectBatchIds(studentIds);
        Map<Long, User> studentMap = students.stream()
                .collect(Collectors.toMap(User::getId, student -> student));

        // 4. 组装返回数据
        return responses.stream().map(response -> {
            QuestionnaireResponseDetailDTO dto = new QuestionnaireResponseDetailDTO();
            dto.setResponse(response);
            dto.setStudent(studentMap.get(response.getStudentId()));
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public QuestionnaireResponseSummaryDTO getQuestionnaireResponseSummary(Long courseId, Long questionnaireId) {
        // 1. 查询问卷基本信息
        Questionnaires questionnaire = questionnairesMapper.selectById(questionnaireId);
        if (questionnaire == null) {
            throw new IllegalArgumentException("问卷不存在");
        }

        // 2. 查询该课程问卷的所有答案记录
        QueryWrapper<QuestionnaireResponses> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                .eq("questionnaire_id", questionnaireId);
        List<QuestionnaireResponses> responses = this.list(queryWrapper);

        // 3. 获取题目总数（从问卷的questions字段解析）
        int totalQuestions = 0;
        try {
            // 假设questions是JSON数组格式
            JSONArray questions = JSON.parseArray(questionnaire.getQuestions());
            totalQuestions = questions.size();
        } catch (Exception e) {
            totalQuestions = 0;
        }

        // 4. 获取所有答案
        List<String> allAnswers = responses.stream()
                .map(QuestionnaireResponses::getAnswers)
                .collect(Collectors.toList());

        // 5. 计算完成率
        Long totalStudents = coursesMapper.getStudentCountByCourseId(courseId);
        double completionRate = totalStudents == 0 ? 0.0 :
                (double) responses.size() / totalStudents * 100;

        // 6. 组装返回数据
        QuestionnaireResponseSummaryDTO summary = new QuestionnaireResponseSummaryDTO();
        summary.setQuestionnaire(questionnaire);
        summary.setTotalQuestions(totalQuestions);
        summary.setTotalResponses(responses.size());
        summary.setCompletionRate(Math.round(completionRate * 100.0) / 100.0);  // 保留两位小数
        summary.setAnswers(allAnswers);

        return summary;
    }

    @Override
    public List<AnonymousResponseDTO> getQuestionnaireFillinAnonymous(Long courseId, Long questionnaireId) {
        QueryWrapper<QuestionnaireResponses> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId)
          .eq("questionnaire_id", questionnaireId)
          .orderByDesc("submitted_at");
        List<QuestionnaireResponses> list = this.list(qw);
        return list.stream().map(r -> {
            AnonymousResponseDTO dto = new AnonymousResponseDTO();
            dto.setId(r.getId());
            dto.setAnswers(r.getAnswers());
            dto.setSubmittedAt(r.getSubmittedAt());
            dto.setAnonymousId(AnonymizeUtils.anonymize(r.getStudentId(), questionnaireId));
            return dto;
        }).collect(Collectors.toList());
    }
}
