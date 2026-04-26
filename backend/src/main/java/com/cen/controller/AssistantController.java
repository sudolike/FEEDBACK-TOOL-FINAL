package com.cen.controller;

import com.cen.common.Result;
import com.cen.controller.dto.ChatRequestDTO;
import com.cen.service.IAssistantService;
import com.cen.service.IKnowledgeBaseService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 助手（含 RAG）
 *  - /assistant/chat        通用对话（学生浮窗、教师助手）
 *  - /assistant/recommend   选课推荐
 *  - /assistant/summarize   教师总结某门课的某份问卷
 *  - /assistant/kb/rebuild  重建知识库（管理员）
 */
@RestController
@RequestMapping("/assistant")
public class AssistantController {

    @Resource private IAssistantService assistantService;
    @Resource private IKnowledgeBaseService knowledgeBaseService;

    @PostMapping("/chat")
    public Result chat(@RequestBody ChatRequestDTO req) {
        return Result.success(assistantService.chat(req));
    }

    @PostMapping("/recommend")
    public Result recommend(@RequestBody Map<String, Object> body) {
        Long userId = body.get("userId") == null ? null : Long.valueOf(body.get("userId").toString());
        String prompt = body.get("prompt") == null ? null : body.get("prompt").toString();
        return Result.success(assistantService.recommendCourses(userId, prompt));
    }

    @GetMapping("/summarize")
    public Result summarize(@RequestParam Long courseId, @RequestParam Long questionnaireId) {
        Map<String, Object> ret = new HashMap<>();
        ret.put("summary", assistantService.summarizeQuestionnaire(courseId, questionnaireId));
        return Result.success(ret);
    }

    @PostMapping("/kb/rebuild")
    public Result rebuildKb() {
        int n = knowledgeBaseService.rebuildAll();
        Map<String, Object> ret = new HashMap<>();
        ret.put("courses", n);
        return Result.success(ret);
    }
}
