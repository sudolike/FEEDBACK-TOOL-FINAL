package com.cen.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cen.common.Constants;
import com.cen.controller.dto.QuestionnaireResponseDetailDTO;
import com.cen.controller.dto.QuestionnaireResponseSummaryDTO;
import com.cen.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
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
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;

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

    @Value("${ai-api-key}")
    private String apiKey;

    @Resource
    private IQuestionnaireResponsesService questionnaireResponsesService;

    @Resource
    private Generation generation;

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
    // 查询课程问卷的填写情况列表
    @GetMapping("/FillinDetails")
    public Result getCourseQuestionnaireResponses(
            @RequestParam Long courseId,
            @RequestParam Long questionnaireId) {
        return Result.success(questionnaireResponsesService.getQuestionnaireFillinDetails(courseId, questionnaireId));
    }

    /**
     * AI分析问卷填写情况
     * 使用阿里通义千问AI对单个问卷的填写情况进行智能分析
     * 
     * @param courseId 课程ID
     * @param questionnaireId 问卷ID
     * @return 包含问卷信息、填写人数和AI分析结果的数据
     * @throws NoApiKeyException API密钥不存在或无效时抛出
     * @throws InputRequiredException 输入参数缺失或格式错误时抛出
     */
    @GetMapping("/analysis")
    public Result getQuestionnaireAnalysis(
            @RequestParam Long courseId,
            @RequestParam Long questionnaireId) throws NoApiKeyException, InputRequiredException {
        // 1. 获取问卷基本信息
        Questionnaires questionnaire = questionnairesMapper.selectById(questionnaireId);
        if (questionnaire == null) {
            throw new ServiceException(Constants.CODE_402,"问卷不存在");
        }

        // 2. 获取问卷的填写详情
        QuestionnaireResponseSummaryDTO responses = questionnaireResponsesService.getQuestionnaireResponseSummary(courseId, questionnaireId);
        
        // 3. 构建AI分析的输入内容（预设指令）
        // 这里构建了发送给AI的提示词(prompt)，包含问卷信息和要求
        StringBuilder aiInput = new StringBuilder();
        // 设置AI分析的主要任务和输出要求（使用英文输出）
        aiInput.append("请分析以下问卷调查数据并给出专业的分析报告。并全部用英文输出\n\n");
        // 添加问卷的基本信息
        aiInput.append("问卷标题：").append(questionnaire.getTitle()).append("\n");
        aiInput.append("问卷描述：").append(questionnaire.getDescription()).append("\n");
        aiInput.append("总问题数量：").append(responses.getTotalQuestions()).append("\n\n");
        aiInput.append("总填写人数：").append(responses.getTotalResponses()).append("\n\n");
        // 添加问卷题目数据，用于AI理解问题内容
        aiInput.append("题目数据：").append(responses.getQuestionnaire().getQuestions()).append("\n\n");
        // 添加所有答案数据，这是AI分析的核心数据
        aiInput.append("答案数据：\n").append(responses.getAnswers()).append("\n\n");;
        // 4. 调用阿里通义千问AI进行分析
        // 构建用户消息对象，包含我们构建的提示词
        Message userMessage = Message.builder()
                .role(Role.USER.getValue()) // 设置消息角色为用户
                .content(aiInput.toString()) // 设置消息内容为构建的提示词
                .build();

        // 配置AI生成参数
        GenerationParam param = GenerationParam.builder()
                .model("qwen-turbo") // 使用通义千问的高效模型
                .messages(Arrays.asList(userMessage)) // 设置消息历史
                .resultFormat(GenerationParam.ResultFormat.MESSAGE) // 设置返回格式为消息格式
                .topP(0.8) // 设置生成多样性参数
                .apiKey(apiKey) // 设置API密钥
                .enableSearch(true) // 启用互联网搜索，增强分析能力
                .build();

        GenerationResult generationResult = generation.call(param);
        String analysis = generationResult.getOutput().getChoices().get(0).getMessage().getContent();

        // 5. 返回分析结果
        Map<String, Object> result = new HashMap<>();
        result.put("questionnaire", questionnaire);
        result.put("totalResponses", responses.getTotalResponses());
        result.put("analysis", analysis);
        
        return Result.success(result);
    }

    /**
     * AI对比分析多个问卷填写情况
     * 使用阿里通义千问AI对多个问卷的填写情况进行对比分析
     * 支持自定义分析描述，指导AI关注特定分析维度
     * 
     * @param courseIds 课程ID列表
     * @param questionnaireIds 问卷ID列表
     * @param analysisDescription 可选的自定义分析描述，指导AI分析的方向和重点
     * @return 包含所有问卷信息和AI对比分析结果的数据
     * @throws NoApiKeyException API密钥不存在或无效时抛出
     * @throws InputRequiredException 输入参数缺失或格式错误时抛出
     */
    @GetMapping("/compareAnalysis")
    public Result compareQuestionnaireAnalysis(
            @RequestParam List<Long> courseIds,
            @RequestParam List<Long> questionnaireIds,
            @RequestParam(required = false) String analysisDescription) throws NoApiKeyException, InputRequiredException {
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
        System.out.println(aiInput.toString()+ "--------sa");
        // 4. 调用阿里通义千问AI进行分析
        // 构建用户消息对象，包含我们构建的提示词
        Message userMessage = Message.builder()
                .role(Role.USER.getValue()) // 设置消息角色为用户
                .content(aiInput.toString()) // 设置消息内容为构建的提示词
                .build();

        // 配置AI生成参数
        GenerationParam param = GenerationParam.builder()
                .model("qwen-turbo") // 使用通义千问的高效模型
                .messages(Arrays.asList(userMessage)) // 设置消息历史
                .resultFormat(GenerationParam.ResultFormat.MESSAGE) // 设置返回格式为消息格式
                .topP(0.8) // 设置生成多样性参数
                .apiKey(apiKey) // 设置API密钥
                .enableSearch(true) // 启用互联网搜索，增强分析能力
                .build();

        GenerationResult generationResult = generation.call(param);
        String analysis = generationResult.getOutput().getChoices().get(0).getMessage().getContent();

        // 5. 返回分析结果
        Map<String, Object> result = new HashMap<>();
        result.put("questionnaires", questionnairesData);
        result.put("analysis", analysis);
        
        return Result.success(result);
    }

    /**
     * 处理CSV文件批量分析
     * 使用阿里通义千问AI对上传的CSV文件数据进行批量分析
     * 适用于教师上传大量数据进行综合分析的场景
     * 
     * @param files 上传的CSV文件列表
     * @return AI分析结果
     * @throws NoApiKeyException API密钥不存在或无效时抛出
     * @throws InputRequiredException 输入参数缺失或格式错误时抛出
     * @throws ServiceException 文件处理过程中的异常
     */
    @PostMapping("/teacher/bulk-analysis")
    public Result bulkAnalysis(@RequestParam("files[]") List<MultipartFile> files) throws NoApiKeyException, InputRequiredException {
        if (files == null || files.isEmpty()) {
            throw new ServiceException(Constants.CODE_400, "请上传至少一个CSV文件");
        }

        // 构建CSV文件分析的AI输入内容（预设指令）
        StringBuilder aiInput = new StringBuilder();
        // 设置AI分析CSV数据的主要任务和输出要求（使用英文输出）
        aiInput.append("请分析以下CSV文件数据并给出专业的分析报告。注意全文用英文输出：\n\n");

        try {
            for (MultipartFile file : files) {
                String content = new String(file.getBytes(), StandardCharsets.UTF_8);
                aiInput.append("文件名：").append(file.getOriginalFilename()).append("\n");
                aiInput.append("文件内容：\n").append(content).append("\n\n");
            }

            // 调用AI进行分析
            Message userMessage = Message.builder()
                    .role(Role.USER.getValue())
                    .content(aiInput.toString())
                    .build();

            GenerationParam param = GenerationParam.builder()
                    .model("qwen-turbo")
                    .messages(Arrays.asList(userMessage))
                    .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                    .topP(0.8)
                    .apiKey(apiKey)
                    .enableSearch(true)
                    .build();

            GenerationResult generationResult = generation.call(param);
            String analysis = generationResult.getOutput().getChoices().get(0).getMessage().getContent();

            return Result.success(analysis);
        } catch (IOException e) {
            throw new ServiceException(Constants.CODE_500, "文件读取失败：" + e.getMessage());
        }
    }
}

