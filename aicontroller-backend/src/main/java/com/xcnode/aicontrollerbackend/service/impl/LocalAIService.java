package com.xcnode.aicontrollerbackend.service.impl;

import com.xcnode.aicontrollerbackend.model.command.EngineCommand;
import com.xcnode.aicontrollerbackend.service.AIService;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocalAIService implements AIService {

    private final Random random = new Random();

    @Override
    public EngineCommand parseVoiceCommand(String voiceCommand) {
        System.out.println("🔧 使用本地AI服务解析指令: " + voiceCommand);

        String lowerCommand = voiceCommand.toLowerCase();

        // 1. 提取数字转速
        Pattern pattern = Pattern.compile("\\b(\\d+)\\b");
        Matcher matcher = pattern.matcher(lowerCommand);

        if (matcher.find()) {
            try {
                int rpm = Integer.parseInt(matcher.group(1));
                rpm = Math.min(Math.max(rpm, 0), 8000);
                return new EngineCommand(
                        rpm,
                        "设定转速",
                        "从指令中识别到数字: " + rpm,
                        0.85,
                        rpm <= 8000
                );
            } catch (NumberFormatException e) {
                // 忽略
            }
        }

        // 2. 关键词匹配
        if (lowerCommand.contains("停止") || lowerCommand.contains("关机") || lowerCommand.contains("熄火")) {
            return new EngineCommand(0, "停止发动机", "关键词匹配: 停止", 0.8, true);
        } else if (lowerCommand.contains("启动") || lowerCommand.contains("点火") || lowerCommand.contains("怠速")) {
            return new EngineCommand(800, "启动发动机", "关键词匹配: 启动", 0.8, true);
        } else if (lowerCommand.contains("低速") || lowerCommand.contains("慢速")) {
            return new EngineCommand(1500, "低速运行", "关键词匹配: 低速", 0.75, true);
        } else if (lowerCommand.contains("中速") || lowerCommand.contains("中等")) {
            return new EngineCommand(3000, "中速运行", "关键词匹配: 中速", 0.75, true);
        } else if (lowerCommand.contains("高速") || lowerCommand.contains("快速")) {
            return new EngineCommand(5000, "高速运行", "关键词匹配: 高速", 0.75, true);
        } else if (lowerCommand.contains("最大") || lowerCommand.contains("最高") || lowerCommand.contains("极限")) {
            return new EngineCommand(8000, "极限速度", "关键词匹配: 极限", 0.8, true);
        } else if (lowerCommand.contains("经济") || lowerCommand.contains("省油")) {
            return new EngineCommand(2000, "经济模式", "关键词匹配: 经济", 0.7, true);
        } else if (lowerCommand.contains("运动")) {
            return new EngineCommand(6000, "运动模式", "关键词匹配: 运动", 0.7, true);
        }

        // 3. 随机演示模式（用于测试）
        if (lowerCommand.contains("演示") || lowerCommand.contains("测试") || lowerCommand.contains("随机")) {
            int rpm = random.nextInt(9) * 1000;
            return new EngineCommand(
                    rpm,
                    "演示模式",
                    "随机生成转速用于测试",
                    0.6,
                    rpm <= 8000
            );
        }

        // 4. 默认返回
        return new EngineCommand(800, "安全模式", "无法识别指令，使用默认转速", 0.5, true);
    }

    @Override
    public String generateResponse(String command, int rpm) {
        // 简单响应生成
        if (rpm == 0) {
            return "发动机已停止";
        } else if (rpm < 1000) {
            return "发动机怠速运行中，当前转速 " + rpm + " RPM";
        } else if (rpm < 3000) {
            return "发动机低速运行，转速 " + rpm + " RPM";
        } else if (rpm < 5000) {
            return "发动机中速运行，转速 " + rpm + " RPM";
        } else if (rpm < 7000) {
            return "发动机高速运行，转速 " + rpm + " RPM";
        } else {
            return "发动机极限运行中，转速 " + rpm + " RPM，请注意安全";
        }
    }

    @Override
    public boolean isCommandSafe(String command) {
        String lowerCommand = command.toLowerCase();

        // 安全检查
        return !lowerCommand.contains("爆炸") &&
                !lowerCommand.contains("破坏") &&
                !lowerCommand.contains("损坏") &&
                !lowerCommand.contains("危险") &&
                !lowerCommand.contains("10000") &&
                !lowerCommand.contains("9000");
    }
}
