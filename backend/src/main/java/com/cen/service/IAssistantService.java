package com.cen.service;

import com.cen.controller.dto.ChatRequestDTO;
import com.cen.controller.dto.ChatResponseDTO;

public interface IAssistantService {

    /** 学生 / 教师通用的对话入口（含 RAG） */
    ChatResponseDTO chat(ChatRequestDTO req);

    /** 教师：选课/排课助手（基于课程库） */
    ChatResponseDTO recommendCourses(Long userId, String prompt);

    /** 教师：基于一组问卷的统计文本归纳 */
    String summarizeQuestionnaire(Long courseId, Long questionnaireId);
}
