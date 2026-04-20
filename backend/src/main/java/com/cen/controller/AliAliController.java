package com.cen.controller;

// 导入阿里云通义千问AI相关依赖
import com.alibaba.dashscope.aigc.generation.Generation; // 通义千问AI生成服务核心类
import com.alibaba.dashscope.aigc.generation.GenerationParam; // AI参数配置类
import com.alibaba.dashscope.aigc.generation.GenerationResult; // AI返回结果类
import com.alibaba.dashscope.common.Message; // AI消息体
import com.alibaba.dashscope.common.Role; // AI角色定义（用户/助手）
import com.alibaba.dashscope.exception.InputRequiredException; // 输入参数异常
import com.alibaba.dashscope.exception.NoApiKeyException; // API密钥异常
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;

/**
 * 阿里云通义千问AI控制器
 * 提供基础的AI文本生成接口，作为系统中AI功能的测试入口
 */
@RestController
@RequestMapping("ai/")
public class AliAliController {

    @Value("${ai-api-key}") // 从application.yml中注入AI API密钥
    private String apiKey; // 阿里云通义千问API密钥

    @Resource // 注入在AliAiConfig中配置的Generation Bean
    private Generation generation; // 通义千问AI生成服务实例


    /**
     * 通义千问AI测试接口
     * 这是一个基础的AI调用示例，用于测试通义千问AI的功能
     * 前端可以直接发送文本内容，后端将内容发送给AI并返回AI的回复
     * 
     * @param content 用户输入的文本内容，作为AI的提示词
     * @return AI生成的回复内容
     * @throws NoApiKeyException API密钥不存在或无效时抛出
     * @throws InputRequiredException 输入参数缺失或格式错误时抛出
     */
    @PostMapping(value = "aliTyqw")
    public String send(@RequestBody String content) throws NoApiKeyException, InputRequiredException {
        // 构建用户消息对象
        // 在通义千问API中，对话是以消息列表形式传递的，每条消息包含角色(role)和内容(content)
        // 这里创建一个用户(USER)角色的消息，内容为用户输入的文本
        Message userMessage = Message.builder()
                .role(Role.USER.getValue()) // 设置消息角色为用户
                .content(content) // 设置消息内容为用户输入
                .build();

        // 构建AI生成参数
        GenerationParam param = GenerationParam.builder()
                // 指定使用的模型，qwen-turbo是通义千问的高效模型，适合一般对话场景
                .model("qwen-turbo")
                .messages(Arrays.asList(userMessage)) // 设置消息历史，这里只有一条用户消息
                // 设置返回格式为消息格式
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                // topP参数控制生成文本的多样性
                // 取值范围为(0,1.0)，值越大生成的随机性越高；值越低生成的确定性越高
                // 0.8是一个平衡的值，既保证回答的多样性，又不会过于发散
                .topP(0.8)
                // 设置API密钥，从application.yml中注入
                .apiKey(apiKey)
                // 启用互联网搜索功能
                // 开启后模型可以获取最新的互联网信息作为参考
                // 模型会根据问题自行判断是否需要使用搜索结果
                .enableSearch(true)
                .build();
        // 调用通义千问AI接口获取生成结果
        GenerationResult generationResult = generation.call(param);
        
        // 从结果中提取AI生成的文本内容并返回
        // getOutput()获取输出对象
        // getChoices()获取所有生成的候选回复列表
        // get(0)获取第一个候选回复(通常只有一个)
        // getMessage()获取回复的消息对象
        // getContent()获取消息的文本内容
        return generationResult.getOutput().getChoices().get(0).getMessage().getContent();
    }

}