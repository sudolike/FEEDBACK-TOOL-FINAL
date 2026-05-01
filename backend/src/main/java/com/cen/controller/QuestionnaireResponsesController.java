package com.cen.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cen.common.Constants;
import com.cen.controller.dto.QuestionnaireResponseDetailDTO;
import com.cen.controller.dto.QuestionnaireResponseSummaryDTO;
import com.cen.exception.ServiceException;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cen.common.Result;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.multipart.MultipartFile;

import com.cen.service.IQuestionnaireResponsesService;
import com.cen.entity.QuestionnaireResponses;
import com.cen.entity.Questionnaires;
import com.cen.mapper.QuestionnairesMapper;
import com.cen.ai.AiClient;
import com.cen.ai.AiMessage;

import java.time.LocalDateTime;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 问卷答案表 前端控制器
 * </p>
 *
 * @author wyt
 * @since 2025-03-16
 */
@RestController
@RequestMapping("/questionnaireResponses")
public class QuestionnaireResponsesController {

    @Resource
    private IQuestionnaireResponsesService questionnaireResponsesService;

    @Resource
    private AiClient aiClient;

    @Resource
    private QuestionnairesMapper questionnairesMapper;

    //新增或修改
    @PostMapping("/save")
    public Result save(@RequestBody QuestionnaireResponses questionnaireResponses) {
        if (questionnaireResponses.getId() == null) {
            questionnaireResponses.setSubmittedAt(LocalDateTime.now());
        }
        return Result.success(questionnaireResponsesService.saveOrUpdate(questionnaireResponses));
    }
    //删除
    @PostMapping("/delete")
    public Result delete(@RequestBody QuestionnaireResponses questionnaireResponses){ //@RequestBody把前台的json对象转成java的对象
        return Result.success(questionnaireResponsesService.removeById(questionnaireResponses.getId()));
    }
    //批量删除
    @PostMapping("/del/batch")
    public Result Batch(@RequestBody List<Integer> ids){
        return Result.success(questionnaireResponsesService.removeBatchByIds(ids));
    }
    //根据id获取
    @GetMapping("/getById")
    public Result findOne(@PathVariable QuestionnaireResponses questionnaireResponses) {
        return Result.success(questionnaireResponsesService.getById(questionnaireResponses.getId()));
    }
    //分页查询
    @GetMapping("/page")
    public Result findPage(@RequestParam(defaultValue = "1") Integer pageNum,
                           @RequestParam(defaultValue = "10") Integer pageSize) {
        QueryWrapper<QuestionnaireResponses> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("id"); //设置id倒序
        return Result.success(questionnaireResponsesService.page(new Page<>(pageNum, pageSize)));
    }
    // 查询课程问卷的填写情况列表（含学生身份，仅管理员/系统内部调用）
    @GetMapping("/FillinDetails")
    public Result getCourseQuestionnaireResponses(
            @RequestParam Long courseId,
            @RequestParam Long questionnaireId) {
        return Result.success(questionnaireResponsesService.getQuestionnaireFillinDetails(courseId, questionnaireId));
    }

    // 教师端拉取问卷答案：匿名化版本（默认接口，前端展示用）
    @GetMapping("/FillinAnonymous")
    public Result getCourseQuestionnaireResponsesAnonymous(
            @RequestParam Long courseId,
            @RequestParam Long questionnaireId) {
        return Result.success(questionnaireResponsesService.getQuestionnaireFillinAnonymous(courseId, questionnaireId));
    }

