package com.xcnode.aicontrollerbackend.service.impl;

import com.xcnode.aicontrollerbackend.model.command.EngineCommand;
import com.xcnode.aicontrollerbackend.service.AIService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Primary
public class DeepSeekAIService implements AIService {

    private final WebClient webClient;
    private final String model;
    private final int timeout;

    public DeepSeekAIService(
            @Value("${ai.deepseek.api-key}") String apiKey,
            @Value("${ai.deepseek.base-url}") String baseUrl,
            @Value("${ai.deepseek.model:deepseek-chat}") String model,
            @Value("${ai.deepseek.timeout:30}") int timeout) {

        this.model = model;
        this.timeout = timeout;

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        System.out.println("🚀 DeepSeek AI服务初始化完成");
    }

    @Override
    public EngineCommand parseVoiceCommand(String voiceCommand) {
        System.out.println("🔍 解析语音指令: " + voiceCommand);

        try {
            String response = callDeepSeekAPI(createParsePrompt(voiceCommand));
            return parseAPIResponse(response, voiceCommand);
        } catch (Exception e) {
            System.err.println("❌ DeepSeek API调用失败: " + e.getMessage());
            return createFallbackCommand(voiceCommand);
        }
    }

    private String callDeepSeekAPI(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        requestBody.put("max_tokens", 1000);
        requestBody.put("temperature", 0.3);
        requestBody.put("stream", false);

        return webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeout))
                .onErrorResume(e -> {
                    System.err.println("💥 DeepSeek请求失败: " + e.getMessage());
                    return Mono.just("{\"error\": \"API调用失败: " + e.getMessage() + "\"}");
                })
                .block();
    }

    private String createParsePrompt(String voiceCommand) {
        return """
            你是一个智能发动机控制系统。请分析用户的语音指令，确定合适的发动机转速。
            
            指令: "%s"
            
            转速范围: 0-8000 RPM
            安全准则: 不允许超过8000 RPM
            
            请以JSON格式返回分析结果:
            {
                "rpm": 数字,           // 0-8000之间的转速值
                "action": "字符串",     // 动作描述，如"加速到中速"
                "reason": "字符串",     // 决策原因
                "confidence": 0.95,    // 置信度 0-1
                "isSafe": true        // 是否安全
            }
            
            常见指令参考:
            - 停止/关机/熄火: 0 RPM
            - 启动/点火/怠速: 800 RPM
            - 低速/慢速: 1500 RPM
            - 中速/中等: 3000 RPM
            - 高速/快速: 5000 RPM
            - 最大/最高/极限: 8000 RPM
            - 经济模式/省油: 2000 RPM
            - 运动模式: 6000 RPM
            - 直接说数字如"2500": 2500 RPM
            
            请确保响应是纯JSON格式，不要有其他文本。
            """.formatted(voiceCommand);
    }

    private EngineCommand parseAPIResponse(String response, String originalCommand) {
        try {
            JSONObject responseJson = JSON.parseObject(response);

            // 检查是否有错误
            if (responseJson.containsKey("error")) {
                System.err.println("❌ DeepSeek返回错误: " + responseJson.getString("error"));
                return createFallbackCommand(originalCommand);
            }

            JSONArray choices = responseJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                String content = message.getString("content");

                System.out.println("📄 DeepSeek原始响应: " + content);

                // 提取JSON部分
                String jsonStr = extractJsonFromResponse(content);
                JSONObject commandData = JSON.parseObject(jsonStr);

                return new EngineCommand(
                        commandData.getIntValue("rpm"),
                        commandData.getString("action"),
                        commandData.getString("reason"),
                        commandData.getDoubleValue("confidence"),
                        commandData.getBooleanValue("isSafe")
                );
            }
        } catch (Exception e) {
            System.err.println("❌ 解析DeepSeek响应失败: " + e.getMessage());
        }

        return createFallbackCommand(originalCommand);
    }

    private String extractJsonFromResponse(String content) {
        // 查找第一个 { 和最后一个 }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}') + 1;

        if (start >= 0 && end > start) {
            return content.substring(start, end);
        }

        return content;
    }

    private EngineCommand createFallbackCommand(String command) {
        System.out.println("🔄 使用备用逻辑解析: " + command);

        String lowerCommand = command.toLowerCase();

        if (lowerCommand.contains("停止") || lowerCommand.contains("关机") || lowerCommand.contains("熄火")) {
            return new EngineCommand(0, "停止发动机", "用户要求停止", 0.9, true);
        } else if (lowerCommand.contains("启动") || lowerCommand.contains("点火") || lowerCommand.contains("怠速")) {
            return new EngineCommand(800, "启动发动机", "怠速运行", 0.9, true);
        } else if (lowerCommand.contains("低速") || lowerCommand.contains("慢速")) {
            return new EngineCommand(1500, "低速运行", "经济速度", 0.8, true);
        } else if (lowerCommand.contains("中速") || lowerCommand.contains("中等")) {
            return new EngineCommand(3000, "中速运行", "平衡性能", 0.8, true);
        } else if (lowerCommand.contains("高速") || lowerCommand.contains("快速")) {
            return new EngineCommand(5000, "高速运行", "高性能", 0.8, true);
        } else if (lowerCommand.contains("最大") || lowerCommand.contains("最高") || lowerCommand.contains("极限")) {
            return new EngineCommand(8000, "极限速度", "最大功率", 0.9, true);
        } else if (lowerCommand.contains("经济") || lowerCommand.contains("省油")) {
            return new EngineCommand(2000, "经济模式", "优化燃油效率", 0.8, true);
        } else if (lowerCommand.contains("运动")) {
            return new EngineCommand(6000, "运动模式", "高性能输出", 0.8, true);
        }

        // 尝试提取数字
        try {
            String numbers = lowerCommand.replaceAll("[^0-9]", "");
            if (!numbers.isEmpty()) {
                int rpm = Integer.parseInt(numbers);
                rpm = Math.min(rpm, 8000);
                boolean isSafe = rpm <= 8000;
                return new EngineCommand(rpm, "设定转速", "用户指定转速", 0.95, isSafe);
            }
        } catch (NumberFormatException e) {
            // 忽略解析错误
        }

        // 默认安全响应
        return new EngineCommand(800, "安全模式", "无法理解指令，使用安全默认值", 0.5, true);
    }

    @Override
    public String generateResponse(String command, int rpm) {
        String prompt = """
            用户指令: "%s"
            执行结果: 发动机转速设置为 %d RPM
            
            请生成一个自然、友好的中文响应，告知用户当前状态。
            要求: 简洁明了，直接说明结果，带一点人性化语气。
            响应长度: 不超过50字
            """.formatted(command, rpm);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            requestBody.put("max_tokens", 100);
            requestBody.put("temperature", 0.7);

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeout))
                    .block();

            return extractContentFromResponse(response);
        } catch (Exception e) {
            System.err.println("❌ 生成响应失败: " + e.getMessage());
            return String.format("✅ 已执行指令 '%s'，当前转速 %d RPM", command, rpm);
        }
    }

    private String extractContentFromResponse(String response) {
        try {
            JSONObject responseJson = JSON.parseObject(response);
            JSONArray choices = responseJson.getJSONArray("choices");
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            return message.getString("content").trim();
        } catch (Exception e) {
            return "指令执行完成";
        }
    }

    @Override
    public boolean isCommandSafe(String command) {
        String lowerCommand = command.toLowerCase();

        // 基础安全检查
        if (lowerCommand.contains("爆炸") || lowerCommand.contains("破坏") ||
                lowerCommand.contains("损坏") || lowerCommand.contains("危险")) {
            return false;
        }

        // 检查极端转速要求
        if (lowerCommand.contains("10000") || lowerCommand.contains("9000") ||
                lowerCommand.contains("一万") || lowerCommand.contains("九千")) {
            return false;
        }

        return true;
    }
}