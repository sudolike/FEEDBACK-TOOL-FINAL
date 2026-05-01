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
import com.cen.entity.CourseFeedback;
import com.cen.entity.CourseQuestionnaire;
import com.cen.entity.Courses;
import com.cen.entity.KbChunk;
import com.cen.entity.QuestionnaireResponses;
import com.cen.entity.Questionnaires;
import com.cen.mapper.ChatMessageMapper;
import com.cen.mapper.CourseFeedbackMapper;
import com.cen.mapper.CourseQuestionnaireMapper;
import com.cen.mapper.CoursesMapper;
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
import java.util.Locale;
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
    @Resource private CoursesMapper coursesMapper;
    @Resource private CourseFeedbackMapper courseFeedbackMapper;
    @Resource private CourseQuestionnaireMapper courseQuestionnaireMapper;

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

        // 教师场景的 query routing：
        // 当老师在通用 AI 助手里问"问卷反馈/学生评价/总结改进"等意图时，
        // 自动注入这位老师近期课程的真实问卷答案与课程评价，避免 LLM 凭空作答。
        // 包一层 try 兜底：即便 routing 内部数据访问异常，也不能让整个 /assistant/chat 500。
        String teacherCtx = null;
        try {
            teacherCtx = buildTeacherFeedbackContext(req);
        } catch (Exception e) {
            log.warn("buildTeacherFeedbackContext 失败，跳过教师反馈注入：{}", e.getMessage());
        }
        if (teacherCtx != null) {
            userMsg.append(teacherCtx);
        }

        userMsg.append("【用户问题】").append(req.getMessage());

        messages.add(Message.builder().role(Role.USER.getValue()).content(userMsg.toString()).build());

        // 3) 调用通义千问
        String reply;
        // demo fallback 仅在用户提问明确指向 HKU 示例课程时才触发，避免任意提问被 demo 截胡。
        String demoFallbackReply = buildDemoFallbackReply(req.getMessage(), citations);
        String genericFallback = buildGenericFallbackReply(citations);
        if (StrUtil.isBlank(apiKey)) {
            reply = demoFallbackReply != null
                    ? demoFallbackReply
                    : (genericFallback != null
                        ? genericFallback
                        : "AI 服务暂不可用，请先配置 AI_API_KEY 后重试。");
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
                    reply = genericFallback != null
                            ? genericFallback
                            : (demoFallbackReply != null ? demoFallbackReply : "AI 服务暂不可用，请稍后再试。");
                }
            } catch (Exception e) {
                log.error("AI 调用失败", e);
                // 调用失败时优先给"基于检索资料"的通用兜底，最后才考虑 demo 文案
                reply = genericFallback != null
                        ? genericFallback
                        : (demoFallbackReply != null
                            ? demoFallbackReply
                            : "AI 服务暂不可用，请稍后再试。错误信息：" + e.getMessage());
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

        // 直接构造完整 prompt 调用 LLM —— 故意绕开 chat() 走 RAG 检索
        // （问卷答案本身已经是完整数据，再做 RAG 反而会混入无关课程内容）
        String combined = list.stream().limit(80).map(QuestionnaireResponses::getAnswers)
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.joining("\n----\n"));

        String prompt = "你是教学评估专家。下面是某门课程匿名学生问卷的全部答案。请用中文输出：\n" +
                "1) 三句话总结整体满意度（含覆盖人数）；\n" +
                "2) 排名前 5 的优点（要具体）；\n" +
                "3) 排名前 5 的待改进项（要具体）；\n" +
                "4) 给授课老师一句可立刻执行的改进建议。\n" +
                "约束：禁止编造未在数据中出现的内容；禁止透露学生姓名/学号；如果答案数量很少，请直接说明样本不足。\n\n" +
                "问卷标题：" + (q.getTitle() == null ? "" : q.getTitle()) + "\n" +
                "问卷题目 (JSON)：\n" + (q.getQuestions() == null ? "" : q.getQuestions()) +
                "\n\n答案集合（共 " + list.size() + " 份，每条用 ---- 分隔）：\n" + combined;

        // 调用 LLM；失败/未配置 key 时回退到基于真实数据的本地汇总（绝不返回 RAG 检索结果）
        if (StrUtil.isBlank(apiKey)) {
            return localQuestionnaireSummary(q, list);
        }
        try {
            List<Message> messages = new ArrayList<>();
            messages.add(Message.builder().role(Role.SYSTEM.getValue())
                    .content("你是面向高校的教学评估专家，回答必须基于给定数据，禁止编造。").build());
            messages.add(Message.builder().role(Role.USER.getValue()).content(prompt).build());
            GenerationParam param = GenerationParam.builder()
                    .apiKey(apiKey)
                    .model(model)
                    .messages(messages)
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .build();
            GenerationResult result = generation.call(param);
            String reply = result.getOutput().getChoices().get(0).getMessage().getContent();
            if (StrUtil.isBlank(reply)) {
                return localQuestionnaireSummary(q, list);
            }
            return reply;
        } catch (Exception e) {
            log.warn("summarizeQuestionnaire 调用 AI 失败，回退本地汇总：{}", e.getMessage());
            return localQuestionnaireSummary(q, list);
        }
    }

    /**
     * AI 不可用时的"基于真实问卷数据"本地兜底：
     * 按题型聚合统计 + 给出几条文本答案样本。
     * 关键：绝对不引入 RAG 检索内容（避免与 demo HKU 等无关数据混淆）。
     */
    private String localQuestionnaireSummary(Questionnaires q, List<QuestionnaireResponses> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("（AI 在线服务暂不可用，以下基于本课程问卷的真实答案做本地汇总）\n\n");
        sb.append("问卷：").append(q.getTitle() == null ? "" : q.getTitle())
                .append("（共 ").append(list.size()).append(" 份匿名答案）\n\n");

        com.alibaba.fastjson.JSONArray questions;
        try {
            questions = com.alibaba.fastjson.JSON.parseArray(q.getQuestions());
        } catch (Exception e) {
            questions = null;
        }
        if (questions == null || questions.isEmpty()) {
            sb.append("题目结构解析失败，请检查问卷设计。");
            return sb.toString();
        }

        // 把所有答案解析成 List<Map>
        List<java.util.Map<String, Object>> parsed = new ArrayList<>();
        for (QuestionnaireResponses r : list) {
            if (r.getAnswers() == null || r.getAnswers().isEmpty()) continue;
            try {
                com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject(r.getAnswers());
                if (obj != null) parsed.add(new java.util.HashMap<>(obj));
            } catch (Exception ignore) {}
        }

        for (int i = 0; i < questions.size(); i++) {
            com.alibaba.fastjson.JSONObject qi = questions.getJSONObject(i);
            if (qi == null) continue;
            String qid = qi.getString("id");
            String title = qi.getString("title");
            String type = qi.getString("type") == null ? "text" : qi.getString("type");
            sb.append((i + 1)).append(". ").append(title == null ? "" : title)
                    .append("（").append(type).append("）\n");

            switch (type.toLowerCase(Locale.ROOT)) {
                case "rating": {
                    double sum = 0; int cnt = 0; int[] hist = new int[5];
                    for (java.util.Map<String, Object> a : parsed) {
                        Object v = qid == null ? null : a.get(qid);
                        if (v == null) continue;
                        try {
                            int r = (v instanceof Number)
                                    ? ((Number) v).intValue()
                                    : Integer.parseInt(String.valueOf(v));
                            if (r >= 1 && r <= 5) { hist[r-1]++; sum += r; cnt++; }
                        } catch (Exception ignore) {}
                    }
                    if (cnt > 0) {
                        sb.append("   平均分 ").append(String.format(Locale.ROOT, "%.2f", sum/cnt))
                                .append(" / 5（").append(cnt).append(" 份回答）。分布：");
                        for (int s = 1; s <= 5; s++) {
                            sb.append(s).append("★=").append(hist[s-1]).append(" ");
                        }
                        sb.append('\n');
                    } else {
                        sb.append("   暂无评分。\n");
                    }
                    break;
                }
                case "single":
                case "multiple": {
                    java.util.LinkedHashMap<String, Integer> dist = new java.util.LinkedHashMap<>();
                    for (java.util.Map<String, Object> a : parsed) {
                        Object v = qid == null ? null : a.get(qid);
                        if (v == null) continue;
                        if (v instanceof java.util.List) {
                            for (Object ov : (java.util.List<?>) v) dist.merge(String.valueOf(ov), 1, Integer::sum);
                        } else {
                            dist.merge(String.valueOf(v), 1, Integer::sum);
                        }
                    }
                    if (dist.isEmpty()) sb.append("   无答案。\n");
                    else {
                        for (java.util.Map.Entry<String, Integer> e : dist.entrySet()) {
                            sb.append("   - ").append(e.getKey()).append("：").append(e.getValue()).append(" 票\n");
                        }
                    }
                    break;
                }
                default: { // text
                    int shown = 0;
                    for (java.util.Map<String, Object> a : parsed) {
                        Object v = qid == null ? null : a.get(qid);
                        if (v == null) continue;
                        String s = String.valueOf(v).trim();
                        if (s.isEmpty()) continue;
                        if (shown >= 5) break;
                        if (s.length() > 160) s = s.substring(0, 160) + "...";
                        sb.append("   · ").append(s).append('\n');
                        shown++;
                    }
                    if (shown == 0) sb.append("   无文本答案。\n");
                }
            }
            sb.append('\n');
        }
        sb.append("（请稍后重试或检查 AI_API_KEY 以获得 AI 文字总结。）");
        return sb.toString();
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

    /**
     * demo HKU fallback：仅在 query 明确指向 HKU 示例课程时返回写死文案。
     * 仅靠 citation 中存在 demo chunk 不足以触发（避免任何提问都被 HKU 截胡）。
     */
    private String buildDemoFallbackReply(String query, List<KbChunk> citations) {
        if (citations == null || citations.isEmpty()) return null;
        if (!queryMentionsHkuDemo(query)) return null;
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

    private boolean queryMentionsHkuDemo(String query) {
        if (query == null) return false;
        String q = query.toLowerCase(Locale.ROOT);
        return q.contains("hku") || q.contains("geog7307") || q.contains("geog7310")
                || q.contains("comp7305") || q.contains("comp3230")
                || q.contains("cloud computing for geospatial")
                || q.contains("principles of operating systems")
                || q.contains("big data analytics");
    }

    /**
     * 通用兜底：AI 不可用时，把检索到的真实资料整理成一段引用式回答。
     * 不会把无关的 demo 文案塞给老师。
     */
    private String buildGenericFallbackReply(List<KbChunk> citations) {
        if (citations == null || citations.isEmpty()) return null;
        List<KbChunk> real = citations.stream()
                .filter(c -> c != null && !DEMO_SOURCE_TYPE.equals(c.getSourceType()))
                .limit(3)
                .collect(Collectors.toList());
        if (real.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        sb.append("（AI 在线服务暂时不可用，以下是从内部资料中检索到的相关内容，供参考）\n\n");
        int idx = 1;
        for (KbChunk c : real) {
            sb.append("[").append(idx++).append("] ").append(c.getTitle() == null ? "" : c.getTitle()).append("\n");
            String content = c.getContent() == null ? "" : c.getContent();
            if (content.length() > 400) content = content.substring(0, 400) + "...";
            sb.append(content).append("\n\n");
        }
        sb.append("如需更精炼的总结，请稍后再试或检查 AI_API_KEY 配置。");
        return sb.toString();
    }

    /* =====================================================================
     * 教师场景 query routing：把真实问卷反馈 / 课程评价注入对话上下文
     * ===================================================================== */

    private static final int FEEDBACK_LIMIT_PER_COURSE = 30;
    private static final int RESPONSE_LIMIT_PER_QUEST = 40;
    private static final int MAX_COURSES_FOR_TEACHER = 3;

    /**
     * 命中"问卷反馈 / 学生评价 / 改进建议"等意图，且当前为教师角色时返回真实数据上下文。
     * 否则返回 null（保留通用 RAG 流程）。
     */
    private String buildTeacherFeedbackContext(ChatRequestDTO req) {
        if (req == null || !"teacher".equalsIgnoreCase(req.getRole())) return null;
        if (req.getUserId() == null) return null;
        String msg = req.getMessage();
        if (StrUtil.isBlank(msg)) return null;
        if (!isTeacherFeedbackIntent(msg)) return null;

        // 选课范围：优先 req.courseId；否则取该教师所授课程（最多 MAX_COURSES_FOR_TEACHER 门）
        List<Courses> courses = new ArrayList<>();
        if (req.getCourseId() != null) {
            Courses single = coursesMapper.selectById(req.getCourseId());
            if (single != null && req.getUserId().equals(single.getTeacherId())) {
                courses.add(single);
            }
        } else {
            QueryWrapper<Courses> cq = new QueryWrapper<>();
            cq.eq("teacher_id", req.getUserId()).orderByDesc("id")
                    .last("LIMIT " + MAX_COURSES_FOR_TEACHER);
            courses = coursesMapper.selectList(cq);
        }
        if (courses.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        sb.append("【教师内部数据：你授课课程的近期反馈与问卷答案】\n");
        sb.append("（仅供你参考，请不要透露具体学生姓名/学号）\n\n");

        boolean anyData = false;
        for (Courses c : courses) {
            sb.append("== 课程：").append(c.getName() == null ? "未命名" : c.getName())
                    .append("（ID=").append(c.getId()).append("） ==\n");

            // 1) 课程文字评价 / 评分
            QueryWrapper<CourseFeedback> fq = new QueryWrapper<>();
            fq.eq("course_id", c.getId()).orderByDesc("id")
                    .last("LIMIT " + FEEDBACK_LIMIT_PER_COURSE);
            List<CourseFeedback> fbs = courseFeedbackMapper.selectList(fq);
            if (!fbs.isEmpty()) {
                anyData = true;
                double avg = fbs.stream().filter(f -> f.getRating() != null)
                        .mapToInt(CourseFeedback::getRating).average().orElse(0);
                sb.append("[课程评价] 共 ").append(fbs.size())
                        .append(" 条，平均评分 ").append(String.format("%.2f", avg)).append("/5\n");
                int kept = 0;
                for (CourseFeedback f : fbs) {
                    if (kept++ >= 12) break;
                    sb.append("  - ").append(f.getRating() == null ? "" : ("[" + f.getRating() + "★] "))
                            .append(safe(f.getContent())).append('\n');
                }
            }

            // 2) 该课程下问卷答案
            //    sys_questionnaires 是问卷模板表（无 course_id），
            //    课程↔问卷的绑定在中间表 sys_course_questionnaire 中，
            //    所以这里要先通过中间表拿到该课程绑定的问卷 id。
            QueryWrapper<CourseQuestionnaire> cqq = new QueryWrapper<>();
            cqq.eq("course_id", c.getId()).orderByDesc("id").last("LIMIT 3");
            List<CourseQuestionnaire> binds = courseQuestionnaireMapper.selectList(cqq);
            for (CourseQuestionnaire bind : binds) {
                if (bind.getQuestionnaireId() == null) continue;
                Questionnaires q = questionnairesMapper.selectById(bind.getQuestionnaireId());
                if (q == null) continue;
                QueryWrapper<QuestionnaireResponses> rq = new QueryWrapper<>();
                rq.eq("course_id", c.getId()).eq("questionnaire_id", q.getId())
                        .orderByDesc("id").last("LIMIT " + RESPONSE_LIMIT_PER_QUEST);
                List<QuestionnaireResponses> resps = questionnaireResponsesMapper.selectList(rq);
                if (resps.isEmpty()) continue;
                anyData = true;
                sb.append("[问卷:").append(safe(q.getTitle())).append("] 共 ")
                        .append(resps.size()).append(" 份匿名回答\n");
                int kept = 0;
                for (QuestionnaireResponses r : resps) {
                    if (kept++ >= 15) break;
                    String answers = safe(r.getAnswers());
                    if (answers.length() > 280) answers = answers.substring(0, 280) + "...";
                    sb.append("  · ").append(answers).append('\n');
                }
            }
            sb.append('\n');
        }

        if (!anyData) return null;
        sb.append("【任务】请基于以上真实数据回答用户的问题，不要编造未在数据中出现的内容。\n\n");
        return sb.toString();
    }

    private boolean isTeacherFeedbackIntent(String msg) {
        String q = msg.toLowerCase(Locale.ROOT);
        // 中英文常见意图关键词，命中任意一个即视为"教师反馈分析"场景
        String[] keys = {
                "问卷", "反馈", "评价", "学生意见", "意见", "改进", "总结", "汇总",
                "归纳", "评分", "满意度",
                "feedback", "survey", "questionnaire", "summary", "summarize",
                "review", "improve", "rating",
        };
        for (String k : keys) {
            if (q.contains(k)) return true;
        }
        return false;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
