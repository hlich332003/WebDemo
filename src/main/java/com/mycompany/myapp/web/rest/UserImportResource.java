package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.service.UserImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class UserImportResource {

    private final Logger log = LoggerFactory.getLogger(UserImportResource.class);

    private final UserImportService userImportService;

    public UserImportResource(UserImportService userImportService) {
        this.userImportService = userImportService;
    }

    /**
     * POST /api/admin/users/import : Import users từ file Excel
     *
     * @param file file Excel chứa danh sách user
     * @return ResponseEntity với kết quả import
     */
    @PostMapping("/users/import")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> importUsers(@RequestParam("file") MultipartFile file) {
        log.info("REST request to import users from Excel file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "File không được để trống");
            return ResponseEntity.badRequest().body(error);
        }

        // Kiểm tra định dạng file
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Chỉ chấp nhận file Excel (.xlsx, .xls)");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            UserImportService.UserImportResult result = userImportService.importUsersFromExcel(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Import thành công");
            response.put("created", result.getCreated());
            response.put("updated", result.getUpdated());
            response.put("totalProcessed", result.getTotalProcessed());
            response.put("errors", result.getErrors());
            response.put("warnings", result.getWarnings());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Lỗi khi đọc file Excel", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi khi đọc file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            log.error("Lỗi khi import users", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Lỗi khi import: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}

