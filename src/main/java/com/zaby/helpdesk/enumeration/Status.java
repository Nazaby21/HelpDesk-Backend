package com.zaby.helpdesk.enumeration;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Status {
    PENDING,
    IN_PROGRESS,
    COMPLETED;

    @JsonCreator
    public static Status fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        if ("RESOLVED".equals(normalized)) {
            return COMPLETED;
        }
        return Status.valueOf(normalized);
    }
}
