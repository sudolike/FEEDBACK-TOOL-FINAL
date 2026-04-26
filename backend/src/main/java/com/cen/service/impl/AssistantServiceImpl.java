package com.cen.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.cen.controller.dto.ChatRequestDTO;
import com.cen.controller.dto.ChatResponseDTO;
import com.cen.entity.ChatMessage;
import com.cen.entity.KbChunk;
import com.cen.entity.QuestionnaireResponses;
import com.cen.entity.Questionnaires;
import com.cen.mapper.ChatMessageMapper;
import com.cen.mapper.QuestionnaireResponsesMapper;
import com.cen.mapper.QuestionnairesMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.service.IAssistantService;
import com.cen.service.IKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AssistantServiceImpl implements IAssistantService {

    @Value("${ai-api-key:}")
    private String apiKey;

    @Value("${ai.model:qwen-plus}")
    private String model;

    @Resource private Generation generation;
    @Resource private IKnowledgeBaseService knowledgeBaseService;
    @Resource private ChatMessageMapper chatMessageMapper;
    @Resource private QuestionnairesMapper questionnairesMapper;
    @Resource private QuestionnaireResponsesMapper questionnaireResponsesMapper;

    @Override
    public ChatResponseDTO chat(ChatRequestDTO req) {
        long t0 = System.currentTimeMillis();

        if (StrUtil.isBlank(req.getSessionId())) {
            req.setSessionId(UUID.randomUUID().toString());
        }

        // 1) 检索 RAG 上下文
        List<KbChunk> citations = knowledgeBaseService.search(req.getMessage(), req.getCourseId(), 5);

        // 2) 组装 messages：system / 历史 / 用户消息（带检索片段）
        List<Message> messages = new ArrayList<>();
        messages.add(systemMessage(req.getRole(), req.getScene()));

        if (req.getHistory() != null) {
            for (ChatRequestDTO.ChatTurn turn : req.getHistory()) {
                messages.add(Message.builder()
                        .role(safeRole(turn.getRole()))
                        .content(turn.getContent())
                        .build());
            }
        }

        StringBuilder userMsg = new StringBuilder();
        if (!citations.isEmpty()) {
            userMsg.append("【可参考的内部资料】\n");
            int idx = 1;
            for (KbChunk c : citations) {
                userMsg.append("[").append(idx++).append("] ")
                        .append(c.getTitle() == null ? "" : c.getTitle()).append("\n")
                        .append(c.getContent() == null ? "" : c.getContent()).append("\n\n");
            }
            userMsg.append("【说明】请优先依据上述资料作答，未涵盖时再用通识回答；引用资料时使用 [编号]。\n\n");
        }
        userMsg.append("【用户问题】").append(req.getMessage());

        messages.add(Message.builder().role(Role.USER.getValue()).content(userMsg.toString()).build());

        // 3) 调用通义千问
        String reply;
        try {
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.9)
                    .build();
            GenerationResult result = generation.call(param);
            reply = result.getOutput().getChoices().get(0).getMessage().getContent();
        } catch (Exception e) {
            log.error("AI 调用失败", e);
            reply = "AI 服务暂不可用，请稍后再试。错误信息：" + e.getMessage();
        }

        // 4) 持久化对话
        if (req.getUserId() != null) {
            ChatMessage in = new ChatMessage();
            in.setUserId(req.getUserId());
            in.setSessionId(req.getSessionId());
            in.setRole("user");
            in.setContent(req.getMessage());
            in.setCreatedAt(LocalDateTime.now());
            chatMessageMapper.insert(in);

            ChatMessage out = new ChatMessage();
            out.setUserId(req.getUserId());
            out.setSessionId(req.getSessionId());
            out.setRole("assistant");
            out.setContent(reply);
            out.setCreatedAt(LocalDateTime.now());
            chatMessageMapper.insert(out);
        }

        ChatResponseDTO resp = new ChatResponseDTO();
        resp.setSessionId(req.getSessionId());
        resp.setReply(reply);
        resp.setCitations(citations);
        resp.setLatencyMs(System.currentTimeMillis() - t0);
        return resp;
    }

    @Override
    public ChatResponseDTO recommendCourses(Long userId, String prompt) {
        ChatRequestDTO r = new ChatRequestDTO();
        r.setUserId(userId);
        r.setRole("student");
        r.setScene("recommend");
        r.setMessage(prompt == null ? "请基于学生评价帮我推荐合适的课程，并对课程难度做评估。" : prompt);
        return chat(r);
    }

    @Override
    public String summarizeQuestionnaire(Long courseId, Long questionnaireId) {
        Questionnaires q = questionnairesMapper.selectById(questionnaireId);
        if (q == null) return "问卷不存在";

        QueryWrapper<QuestionnaireResponses> qw = new QueryWrapper<>();
        qw.eq("course_id", courseId).eq("questionnaire_id", questionnaireId);
        List<QuestionnaireResponses> list = questionnaireResponsesMapper.selectList(qw);
        if (list.isEmpty()) return "暂无问卷答案，无法总结";

        String combined = list.stream().limit(80).map(QuestionnaireResponses::getAnswers)
                .collect(Collectors.joining("\n----\n"));

        String prompt = "下面是某门课程问卷的匿名学生答案，请你做四件事：\n" +
                "1) 三句话总结整体满意度；\n" +
                "2) 列出排名前 5 的优点；\n" +
                "3) 列出排名前 5 的待改进项；\n" +
                "4) 给授课老师一句直接的、可执行的改进建议。\n" +
                "请使用中文，不要凭空编造。\n\n问卷题目：\n" + q.getQuestions() +
                "\n\n答案集合（每条用 ---- 分隔）：\n" + combined;

        ChatRequestDTO req = new ChatRequestDTO();
        req.setRole("teacher");
        req.setScene("summarize");
        req.setMessage(prompt);
        return chat(req).getReply();
    }

    private Message systemMessage(String role, String scene) {
        String r = "student".equals(role) ? "学生" : "teacher".equals(role) ? "教师" : "用户";
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个面向高校《课程反馈管理系统》的 AI 助理。当前对话方角色：").append(r).append("。");
        sb.append("请遵循以下原则：1) 回答必须基于内部资料（若有），不要凭空编造；")
          .append("2) 涉及具体学生评价/打分时，禁止透露学生姓名或学号，使用『有学生提到…』的描述；")
          .append("3) 回答简洁清楚，可使用列表；4) 学生场景下提供学习建议、选课分析、课程难度估计；")
          .append("5) 教师场景下擅长归纳问卷反馈、给出教学改进建议、识别共性问题。");
        if ("recommend".equals(scene)) sb.append("当前任务是：基于评价库给出选课推荐与难度评估。");
        if ("summarize".equals(scene)) sb.append("当前任务是：归纳问卷统计结果。");
        return Message.builder().role(Role.SYSTEM.getValue()).content(sb.toString()).build();
    }

    private String safeRole(String r) {
        if (r == null) return Role.USER.getValue();
        switch (r.toLowerCase()) {
            case "system": return Role.SYSTEM.getValue();
            case "assistant": return Role.ASSISTANT.getValue();
            default: return Role.USER.getValue();
        }
    }
}
