package com.cen.service;

import com.cen.controller.dto.QuestionnaireResponseDetailDTO;
import com.cen.entity.QuestionnaireResponses;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cen.controller.dto.QuestionnaireResponseSummaryDTO;

import java.util.List;

public interface IQuestionnaireResponsesService extends IService<QuestionnaireResponses> {
    // 获取课程问卷的答案汇总
    QuestionnaireResponseSummaryDTO getQuestionnaireResponseSummary(Long courseId, Long questionnaireId);

    List<QuestionnaireResponseDetailDTO> getQuestionnaireFillinDetails(Long courseId, Long questionnaireId);
}