    /**
     * 问卷数据分析（含逐题统计 + 可选 AI 文本分析）
     *
     * 返回字段：
     *  - questionnaire     问卷基本信息
     *  - totalResponses    填写总数
     *  - questions[]       逐题统计（前端"逐题分析"卡片消费）
     *      - id / title / type / responses / required
     *      - distribution  选择题选项分布 / 评分题 1..5 分布
     *      - average       评分题平均分
     *      - samples       文本题前若干条示例
     *  - analysis          AI 文本分析（在线服务可用时返回，否则为空字符串）
     */
    @GetMapping("/analysis")
    public Result getQuestionnaireAnalysis(
            @RequestParam Long courseId,
            @RequestParam Long questionnaireId) {
        // 1. 获取问卷基本信息
        Questionnaires questionnaire = questionnairesMapper.selectById(questionnaireId);
        if (questionnaire == null) {
            throw new ServiceException(Constants.CODE_402,"问卷不存在");
        }

        // 2. 获取问卷的填写详情
        QuestionnaireResponseSummaryDTO responses =
                questionnaireResponsesService.getQuestionnaireResponseSummary(courseId, questionnaireId);

        // 3. 逐题统计（前端"逐题分析"卡片直接消费）
        List<Map<String, Object>> perQuestion = buildPerQuestionStats(
                questionnaire.getQuestions(), responses.getAnswers());

        // 4. 在线 AI 分析（失败/未配置 key 时退化为空字符串，不阻塞统计返回）
        String analysis = tryAiQuestionnaireAnalysis(questionnaire, responses);

        Map<String, Object> result = new HashMap<>();
        result.put("questionnaire", questionnaire);
        result.put("totalResponses", responses.getTotalResponses());
        result.put("questions", perQuestion);
        result.put("analysis", analysis);
        return Result.success(result);
    }

    /**
     * 解析问卷题目 + 所有答案，按题型聚合统计。
     * 鲁棒处理：单条答案可能是 {qid: value} 形式，也可能是空 / 非法 JSON。
     */
    private List<Map<String, Object>> buildPerQuestionStats(
            String questionsJson, List<String> answerJsonList) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (questionsJson == null || questionsJson.trim().isEmpty()) return out;

        com.alibaba.fastjson.JSONArray questions;
        try {
            questions = com.alibaba.fastjson.JSON.parseArray(questionsJson);
        } catch (Exception e) {
            return out;
        }
        if (questions == null) return out;

        // 把所有答案解析成 List<Map> 备用（容错：JSON / 字符串 / null）
        List<Map<String, Object>> parsedAnswers = new ArrayList<>();
        if (answerJsonList != null) {
            for (String ans : answerJsonList) {
                if (ans == null || ans.trim().isEmpty()) continue;
                try {
                    com.alibaba.fastjson.JSONObject obj = com.alibaba.fastjson.JSON.parseObject(ans);
                    if (obj != null) parsedAnswers.add(new HashMap<>(obj));
                } catch (Exception ignore) {
                    // 容错：单条答案非合法 JSON 时整体跳过
                }
            }
        }

