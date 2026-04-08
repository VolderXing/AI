package com.xcnode.aicontrollerbackend.controller;

import com.xcnode.aicontrollerbackend.model.command.EngineCommand;
import com.xcnode.aicontrollerbackend.service.AIService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    private final AIService aiService;

    public TestController(AIService aiService) {
        this.aiService = aiService;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "status", "running",
                "service", "DeepSeek AI Engine Control",
                "timestamp", System.currentTimeMillis()
        );
    }

    @PostMapping("/command")
    public Map<String, Object> testCommand(@RequestBody Map<String, String> request) {
        String command = request.get("command");

        try {
            EngineCommand result = aiService.parseVoiceCommand(command);
            String response = aiService.generateResponse(command, result.getRpm());

            return Map.of(
                    "success", true,
                    "command", command,
                    "rpm", result.getRpm(),
                    "action", result.getAction(),
                    "reason", result.getReason(),
                    "confidence", result.getConfidence(),
                    "isSafe", result.isSafe(),
                    "aiResponse", response,
                    "timestamp", System.currentTimeMillis()
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            );
        }
    }
}