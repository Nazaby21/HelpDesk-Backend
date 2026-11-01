package com.zaby.helpdesk.controller;

import com.zaby.helpdesk.exception.ErrorResponse;
import com.zaby.helpdesk.service.SupabaseStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final SupabaseStorageService supabaseStorageService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "BAD_REQUEST", "File is missing", LocalDateTime.now()));
        }

        try {
            String publicUrl = supabaseStorageService.uploadFile(file);
            return ResponseEntity.ok(Map.of("url", publicUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_SERVER_ERROR", "Failed to upload image: " + e.getMessage(), LocalDateTime.now()));
        }
    }
}
