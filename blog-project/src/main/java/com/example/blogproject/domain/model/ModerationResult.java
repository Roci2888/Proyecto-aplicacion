package com.example.blogproject.domain.model;

public class ModerationResult {

    private final boolean allowed;
    private final String reasonCode;
    private final String userMessage;
    private final String debugMessage;

    public ModerationResult(boolean allowed, String reasonCode, String userMessage, String debugMessage) {
        this.allowed = allowed;
        this.reasonCode = reasonCode;
        this.userMessage = userMessage;
        this.debugMessage = debugMessage;
    }

    public static ModerationResult allow() {
        return new ModerationResult(
                true,
                "OK",
                "Contenido permitido.",
                "La imagen ha pasado la moderación."
        );
    }

    public static ModerationResult reject(String reasonCode, String userMessage, String debugMessage) {
        return new ModerationResult(
                false,
                reasonCode,
                userMessage,
                debugMessage
        );
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getReasonCode() {
        return reasonCode;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public String getDebugMessage() {
        return debugMessage;
    }
}
