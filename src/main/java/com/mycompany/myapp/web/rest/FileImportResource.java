package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.FileImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/import")
public class FileImportResource {

    private final Logger log = LoggerFactory.getLogger(FileImportResource.class);

    private final FileImportService fileImportService;

    public FileImportResource(FileImportService fileImportService) {
        this.fileImportService = fileImportService;
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> importProducts(@RequestParam("file") MultipartFile file) {
        log.debug("REST request to import products from file : {}", file.getOriginalFilename());
        try {
            fileImportService.importProducts(file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error importing products from file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/products-from-url")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> importProductsFromUrl(@RequestBody String url) { // Thêm @RequestBody String url
        log.debug("REST request to import products from URL : {}", url);
        try {
            fileImportService.importProductsFromUrl(url);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error importing products from URL", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/users")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> importUsers(@RequestParam("file") MultipartFile file) {
        log.debug("REST request to import users from file : {}", file.getOriginalFilename());
        try {
            fileImportService.importUsers(file);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error importing users from file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/users-from-url")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> importUsersFromUrl(@RequestBody String url) {
        log.debug("REST request to import users from URL : {}", url);
        try {
            fileImportService.importUsersFromUrl(url);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error importing users from URL", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
