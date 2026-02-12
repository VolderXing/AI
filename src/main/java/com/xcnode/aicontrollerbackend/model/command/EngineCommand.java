package com.xcnode.aicontrollerbackend.model.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EngineCommand {
    private int rpm;
    private String action;
    private String reason;
    private double confidence;
    private boolean isSafe;
}
