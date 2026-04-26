package com.cen.service;

import com.cen.controller.dto.AnonymousResponseDTO;
import com.cen.controller.dto.QuestionnaireResponseDetailDTO;
import com.cen.entity.QuestionnaireResponses;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cen.controller.dto.QuestionnaireResponseSummaryDTO;

import java.util.List;

public interface IQuestionnaireResponsesService extends IService<QuestionnaireResponses> {
    // 获取课程问卷的答案汇总
    QuestionnaireResponseSummaryDTO getQuestionnaireResponseSummary(Long courseId, Long questionnaireId);

    List<QuestionnaireResponseDetailDTO> getQuestionnaireFillinDetails(Long courseId, Long questionnaireId);

    /** 匿名化版本：不返回学生姓名、头像、邮箱，仅返回 anonymousId。 */
    List<AnonymousResponseDTO> getQuestionnaireFillinAnonymous(Long courseId, Long questionnaireId);
}
