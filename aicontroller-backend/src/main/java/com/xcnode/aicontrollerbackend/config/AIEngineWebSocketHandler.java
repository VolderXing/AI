package com.xcnode.aicontrollerbackend.config;

import com.alibaba.fastjson.JSON;
import com.xcnode.aicontrollerbackend.model.command.EngineCommand;
import com.xcnode.aicontrollerbackend.service.AIService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
@Component
public class AIEngineWebSocketHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final AIService aiService;

    public AIEngineWebSocketHandler(AIService aiService) {
        this.aiService = aiService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("🔌 WebSocket连接建立: " + session.getId());

        // 发送欢迎消息
        sendMessage(session, createMessage("system", Map.of(
                "message", "🚀 AI发动机控制系统已就绪",
                "timestamp", System.currentTimeMillis()
        )));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String voiceCommand = message.getPayload().trim();
        System.out.println("🎤 收到语音指令: " + voiceCommand);

        if (voiceCommand.isEmpty()) {
            sendMessage(session, createMessage("error", "指令不能为空"));
            return;
        }

        // 发送处理中状态
        broadcastMessage(createMessage("processing", Map.of(
                "command", voiceCommand,
                "status", "AI正在分析指令..."
        )));

        try {
            // 安全检查
            if (!aiService.isCommandSafe(voiceCommand)) {
                broadcastMessage(createMessage("safety_warning", Map.of(
                        "command", voiceCommand,
                        "message", "⚠️ 安全系统阻止执行该指令",
                        "reason", "指令可能存在安全风险"
                )));
                return;
            }

            // AI解析指令
            EngineCommand engineCommand = aiService.parseVoiceCommand(voiceCommand);

            // 验证AI解析结果的安全性
            if (!engineCommand.isSafe()) {
                broadcastMessage(createMessage("safety_warning", Map.of(
                        "command", voiceCommand,
                        "message", "⚠️ AI安全检测未通过",
                        "reason", engineCommand.getReason(),
                        "suggestedRpm", 800
                )));
                return;
            }

            // 生成AI响应
            String aiResponse = aiService.generateResponse(voiceCommand, engineCommand.getRpm());

            // 广播成功结果
            Map<String, Object> successData = Map.of(
                    "rpm", engineCommand.getRpm(),
                    "command", voiceCommand,
                    "action", engineCommand.getAction(),
                    "reason", engineCommand.getReason(),
                    "confidence", engineCommand.getConfidence(),
                    "aiResponse", aiResponse,
                    "timestamp", System.currentTimeMillis()
            );

            broadcastMessage(createMessage("success", successData));

            System.out.printf("✅ 指令解析完成: %s -> %d RPM (置信度: %.2f)%n",
                    voiceCommand, engineCommand.getRpm(), engineCommand.getConfidence());

        } catch (Exception e) {
            System.err.println("❌ 处理指令异常: " + e.getMessage());
            broadcastMessage(createMessage("error", Map.of(
                    "command", voiceCommand,
                    "message", "😞 系统处理失败，请稍后重试"
            )));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("🔌 WebSocket连接关闭: " + session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("💥 WebSocket传输错误: " + exception.getMessage());
        sessions.remove(session);
    }

    private void broadcastMessage(String message) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                sendMessage(session, message);
            }
        }
    }

    private void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            System.err.println("❌ 发送WebSocket消息失败: " + e.getMessage());
        }
    }

    private String createMessage(String type, Object data) {
        Map<String, Object> message = Map.of(
                "type", type,
                "data", data,
                "timestamp", System.currentTimeMillis()
        );
        return JSON.toJSONString(message);
    }
}