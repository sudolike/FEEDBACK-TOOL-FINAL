package com.cen.enums;

public enum QuestionnaireStatus {
    PENDING(0, "待发布"),
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成");

    private final int code;
    private final String description;

    QuestionnaireStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
} 