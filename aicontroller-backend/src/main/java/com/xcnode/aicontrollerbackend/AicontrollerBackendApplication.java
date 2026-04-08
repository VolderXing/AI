package com.xcnode.aicontrollerbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AicontrollerBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AicontrollerBackendApplication.class, args);
        System.out.println("🚀 AI发动机控制系统启动成功!");
        System.out.println("📍 访问: http://localhost:8080");
        System.out.println("🔌 WebSocket: ws://localhost:8080/engine-websocket");
    }

}
