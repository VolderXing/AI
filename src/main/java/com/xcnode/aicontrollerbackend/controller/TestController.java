package com.xcnode.aicontrollerbackend.controller;

import com.xcnode.aicontrollerbackend.model.command.EngineCommand;
import com.xcnode.aicontrollerbackend.service.AIService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
@Tag(name = "测试接口", description = "AI引擎控制测试相关接口")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TestController {

    private final AIService aiService;

    public TestController(AIService aiService) {
        this.aiService = aiService;
    }

    @Operation(
            summary = "获取服务状态",
            description = "返回当前服务的运行状态和基本信息",
            tags = {"健康检查"}
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "成功获取状态",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StatusResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "status": "running",
                                                "service": "DeepSeek AI Engine Control",
                                                "timestamp": 1700000000000
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "status", "running",
                "service", "DeepSeek AI Engine Control",
                "timestamp", System.currentTimeMillis()
        );
    }

    @Operation(
            summary = "测试语音命令",
            description = "接收语音命令文本，解析并返回AI处理结果",
            tags = {"AI处理"}
    )
    @Parameters({
            @Parameter(
                    name = "command",
                    description = "语音命令文本",
                    required = true,
                    example = "增加转速到3000转",
                    schema = @Schema(type = "string", minLength = 1, maxLength = 500)
            )
    })
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "命令处理成功",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommandResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "success": true,
                                                "command": "增加转速到3000转",
                                                "rpm": 3000.0,
                                                "action": "increase_rpm",
                                                "reason": "用户明确要求增加转速",
                                                "confidence": 0.95,
                                                "isSafe": true,
                                                "aiResponse": "已将发动机转速调整至3000转每分钟。",
                                                "timestamp": 1700000000000
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "参数错误",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "success": false,
                                                "error": "命令参数不能为空",
                                                "timestamp": 1700000000000
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "服务器内部错误",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "success": false,
                                                "error": "AI服务处理失败",
                                                "timestamp": 1700000000000
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/command")
    public Map<String, Object> testCommand(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "请求体",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommandRequest.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                                "command": "增加转速到3000转"
                                            }
                                            """
                            )
                    )
            )
            @RequestBody Map<String, String> request) {
        log.info("Received request: {}", request);
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

    // 定义 Schema 类（可放在单独的包中）
    @Schema(name = "StatusResponse", description = "状态响应")
    public static class StatusResponse {
        @Schema(description = "服务状态", example = "running")
        private String status;

        @Schema(description = "服务名称", example = "DeepSeek AI Engine Control")
        private String service;

        @Schema(description = "时间戳", example = "1700000000000")
        private Long timestamp;
    }

    @Schema(name = "CommandRequest", description = "命令请求")
    public static class CommandRequest {
        @Schema(description = "语音命令", requiredMode = Schema.RequiredMode.REQUIRED, example = "增加转速到3000转")
        private String command;
    }

    @Schema(name = "CommandResponse", description = "命令响应")
    public static class CommandResponse {
        @Schema(description = "是否成功", example = "true")
        private Boolean success;

        @Schema(description = "原始命令", example = "增加转速到3000转")
        private String command;

        @Schema(description = "转速值", example = "3000.0")
        private Double rpm;

        @Schema(description = "动作指令", example = "increase_rpm")
        private String action;

        @Schema(description = "解析理由", example = "用户明确要求增加转速")
        private String reason;

        @Schema(description = "置信度", example = "0.95")
        private Double confidence;

        @Schema(description = "是否安全", example = "true")
        private Boolean isSafe;

        @Schema(description = "AI响应文本", example = "已将发动机转速调整至3000转每分钟。")
        private String aiResponse;

        @Schema(description = "错误信息", example = "命令解析失败")
        private String error;

        @Schema(description = "时间戳", example = "1700000000000")
        private Long timestamp;
    }
}
