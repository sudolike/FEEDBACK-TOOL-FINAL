package com.cen.service;

import com.cen.entity.CourseQuestionnaire;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cen.entity.Questionnaires;
import com.cen.controller.dto.QuestionnaireWithStatusDTO;
import com.cen.controller.dto.QuestionnaireResponseDTO;
import com.cen.controller.dto.QuestionnaireFullInfoDTO;
import com.cen.controller.dto.QuestionnaireSubmissionStatsDTO;
import com.cen.controller.dto.QuestionnaireResponseDetailDTO;

import java.util.List;
import java.util.Map;


public interface ICourseQuestionnaireService extends IService<CourseQuestionnaire> {
    List<QuestionnaireWithStatusDTO> getQuestionnaireByCourseId(Long courseId);
    
    boolean bindQuestionnaires(Long courseId, String questionnaireIds);
    
    boolean unbindQuestionnaire(Long courseId, Long questionnaireId);

    // 更新问卷状态
    boolean updateStatus(Long courseId, Long questionnaireId, Integer status);

    // 发布问卷
    boolean publishQuestionnaire(Long courseId, Long questionnaireId);
    
    // 结束问卷
    boolean completeQuestionnaire(Long courseId, Long questionnaireId);

    // 撤回问卷
    boolean recallQuestionnaire(Long courseId, Long questionnaireId);

    // 获取课程的进行中和已结束问卷，并标记是否已提交
    Map<String, List<QuestionnaireWithStatusDTO>> getQuestionnairesByStatus(Long courseId, Long studentId);

    // 获取学生的问卷答案
    QuestionnaireResponseDTO getStudentResponse(Long courseId, Long questionnaireId, Long studentId);

    // 获取学生的所有课程问卷
    List<QuestionnaireFullInfoDTO> getStudentQuestionnaires(Long studentId);

    // 获取课程问卷的填写统计信息
    List<QuestionnaireSubmissionStatsDTO> getQuestionnaireSubmissionStats(Long courseId);

    // 获取课程问卷的学生填写情况
    List<QuestionnaireResponseDetailDTO> getQuestionnaireResponses(Long courseId, Long questionnaireId);
}
