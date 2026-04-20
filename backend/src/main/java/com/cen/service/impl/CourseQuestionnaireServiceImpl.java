package com.cen.service.impl;

import com.cen.entity.CourseQuestionnaire;
import com.cen.mapper.CourseQuestionnaireMapper;
import com.cen.service.ICourseQuestionnaireService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import com.cen.entity.Questionnaires;
import com.cen.mapper.QuestionnairesMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import com.cen.controller.dto.QuestionnaireWithStatusDTO;
import com.cen.enums.QuestionnaireStatus;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import java.util.HashMap;
import java.util.Set;
import com.cen.entity.QuestionnaireResponses;
import com.cen.mapper.QuestionnaireResponsesMapper;
import com.cen.controller.dto.QuestionnaireResponseDTO;
import com.cen.entity.Courses;
import com.cen.mapper.CoursesMapper;
import com.cen.entity.User;
import com.cen.mapper.UserMapper;
import com.cen.controller.dto.QuestionnaireFullInfoDTO;
import java.util.Comparator;
import com.cen.controller.dto.QuestionnaireSubmissionStatsDTO;
import com.cen.entity.CourseStudents;
import com.cen.mapper.CourseStudentsMapper;
import com.cen.controller.dto.QuestionnaireResponseDetailDTO;

import javax.annotation.Resource;


@Service
public class CourseQuestionnaireServiceImpl extends ServiceImpl<CourseQuestionnaireMapper, CourseQuestionnaire> implements ICourseQuestionnaireService {

    @Resource
    private QuestionnairesMapper questionnairesMapper;
    
    @Resource
    private QuestionnaireResponsesMapper questionnaireResponsesMapper;

    @Resource
    private CoursesMapper coursesMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CourseStudentsMapper courseStudentsMapper;

    @Override
    public List<QuestionnaireWithStatusDTO> getQuestionnaireByCourseId(Long courseId) {
        // 1. 先查询关联表
        QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId);
        List<CourseQuestionnaire> courseQuestionnaires = this.list(queryWrapper);
        
