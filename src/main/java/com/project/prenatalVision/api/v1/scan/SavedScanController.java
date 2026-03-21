package com.project.prenatalVision.api.v1.scan;

import com.project.prenatalVision.api.configuration.ApiResponse;
import com.project.prenatalVision.application.scan.SavedScanService;
import com.project.prenatalVision.domain.scan.ScanRecord;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/saved-scans")
@RequiredArgsConstructor
public class SavedScanController {

    private final SavedScanService savedScanService;

    @PostMapping
    public ApiResponse<Map<String, String>> saveScan(@Valid @RequestBody SaveScanRequest request) {
        try {
            String userEmail = getAuthenticatedEmail();
            ScanRecord saved = savedScanService.saveScan(userEmail, request);
            return ApiResponse.success(Map.of("id", saved.getId()));
        } catch (Exception e) {
            log.error("Failed to save scan", e);
            return ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to save scan: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<List<SavedScanSummary>> listScans() {
        try {
            String userEmail = getAuthenticatedEmail();
            List<SavedScanSummary> scans = savedScanService.getUserScans(userEmail);
            return ApiResponse.success(scans);
        } catch (Exception e) {
            log.error("Failed to list scans", e);
            return ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to list scans: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<SavedScanDetail> getScanDetail(@PathVariable String id) {
        try {
            String userEmail = getAuthenticatedEmail();
            SavedScanDetail detail = savedScanService.getScanDetail(id, userEmail);
            return ApiResponse.success(detail);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get scan detail", e);
            return ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to get scan: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Map<String, String>> deleteScan(@PathVariable String id) {
        try {
            String userEmail = getAuthenticatedEmail();
            savedScanService.deleteScan(id, userEmail);
            return ApiResponse.success(Map.of("deleted", id));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("NOT_FOUND", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete scan", e);
            return ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to delete scan: " + e.getMessage());
        }
    }

    private String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
