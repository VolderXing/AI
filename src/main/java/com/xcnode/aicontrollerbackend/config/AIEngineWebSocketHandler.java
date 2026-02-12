package com.xcnode.aicontrollerbackend.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.xcnode.aicontrollerbackend.model.command.EngineCommand;
import com.xcnode.aicontrollerbackend.service.AIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AIEngineWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    @Qualifier("deepSeekAIService")
    private AIService deepSeekAIService;

    @Autowired
    @Qualifier("fallbackAIService")
    private AIService fallbackAIService;

    @Autowired
    @Qualifier("localAIService")
    private AIService localAIService;

    // 在线会话管理
    private static final ConcurrentHashMap<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

    // 当前使用的AI服务类型
    private String currentAIType = "deepseek";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        SESSIONS.put(sessionId, session);
        int count = ONLINE_COUNT.incrementAndGet();

        System.out.println("✅ WebSocket连接建立 - SessionID: " + sessionId + "，当前在线: " + count);

        // 发送连接成功消息 - 适配前端格式
        JSONObject welcomeMsg = new JSONObject();
        welcomeMsg.put("type", "system");

        JSONObject data = new JSONObject();
        data.put("message", "✅ WebSocket连接成功");
        data.put("timestamp", System.currentTimeMillis());

        welcomeMsg.put("data", data);

        session.sendMessage(new TextMessage(welcomeMsg.toJSONString()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("📩 收到WebSocket消息: " + payload);

        try {
            // 先尝试按 JSON 解析
            JSONObject jsonMsg = JSON.parseObject(payload);
            String type = jsonMsg.getString("type");

            switch (type) {
                case "voice_command":
                    handleVoiceCommand(session, jsonMsg);
                    break;
                case "switch_ai":
                    handleSwitchAI(session, jsonMsg);
                    break;
                case "ping":
                    handlePing(session);
                    break;
                default:
                    sendErrorMessage(session, "未知的消息类型: " + type);
            }
        } catch (JSONException e) {
            // 如果不是 JSON，按纯文本指令处理
            System.out.println("📝 收到纯文本指令: " + payload);
            handlePlainTextCommand(session, payload);
        }
    }

    /**
     * 处理纯文本指令
     */
    private void handlePlainTextCommand(WebSocketSession session, String command) throws IOException {
        // 1. 安全检查
        AIService aiService = getCurrentAIService();
        if (!aiService.isCommandSafe(command)) {
            JSONObject result = new JSONObject();
            result.put("type", "command_result");

            JSONObject data = new JSONObject();
            data.put("success", false);
            data.put("message", "指令存在安全风险，已被拒绝执行");
            data.put("command", command);
            data.put("timestamp", System.currentTimeMillis());

            result.put("data", data);
            session.sendMessage(new TextMessage(result.toJSONString()));
            return;
        }

        // 2. 解析指令
        EngineCommand engineCommand = aiService.parseVoiceCommand(command);

        // 3. 生成响应
        String response = aiService.generateResponse(command, engineCommand.getRpm());

        // 4. 发送结果
        JSONObject result = new JSONObject();
        result.put("type", "command_result");

        JSONObject data = new JSONObject();
        data.put("success", true);
        data.put("command", command);
        data.put("engineCommand", JSON.toJSON(engineCommand));
        data.put("message", response);
        data.put("aiType", currentAIType);
        data.put("timestamp", System.currentTimeMillis());

        result.put("data", data);

        session.sendMessage(new TextMessage(result.toJSONString()));

        System.out.println("📤 指令执行结果 - RPM: " + engineCommand.getRpm() +
                ", 动作: " + engineCommand.getAction() +
                ", AI: " + currentAIType);
    }

    private void handleVoiceCommand(WebSocketSession session, JSONObject jsonMsg) throws IOException {
        String command = jsonMsg.getString("command");
        if (command == null || command.trim().isEmpty()) {
            sendErrorMessage(session, "指令不能为空");
            return;
        }

        // 1. 安全检查
        AIService aiService = getCurrentAIService();
        if (!aiService.isCommandSafe(command)) {
            JSONObject result = new JSONObject();
            result.put("type", "command_result");

            JSONObject data = new JSONObject();
            data.put("success", false);
            data.put("message", "指令存在安全风险，已被拒绝执行");
            data.put("command", command);
            data.put("timestamp", System.currentTimeMillis());

            result.put("data", data);
            session.sendMessage(new TextMessage(result.toJSONString()));
            return;
        }

        // 2. 解析指令
        EngineCommand engineCommand = aiService.parseVoiceCommand(command);

        // 3. 生成响应
        String response = aiService.generateResponse(command, engineCommand.getRpm());

        // 4. 发送结果 - 适配前端格式
        JSONObject result = new JSONObject();
        result.put("type", "command_result");

        JSONObject data = new JSONObject();
        data.put("success", true);
        data.put("command", command);
        data.put("engineCommand", JSON.toJSON(engineCommand));
        data.put("message", response);
        data.put("aiType", currentAIType);
        data.put("timestamp", System.currentTimeMillis());

        result.put("data", data);

        session.sendMessage(new TextMessage(result.toJSONString()));

        System.out.println("📤 指令执行结果 - RPM: " + engineCommand.getRpm() +
                ", 动作: " + engineCommand.getAction() +
                ", AI: " + currentAIType);
    }

    private void handleSwitchAI(WebSocketSession session, JSONObject jsonMsg) throws IOException {
        String aiType = jsonMsg.getString("aiType");
        if (aiType == null || aiType.trim().isEmpty()) {
            sendErrorMessage(session, "AI类型不能为空");
            return;
        }

        String oldType = currentAIType;
        currentAIType = aiType;

        JSONObject result = new JSONObject();
        result.put("type", "system");

        JSONObject data = new JSONObject();
        data.put("message", "🔧 AI服务已切换: " + oldType + " → " + aiType);
        data.put("aiType", currentAIType);
        data.put("timestamp", System.currentTimeMillis());

        result.put("data", data);

        session.sendMessage(new TextMessage(result.toJSONString()));
        System.out.println("🔄 AI服务切换: " + oldType + " -> " + aiType);
    }

    private void handlePing(WebSocketSession session) throws IOException {
        JSONObject pong = new JSONObject();
        pong.put("type", "pong");

        JSONObject data = new JSONObject();
        data.put("timestamp", System.currentTimeMillis());

        pong.put("data", data);
        session.sendMessage(new TextMessage(pong.toJSONString()));
    }

    private void sendErrorMessage(WebSocketSession session, String errorMsg) throws IOException {
        JSONObject error = new JSONObject();
        error.put("type", "error");

        JSONObject data = new JSONObject();
        data.put("message", errorMsg);
        data.put("timestamp", System.currentTimeMillis());

        error.put("data", data);
        session.sendMessage(new TextMessage(error.toJSONString()));
    }

    private AIService getCurrentAIService() {
        switch (currentAIType.toLowerCase()) {
            case "deepseek":
                return deepSeekAIService;
            case "local":
                return localAIService;
            case "fallback":
            default:
                return fallbackAIService;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        SESSIONS.remove(sessionId);
        int count = ONLINE_COUNT.decrementAndGet();

        System.out.println("❌ WebSocket连接关闭 - SessionID: " + sessionId +
                "，状态: " + status + "，当前在线: " + count);

        // 只在session还开着时发送消息
        if (session.isOpen()) {
            try {
                JSONObject closeMsg = new JSONObject();
                closeMsg.put("type", "system");

                JSONObject data = new JSONObject();
                data.put("message", "🔌 WebSocket连接已关闭");
                data.put("timestamp", System.currentTimeMillis());

                closeMsg.put("data", data);
                session.sendMessage(new TextMessage(closeMsg.toJSONString()));
            } catch (Exception e) {
                // 忽略发送失败
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("⚠️ WebSocket传输错误 - SessionID: " + session.getId() +
                "，错误: " + exception.getMessage());

        // 发送错误消息
        JSONObject errorMsg = new JSONObject();
        errorMsg.put("type", "error");

        JSONObject data = new JSONObject();
        data.put("message", "💥 WebSocket连接错误");
        data.put("timestamp", System.currentTimeMillis());

        errorMsg.put("data", data);

        if (session.isOpen()) {
            session.sendMessage(new TextMessage(errorMsg.toJSONString()));
        }
    }

    // 静态方法：广播消息给所有在线客户端
    public static void broadcastMessage(String message) {
        JSONObject msg = new JSONObject();
        msg.put("type", "broadcast");

        JSONObject data = new JSONObject();
        data.put("message", message);
        data.put("timestamp", System.currentTimeMillis());

        msg.put("data", data);

        SESSIONS.values().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(msg.toJSONString()));
                }
            } catch (IOException e) {
                System.err.println("❌ 广播消息失败: " + e.getMessage());
            }
        });
    }

    // 获取在线人数
    public static int getOnlineCount() {
        return ONLINE_COUNT.get();
    }
}