        // 2. 如果没有关联问卷，返回空列表
        if (courseQuestionnaires.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 3. 获取所有问卷ID
        List<Long> questionnaireIds = courseQuestionnaires.stream()
                .map(CourseQuestionnaire::getQuestionnaireId)
                .collect(Collectors.toList());
                
        // 4. 查询问卷详情
        List<Questionnaires> questionnaires = questionnairesMapper.selectBatchIds(questionnaireIds);
        
        // 5. 创建问卷ID到问卷对象的映射
        Map<Long, Questionnaires> questionnaireMap = questionnaires.stream()
                .collect(Collectors.toMap(Questionnaires::getId, q -> q));
                
        // 6. 组装返回结果
        return courseQuestionnaires.stream()
                .map(cq -> {
                    QuestionnaireWithStatusDTO dto = new QuestionnaireWithStatusDTO();
                    dto.setQuestionnaire(questionnaireMap.get(cq.getQuestionnaireId()));
                    dto.setStatus(cq.getStatus());
                    // 获取状态描述
                    dto.setStatusDescription(Arrays.stream(QuestionnaireStatus.values())
                            .filter(status -> status.getCode() == cq.getStatus())
                            .findFirst()
                            .map(QuestionnaireStatus::getDescription)
                            .orElse("未知状态"));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean bindQuestionnaires(Long courseId, String questionnaireIds) {
        // // 1. 先删除该课程的所有关联
        // QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        // queryWrapper.eq("course_id", courseId);
        // this.remove(queryWrapper);
        
        // 2. 如果问卷ID为空，则直接返回true（相当于解除所有关联）
        if (StringUtils.isBlank(questionnaireIds)) {
            return true;
        }
        
        // 3. 将问卷ID字符串转换为List
        List<Long> qIds = Arrays.stream(questionnaireIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
                
        // 4. 批量创建新的关联关系，默认状态为待发布
        List<CourseQuestionnaire> relations = qIds.stream()
                .map(qId -> new CourseQuestionnaire(null, courseId, qId, QuestionnaireStatus.PENDING.getCode(), LocalDateTime.now()))
                .collect(Collectors.toList());
                
        // 5. 批量保存
        return this.saveBatch(relations);
    }
 
    @Override
    @Transactional
    public boolean unbindQuestionnaire(Long courseId, Long questionnaireId) {
        QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                   .eq("questionnaire_id", questionnaireId);
        return this.remove(queryWrapper);
    }

    @Override
    @Transactional
    public boolean updateStatus(Long courseId, Long questionnaireId, Integer status) {
        // 验证状态值是否有效
        boolean validStatus = Arrays.stream(QuestionnaireStatus.values())
                .anyMatch(s -> s.getCode() == status);
        if (!validStatus) {
            throw new IllegalArgumentException("Invalid status value");
        }

        // 更新状态
        UpdateWrapper<CourseQuestionnaire> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("course_id", courseId)
                    .eq("questionnaire_id", questionnaireId)
                    .set("status", status);
        
        return this.update(updateWrapper);
    }

    @Override
    @Transactional
    public boolean publishQuestionnaire(Long courseId, Long questionnaireId) {
        // 1. 检查当前状态是否为待发布
        QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                   .eq("questionnaire_id", questionnaireId);
        CourseQuestionnaire questionnaire = this.getOne(queryWrapper);
        
        if (questionnaire == null) {
            throw new IllegalArgumentException("问卷不存在");
        }
        
        if (questionnaire.getStatus() != QuestionnaireStatus.PENDING.getCode()) {
            throw new IllegalArgumentException("只有待发布状态的问卷可以发布");
        }
        
        // 2. 更新状态为进行中
        UpdateWrapper<CourseQuestionnaire> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("course_id", courseId)
                    .eq("questionnaire_id", questionnaireId)
                    .set("status", QuestionnaireStatus.IN_PROGRESS.getCode());
        
        return this.update(updateWrapper);
    }

    @Override
    @Transactional
    public boolean completeQuestionnaire(Long courseId, Long questionnaireId) {
        // 1. 检查当前状态是否为进行中
        QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                   .eq("questionnaire_id", questionnaireId);
        CourseQuestionnaire questionnaire = this.getOne(queryWrapper);
        
        if (questionnaire == null) {
            throw new IllegalArgumentException("问卷不存在");
        }
        
        if (questionnaire.getStatus() != QuestionnaireStatus.IN_PROGRESS.getCode()) {
            throw new IllegalArgumentException("只有进行中状态的问卷可以结束");
        }
        
        // 2. 更新状态为已完成
        UpdateWrapper<CourseQuestionnaire> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("course_id", courseId)
                    .eq("questionnaire_id", questionnaireId)
                    .set("status", QuestionnaireStatus.COMPLETED.getCode());
        
        return this.update(updateWrapper);
    }

    @Override
    @Transactional
    public boolean recallQuestionnaire(Long courseId, Long questionnaireId) {
        // 1. 检查当前状态是否为进行中
        QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                   .eq("questionnaire_id", questionnaireId);
        CourseQuestionnaire questionnaire = this.getOne(queryWrapper);
        
        if (questionnaire == null) {
            throw new IllegalArgumentException("问卷不存在");
        }
        
        if (questionnaire.getStatus() != QuestionnaireStatus.IN_PROGRESS.getCode()) {
            throw new IllegalArgumentException("只有进行中状态的问卷可以撤回");
        }
        
        // 2. 更新状态为待发布
        UpdateWrapper<CourseQuestionnaire> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("course_id", courseId)
                    .eq("questionnaire_id", questionnaireId)
                    .set("status", QuestionnaireStatus.PENDING.getCode());
        
        return this.update(updateWrapper);
    }

    @Override
    public Map<String, List<QuestionnaireWithStatusDTO>> getQuestionnairesByStatus(Long courseId, Long studentId) {
        // 1. 获取课程所有问卷
        List<QuestionnaireWithStatusDTO> allQuestionnaires = getQuestionnaireByCourseId(courseId);
        
        // 2. 查询学生的所有问卷提交记录
        QueryWrapper<QuestionnaireResponses> responseQuery = new QueryWrapper<>();
        responseQuery.eq("course_id", courseId)
                    .eq("student_id", studentId);
        List<QuestionnaireResponses> responses = questionnaireResponsesMapper.selectList(responseQuery);
        
        // 创建已提交问卷ID集合
        Set<Long> submittedQuestionnaireIds = responses.stream()
                .map(QuestionnaireResponses::getQuestionnaireId)
                .collect(Collectors.toSet());
        
        // 设置提交状态
        allQuestionnaires.forEach(dto -> {
            dto.setHasSubmitted(submittedQuestionnaireIds.contains(dto.getQuestionnaire().getId()));
        });
        
        // 3. 按状态分类
        Map<String, List<QuestionnaireWithStatusDTO>> result = new HashMap<>();
        
        // 进行中的问卷
        List<QuestionnaireWithStatusDTO> ongoingQuestionnaires = allQuestionnaires.stream()
                .filter(q -> q.getStatus() == QuestionnaireStatus.IN_PROGRESS.getCode())
                .collect(Collectors.toList());
        
        // 已结束的问卷
        List<QuestionnaireWithStatusDTO> completedQuestionnaires = allQuestionnaires.stream()
                .filter(q -> q.getStatus() == QuestionnaireStatus.COMPLETED.getCode())
                .collect(Collectors.toList());
        
        // 4. 放入结果Map
        result.put("ongoing", ongoingQuestionnaires);
        result.put("completed", completedQuestionnaires);
        
        return result;
    }

    @Override
    public QuestionnaireResponseDTO getStudentResponse(Long courseId, Long questionnaireId, Long studentId) {
        // 1. 查询问卷答案
        QueryWrapper<QuestionnaireResponses> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId)
                    .eq("questionnaire_id", questionnaireId)
                    .eq("student_id", studentId);
        
        QuestionnaireResponses response = questionnaireResponsesMapper.selectOne(queryWrapper);
        
        if (response == null) {
            return null;  // 学生未提交答案
        }
        
        // 2. 查询问卷信息
        Questionnaires questionnaire = questionnairesMapper.selectById(questionnaireId);
        if (questionnaire == null) {
            throw new IllegalArgumentException("问卷不存在");
        }
        
        // 3. 组装返回数据
        QuestionnaireResponseDTO dto = new QuestionnaireResponseDTO();
        dto.setId(response.getId());
        dto.setQuestionnaire(questionnaire);
        dto.setAnswers(response.getAnswers());
        dto.setSubmittedAt(response.getSubmittedAt());
        
        return dto;
    }

    @Override
    public List<QuestionnaireFullInfoDTO> getStudentQuestionnaires(Long studentId) {
        // 1. 获取学生所有课程
        List<Courses> studentCourses = coursesMapper.getCoursesByStudentId(studentId);
        if (studentCourses.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 获取所有课程ID
        List<Long> courseIds = studentCourses.stream()
                .map(Courses::getId)
                .collect(Collectors.toList());
                
        // 3. 查询这些课程的进行中和已结束的问卷
        QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("course_id", courseIds)
                .in("status", Arrays.asList(
                        QuestionnaireStatus.IN_PROGRESS.getCode(),
                        QuestionnaireStatus.COMPLETED.getCode()
                ));
        List<CourseQuestionnaire> courseQuestionnaires = this.list(queryWrapper);
        
        // 4. 获取所有问卷ID
        List<Long> questionnaireIds = courseQuestionnaires.stream()
                .map(CourseQuestionnaire::getQuestionnaireId)
                .collect(Collectors.toList());
                
        // 5. 查询问卷详情
        List<Questionnaires> questionnaires = questionnairesMapper.selectBatchIds(questionnaireIds);
        Map<Long, Questionnaires> questionnaireMap = questionnaires.stream()
                .collect(Collectors.toMap(Questionnaires::getId, q -> q));
                
        // 6. 创建课程ID到课程对象的映射
        Map<Long, Courses> courseMap = studentCourses.stream()
                .collect(Collectors.toMap(Courses::getId, c -> c));
                
        // 7. 获取所有教师ID
        Set<Long> teacherIds = studentCourses.stream()
                .map(Courses::getTeacherId)
                .collect(Collectors.toSet());
                
        // 8. 查询教师信息
        List<User> teachers = userMapper.selectBatchIds(teacherIds);
        Map<Long, User> teacherMap = teachers.stream()
                .collect(Collectors.toMap(User::getId, t -> t));
                
        // 9. 查询学生的问卷提交记录
        QueryWrapper<QuestionnaireResponses> responseQuery = new QueryWrapper<>();
        responseQuery.eq("student_id", studentId);
        List<QuestionnaireResponses> responses = questionnaireResponsesMapper.selectList(responseQuery);
        Set<Long> submittedQuestionnaireIds = responses.stream()
                .map(QuestionnaireResponses::getQuestionnaireId)
                .collect(Collectors.toSet());
                
        // 10. 组装返回数据
        return courseQuestionnaires.stream()
                .map(cq -> {
                    QuestionnaireFullInfoDTO dto = new QuestionnaireFullInfoDTO();
                    dto.setQuestionnaire(questionnaireMap.get(cq.getQuestionnaireId()));
                    dto.setCourse(courseMap.get(cq.getCourseId()));
                    dto.setTeacher(teacherMap.get(courseMap.get(cq.getCourseId()).getTeacherId()));
                    dto.setStatus(cq.getStatus());
                    dto.setStatusDescription(Arrays.stream(QuestionnaireStatus.values())
                            .filter(status -> status.getCode() == cq.getStatus())
                            .findFirst()
                            .map(QuestionnaireStatus::getDescription)
                            .orElse("未知状态"));
                    dto.setHasSubmitted(submittedQuestionnaireIds.contains(cq.getQuestionnaireId()));
                    dto.setCreatedAt(cq.getCreatedAt());
                    return dto;
                })
                .sorted(Comparator.comparing(QuestionnaireFullInfoDTO::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionnaireSubmissionStatsDTO> getQuestionnaireSubmissionStats(Long courseId) {
        // 1. 获取课程的所有问卷
        QueryWrapper<CourseQuestionnaire> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("course_id", courseId);
        List<CourseQuestionnaire> courseQuestionnaires = this.list(queryWrapper);
        
        if (courseQuestionnaires.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 获取所有问卷ID
        List<Long> questionnaireIds = courseQuestionnaires.stream()
                .map(CourseQuestionnaire::getQuestionnaireId)
                .collect(Collectors.toList());
                
        // 3. 查询问卷详情
        List<Questionnaires> questionnaires = questionnairesMapper.selectBatchIds(questionnaireIds);
        Map<Long, Questionnaires> questionnaireMap = questionnaires.stream()
                .collect(Collectors.toMap(Questionnaires::getId, q -> q));
                
        // 4. 获取该课程的总学生数
        Long totalStudents = coursesMapper.getStudentCountByCourseId(courseId);
        
        // 5. 查询每个问卷的提交记录数
        Map<Long, Long> submissionCountMap = new HashMap<>();
        for (Long questionnaireId : questionnaireIds) {
            QueryWrapper<QuestionnaireResponses> responseQuery = new QueryWrapper<>();
            responseQuery.eq("course_id", courseId)
                        .eq("questionnaire_id", questionnaireId);
            long count = questionnaireResponsesMapper.selectCount(responseQuery);
            submissionCountMap.put(questionnaireId, count);
        }
        
        // 6. 组装返回数据
        return courseQuestionnaires.stream()
                .map(cq -> {
                    QuestionnaireSubmissionStatsDTO dto = new QuestionnaireSubmissionStatsDTO();
                    dto.setQuestionnaire(questionnaireMap.get(cq.getQuestionnaireId()));
                    dto.setStatus(cq.getStatus());
                    dto.setStatusDescription(Arrays.stream(QuestionnaireStatus.values())
                            .filter(status -> status.getCode() == cq.getStatus())
                            .findFirst()
                            .map(QuestionnaireStatus::getDescription)
                            .orElse("未知状态"));
                    dto.setTotalStudents(totalStudents);
                    
                    Long submittedCount = submissionCountMap.get(cq.getQuestionnaireId());
                    dto.setSubmittedCount(submittedCount);
                    
                    // 计算提交率
                    double rate = totalStudents == 0 ? 0.0 : 
                        (double) submittedCount / totalStudents * 100;
                    dto.setSubmissionRate(Math.round(rate * 100.0) / 100.0);  // 保留两位小数
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<QuestionnaireResponseDetailDTO> getQuestionnaireResponses(Long courseId, Long questionnaireId) {
        // 1. 获取该课程的所有学生
        QueryWrapper<CourseStudents> studentQuery = new QueryWrapper<>();
        studentQuery.eq("course_id", courseId);
        List<CourseStudents> courseStudents = courseStudentsMapper.selectList(studentQuery);
        
        if (courseStudents.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 获取所有学生ID
        List<Long> studentIds = courseStudents.stream()
                .map(CourseStudents::getStudentId)
                .collect(Collectors.toList());
                
        // 3. 查询所有学生信息
        List<User> students = userMapper.selectBatchIds(studentIds);
        Map<Long, User> studentMap = students.stream()
                .collect(Collectors.toMap(User::getId, student -> student));
                
        // 4. 查询问卷答案
        QueryWrapper<QuestionnaireResponses> responseQuery = new QueryWrapper<>();
        responseQuery.eq("course_id", courseId)
                    .eq("questionnaire_id", questionnaireId);
        List<QuestionnaireResponses> responses = questionnaireResponsesMapper.selectList(responseQuery);
        
        // 创建学生ID到答案的映射
        Map<Long, QuestionnaireResponses> responseMap = responses.stream()
                .collect(Collectors.toMap(QuestionnaireResponses::getStudentId, response -> response));
                
        // 5. 组装返回数据
        return studentIds.stream().map(studentId -> {
            QuestionnaireResponseDetailDTO dto = new QuestionnaireResponseDetailDTO();
            dto.setStudent(studentMap.get(studentId));
            dto.setResponse(responseMap.get(studentId));  // 如果学生未提交，这里会是null
            return dto;
        }).collect(Collectors.toList());
    }
}
