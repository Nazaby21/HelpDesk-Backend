package com.zaby.helpdesk.service;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String supabaseBucket;

    /**
     * Uploads a file to Supabase Storage and returns the public URL.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        // Generate a unique file name
        String fileName = UUID.randomUUID().toString() + extension;

        // Construct the Supabase Storage API URL for upload
        // POST /storage/v1/object/{bucketName}/{fileName}
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + supabaseBucket + "/" + fileName;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(uploadUrl);

            // Set Headers required by Supabase API
            httpPost.setHeader("Authorization", "Bearer " + supabaseKey);
            httpPost.setHeader("apikey", supabaseKey);

            // Create ByteArrayEntity from MultipartFile bytes
            byte[] fileBytes = file.getBytes();
            String contentTypeStr = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            ByteArrayEntity entity = new ByteArrayEntity(fileBytes, ContentType.parse(contentTypeStr));
            httpPost.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                if (response.getCode() >= 400) {
                    throw new IOException("Failed to upload to Supabase: HTTP " + response.getCode() + " " + response.getReasonPhrase());
                }
            }
        }

        return getPublicUrl(fileName);
    }

    /**
     * Returns the public URL for a given file name in the public bucket.
     */
    public String getPublicUrl(String fileName) {
        // Construct GET URL for a public bucket:
        return supabaseUrl + "/storage/v1/object/public/" + supabaseBucket + "/" + fileName;
    }
}
