package com.xcnode.aicontrollerbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private AIEngineWebSocketHandler aiEngineWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 匹配前端的 '/engine-websocket' 路径
        registry.addHandler(aiEngineWebSocketHandler, "/engine-websocket")
                .setAllowedOrigins("http://localhost:3000");  // 只允许前端3000端口;
    }
}
