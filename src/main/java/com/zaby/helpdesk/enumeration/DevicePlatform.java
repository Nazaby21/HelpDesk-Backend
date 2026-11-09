package com.zaby.helpdesk.enumeration;

public enum DevicePlatform {
    ANDROID,
    IOS,
    WEB;

    public static DevicePlatform fromString(String value) {
        if (value == null || value.isBlank()) {
            return ANDROID;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "IOS", "IPHONE", "IPAD", "APPLE" -> IOS;
            case "WEB", "BROWSER" -> WEB;
            default -> ANDROID;
        };
    }
}
