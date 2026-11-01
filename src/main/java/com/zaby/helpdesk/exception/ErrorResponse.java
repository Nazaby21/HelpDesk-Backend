package com.zaby.helpdesk.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String code;
    private String message;
    private List<FieldErrorDetail> fieldErrors = new ArrayList<>();

    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.code = resolveCode(status);
        this.fieldErrors = new ArrayList<>();
    }

    public ErrorResponse(int status, String code, String message, LocalDateTime timestamp) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.timestamp = timestamp;
        this.fieldErrors = new ArrayList<>();
    }

    private static String resolveCode(int status) {
        try {
            return HttpStatus.valueOf(status).name();
        } catch (Exception e) {
            return "ERROR";
        }
    }
}
