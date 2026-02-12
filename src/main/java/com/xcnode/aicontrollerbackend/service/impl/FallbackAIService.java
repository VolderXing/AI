package com.xcnode.aicontrollerbackend.service.impl;

import com.xcnode.aicontrollerbackend.model.command.EngineCommand;
import com.xcnode.aicontrollerbackend.service.AIService;
import org.springframework.stereotype.Service;

@Service
public class FallbackAIService implements AIService {

    @Override
    public EngineCommand parseVoiceCommand(String voiceCommand) {
        System.out.println("⚠️ 使用备用AI服务解析指令: " + voiceCommand);
        return createFallbackCommand(voiceCommand);
    }

    @Override
    public String generateResponse(String command, int rpm) {
        return String.format("✅ 已执行指令 '%s'，当前转速 %d RPM", command, rpm);
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

    private EngineCommand createFallbackCommand(String command) {
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
}