        for (int i = 0; i < questions.size(); i++) {
            com.alibaba.fastjson.JSONObject q = questions.getJSONObject(i);
            if (q == null) continue;
            String qid = q.getString("id");
            String type = q.getString("type") == null ? "text" : q.getString("type");
            String title = q.getString("title");
            Boolean required = q.getBoolean("required");
            com.alibaba.fastjson.JSONArray opts = q.getJSONArray("options");

            Map<String, Object> stat = new HashMap<>();
            stat.put("id", qid);
            stat.put("title", title);
            stat.put("type", type);
            stat.put("required", required != null && required);

            int answeredCount = 0;
            Map<String, Integer> dist = new java.util.LinkedHashMap<>();
            // 评分题预填 1..5 槽，避免前端只看到部分键
            if ("rating".equalsIgnoreCase(type)) {
                for (int s = 1; s <= 5; s++) dist.put(s + "★", 0);
            } else if (opts != null) {
                for (int oi = 0; oi < opts.size(); oi++) {
                    dist.put(String.valueOf(opts.get(oi)), 0);
                }
            }
            double sum = 0;
            int sumCount = 0;
            List<String> textSamples = new ArrayList<>();

            for (Map<String, Object> ans : parsedAnswers) {
                Object v = qid == null ? null : ans.get(qid);
                if (v == null) continue;
                answeredCount++;
                switch (type.toLowerCase()) {
                    case "single":
                        dist.merge(String.valueOf(v), 1, Integer::sum);
                        break;
                    case "multiple":
                        if (v instanceof java.util.List) {
                            for (Object ov : (java.util.List<?>) v) {
                                dist.merge(String.valueOf(ov), 1, Integer::sum);
                            }
                        } else {
                            dist.merge(String.valueOf(v), 1, Integer::sum);
                        }
                        break;
                    case "rating":
                        try {
                            int r = (v instanceof Number)
                                    ? ((Number) v).intValue()
                                    : Integer.parseInt(String.valueOf(v));
                            if (r >= 1 && r <= 5) {
                                dist.merge(r + "★", 1, Integer::sum);
                                sum += r;
                                sumCount++;
                            }
                        } catch (Exception ignore) {}
                        break;
                    case "text":
                    default:
                        String s = String.valueOf(v).trim();
                        if (!s.isEmpty() && textSamples.size() < 8) {
                            textSamples.add(s.length() > 200 ? s.substring(0, 200) + "..." : s);
                        }
                        break;
                }
            }

            stat.put("responses", answeredCount);
            if (!"text".equalsIgnoreCase(type)) {
                stat.put("distribution", dist);
            }
            if ("rating".equalsIgnoreCase(type) && sumCount > 0) {
                stat.put("average", Math.round((sum / sumCount) * 100.0) / 100.0);
            }
            if ("text".equalsIgnoreCase(type) && !textSamples.isEmpty()) {
                stat.put("samples", textSamples);
            }
            out.add(stat);
        }
        return out;
    }

    /** 在线 AI 分析（失败 → 返回空串；不影响逐题统计返回） */
    private String tryAiQuestionnaireAnalysis(Questionnaires questionnaire,
                                              QuestionnaireResponseSummaryDTO responses) {
        if (!aiClient.isReady()) return "";
        try {
            StringBuilder aiInput = new StringBuilder();
            aiInput.append("请用中文为下面这份课程问卷给出 1) 整体满意度概述（3 句以内）；")
                    .append("2) 学生反映的 3-5 个亮点；3) 待改进的 3-5 个共性问题；")
                    .append("4) 给授课教师一句直接、可执行的改进建议。\n")
                    .append("不要透露任何学生姓名/学号；不要编造未在数据中出现的内容。\n\n");
            aiInput.append("问卷标题：").append(safe(questionnaire.getTitle())).append("\n");
            aiInput.append("问卷描述：").append(safe(questionnaire.getDescription())).append("\n");
            aiInput.append("总题数：").append(responses.getTotalQuestions()).append("\n");
            aiInput.append("总填写人数：").append(responses.getTotalResponses()).append("\n\n");
            aiInput.append("题目结构 (JSON)：").append(safe(questionnaire.getQuestions())).append("\n\n");
            aiInput.append("答案集合（每条用 ---- 分隔）：\n");
            if (responses.getAnswers() != null) {
                for (int i = 0; i < responses.getAnswers().size() && i < 80; i++) {
                    aiInput.append(safe(responses.getAnswers().get(i))).append("\n----\n");
                }
            }

            String text = aiClient.chat(
                    "你是面向高校的教学评估专家，回答必须基于给定数据，禁止编造。",
                    Arrays.asList(AiMessage.user(aiInput.toString())));
            return text == null ? "" : text;
        } catch (Exception e) {
            return "";
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }

    /**
     * AI对比分析多个问卷填写情况
     * 通过统一的 AiClient（dashscope / pai-eas）对多个问卷的填写情况进行对比分析。
     * 支持自定义分析描述，指导AI关注特定分析维度。
     */
    @GetMapping("/compareAnalysis")
    public Result compareQuestionnaireAnalysis(
            @RequestParam List<Long> courseIds,
            @RequestParam List<Long> questionnaireIds,
            @RequestParam(required = false) String analysisDescription) {
        // 1. 验证参数
        if (courseIds.size() != questionnaireIds.size()) {
            throw new ServiceException(Constants.CODE_400, "课程和问卷数量不匹配");
        }

        // 2. 获取所有问卷信息和填写详情
        List<Map<String, Object>> questionnairesData = new ArrayList<>();
        for (int i = 0; i < courseIds.size(); i++) {
            Long courseId = courseIds.get(i);
            Long questionnaireId = questionnaireIds.get(i);
            
            // 获取问卷基本信息
            Questionnaires questionnaire = questionnairesMapper.selectById(questionnaireId);
            if (questionnaire == null) {
                throw new ServiceException(Constants.CODE_402, "问卷不存在: " + questionnaireId);
            }
            
            // 获取问卷填写详情
            QuestionnaireResponseSummaryDTO responses = 
                questionnaireResponsesService.getQuestionnaireResponseSummary(courseId, questionnaireId);
            
            Map<String, Object> questionnaireData = new HashMap<>();
            questionnaireData.put("questionnaire", questionnaire);
            questionnaireData.put("responses", responses);
            questionnairesData.add(questionnaireData);
        }

        // 3. 构建AI对比分析的输入内容（预设指令）
        StringBuilder aiInput = new StringBuilder();
        // 设置AI对比分析的主要任务和输出要求（使用英文输出）
        aiInput.append("请对以下多个问卷的填写情况进行对比分析，并给出专业的分析报告。注意全文用英文输出\n\n");
        
        // 如果提供了自定义分析描述，则添加到提示中
        if (analysisDescription != null && !analysisDescription.trim().isEmpty()) {
            aiInput.append("分析要求：").append(analysisDescription).append("\n\n");
        }
        
        for (int i = 0; i < questionnairesData.size(); i++) {
            Map<String, Object> data = questionnairesData.get(i);
            Questionnaires questionnaire = (Questionnaires) data.get("questionnaire");
            QuestionnaireResponseSummaryDTO responses = (QuestionnaireResponseSummaryDTO) data.get("responses");
            aiInput.append("问卷标题：").append(questionnaire.getTitle()).append("\n");
            aiInput.append("问卷描述：").append(questionnaire.getDescription()).append("\n");
            aiInput.append("总问题数量：").append(responses.getTotalQuestions()).append("\n");
            aiInput.append("总填写人数：").append(responses.getTotalResponses()).append("\n");
            aiInput.append("完成率：").append(responses.getCompletionRate()).append("%\n");
            aiInput.append("题目数据（title是题目,id对应答案的key）：").append(questionnaire.getQuestions()).append("\n");
            aiInput.append("答案数据（只是答案）：\n").append(responses.getAnswers()).append("\n\n");
        }
        
        // 如果没有自定义分析描述，则使用默认的分析维度
        // 这是预设的分析框架，指导AI从多个维度进行全面分析
        if (analysisDescription == null || analysisDescription.trim().isEmpty()) {
            aiInput.append("注意全文用英文输出：请从以下几个方面进行分析：\n");
            aiInput.append("1. 各问卷的参与度对比\n"); // 分析问卷参与情况
            aiInput.append("2. 答案内容的相似性和差异性\n"); // 分析答案内容的异同
            aiInput.append("3. 问卷设计的优劣对比\n"); // 评估问卷设计质量
            aiInput.append("4. 总体趋势和特点\n"); // 总结整体趋势
            aiInput.append("5. 改进建议\n\n"); // 提供具体改进建议
        }
        // 4. 调用 AiClient 进行分析（dashscope / pai-eas，由 ai.provider 决定）
        String analysis;
        if (!aiClient.isReady()) {
            analysis = "";
        } else {
            try {
                analysis = aiClient.chat(
                        "You are an education assessment expert; analyze faithfully without fabrication.",
                        Arrays.asList(AiMessage.user(aiInput.toString())));
            } catch (Exception e) {
                analysis = "";
            }
        }

        // 5. 返回分析结果
        Map<String, Object> result = new HashMap<>();
        result.put("questionnaires", questionnairesData);
        result.put("analysis", analysis == null ? "" : analysis);

        return Result.success(result);
    }

    /**
     * 处理CSV文件批量分析
     * 通过统一的 AiClient 对上传的 CSV 数据进行批量分析。
     */
    @PostMapping("/teacher/bulk-analysis")
    public Result bulkAnalysis(@RequestParam("files[]") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "请上传至少一个CSV文件");
        }

        StringBuilder aiInput = new StringBuilder();
        aiInput.append("请分析以下CSV文件数据并给出专业的分析报告。注意全文用英文输出：\n\n");

        try {
            for (MultipartFile file : files) {
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                aiInput.append("文件名：").append(file.getOriginalFilename()).append("\n");
                aiInput.append("文件内容：\n").append(content).append("\n\n");
            }
        } catch (IOException e) {
            throw new ServiceException(Constants.CODE_500, "文件读取失败：" + e.getMessage());
        }

        if (!aiClient.isReady()) {
            return Result.success("AI service is currently unavailable. Please configure provider first.");
        }
        try {
            String analysis = aiClient.chat(
                    "You are a senior data analyst.",
                    Arrays.asList(AiMessage.user(aiInput.toString())));
            return Result.success(analysis == null ? "" : analysis);
        } catch (Exception e) {
            return Result.success("AI analysis failed: " + e.getMessage());
        }
    }
}

