package com.mycompany.myapp.web.rest;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * REST controller for uploading files (images) to database.
 */
@RestController
@RequestMapping("/api")
public class FileUploadResource {

    private final Logger log = LoggerFactory.getLogger(FileUploadResource.class);

    /**
     * POST  /upload/image : Upload product image to database
     *
     * @param file the image file to upload
     * @return the ResponseEntity with status 200 (OK) and the base64 data URL, or status 400 (Bad Request) if the file is invalid
     */
    @PostMapping("/upload/image")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        log.debug("REST request to upload image : {}", file.getOriginalFilename());

        try {
            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            // Validate file type (only images)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }

            // Validate file size (max 5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("error", "File size must not exceed 5MB"));
            }

            // Convert to byte array
            byte[] imageData = file.getBytes();

            // Convert to base64 data URL for immediate display
            String base64Data = Base64.getEncoder().encodeToString(imageData);
            String dataUrl = "data:" + contentType + ";base64," + base64Data;

            // Return data URL (will be stored in imageUrl field, and imageData will be extracted when saving product)
            Map<String, String> response = new HashMap<>();
            response.put("imageUrl", dataUrl);
            response.put("contentType", contentType);

            log.debug("Image uploaded successfully to memory");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of("error", "Failed to upload image: " + e.getMessage())
            );
        }
    }
}
