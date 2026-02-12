package com.xcnode.aicontrollerbackend.service;

import com.xcnode.aicontrollerbackend.model.command.EngineCommand;

public interface AIService {
    EngineCommand parseVoiceCommand(String voiceCommand);

    String generateResponse(String command, int rpm);

    boolean isCommandSafe(String command);
}
