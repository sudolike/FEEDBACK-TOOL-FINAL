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

    private static final String DEMO_SOURCE_TYPE = "demo_hku";
    private static final long DEMO_SUMMARY_ID = 910001L;
    private static final long DEMO_RECOMMEND_ID = 910002L;
    private static final long DEMO_DIFFICULTY_ID = 910003L;

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
        String demoFallbackReply = buildDemoFallbackReply(citations);
        if (StrUtil.isBlank(apiKey)) {
            reply = demoFallbackReply != null
                    ? demoFallbackReply
                    : "AI 服务暂不可用，请先配置 AI_API_KEY 后重试。";
        } else {
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
                if (StrUtil.isBlank(reply)) {
                    reply = demoFallbackReply != null
                            ? demoFallbackReply
                            : "AI 服务暂不可用，请稍后再试。";
                }
            } catch (Exception e) {
                log.error("AI 调用失败", e);
                reply = demoFallbackReply != null
                        ? demoFallbackReply
                        : "AI 服务暂不可用，请稍后再试。错误信息：" + e.getMessage();
            }
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

    private String buildDemoFallbackReply(List<KbChunk> citations) {
        if (citations == null || citations.isEmpty()) return null;
        for (KbChunk citation : citations) {
            if (!DEMO_SOURCE_TYPE.equals(citation.getSourceType()) || citation.getSourceId() == null) {
                continue;
            }
            long sourceId = citation.getSourceId();
            if (sourceId == DEMO_SUMMARY_ID) {
                return "可以把 HKU GEOG7310 理解为一门 6 学分、偏应用导向的云计算课程，主题是把 cloud computing 用在 geospatial data analytics 上。[1]\n\n"
                        + "基于知识库，这门课的核心覆盖 cloud concepts、platforms、services，以及 cloud architecture、data storage and retrieval、processing and analysis、visualization，还强调 hands-on cloud-based tools，以及部署 cloud-based geospatial data applications。[1]\n\n"
                        + "如果从课程评价总结的角度看，根据知识库，它更适合对 GIS、空间数据、遥感或云平台结合感兴趣的学生；优点是实践性强、应用场景明确、项目感较强；如果缺少 geospatial data 或 cloud basics 基础，上手速度可能会偏慢。[1]";
            }
            if (sourceId == DEMO_RECOMMEND_ID) {
                return "如果你在 HKU 想学云计算和数据分析，这三门课的定位可以这样看：[1]\n\n"
                        + "GEOG7307 更偏 big data analytics，根据知识库，它更偏统计分析、建模、可视化、data fusion 和 data-mining 导向，适合想强化数据分析方法的同学。[1]\n"
                        + "GEOG7310 更偏 geospatial application + cloud workflow，适合想把云平台能力用于空间数据分析与应用部署的同学。[1]\n"
                        + "COMP7305 更偏 distributed systems / cloud stack，覆盖 SaaS、PaaS、IaaS、virtualization、Hadoop file system、MapReduce、Spark 和 Amazon EC2 deployment，适合想走系统和平台实现路线的同学。[1]\n\n"
                        + "所以根据知识库的推荐结论：偏统计建模选 GEOG7307，偏空间数据云应用选 GEOG7310，偏集群与云系统实现选 COMP7305。[1]";
            }
            if (sourceId == DEMO_DIFFICULTY_ID) {
                return "HKU COMP3230 可以按中高难度来理解，更适合已经有编程基础和系统基础的学生。[1]\n\n"
                        + "知识库里的官方事实包括：它是 6 学分课程，先修要求是 COMP2113 或 COMP2123 或 ENGG1340；以及 COMP2120 或 ELEC2441。课程主题覆盖 operating system structures、process and thread、CPU scheduling、process synchronization、deadlocks、memory management、file systems、I/O systems、device driver 和 disk scheduling。[1]\n\n"
                        + "从难点看，通常会卡在 concurrency、synchronization、virtual memory、deadlock 和 file-system reasoning。根据知识库，建议先补足编程和系统基础，再修这门课会更稳。[1]";
            }
        }
        return null;
    }
}
