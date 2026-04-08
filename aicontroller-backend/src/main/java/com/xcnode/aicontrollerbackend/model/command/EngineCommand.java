package com.xcnode.aicontrollerbackend.model.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EngineCommand {
    private int rpm;
    private String action;
    private String reason;
    private double confidence;
    private boolean isSafe;

    // 便捷构造方法
    public EngineCommand(int rpm, String action, String reason) {
        this.rpm = rpm;
        this.action = action;
        this.reason = reason;
        this.confidence = 0.9;
        this.isSafe = true;
    }
}