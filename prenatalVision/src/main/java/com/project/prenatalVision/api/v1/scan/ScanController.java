package com.project.prenatalVision.api.v1.scan;

import com.project.prenatalVision.api.configuration.ApiResponse;
import com.project.prenatalVision.application.scan.ScanService;
import com.project.prenatalVision.domain.scan.ScanResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/scans")
@RequiredArgsConstructor

public class ScanController {

    private final ScanService scanService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)

    public ApiResponse<ScanResponse> analyzeScan(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "scan_type", defaultValue = "crl") String scanType,
            @RequestParam(value = "ga_weeks", required = false) Integer gaWeeks) {
            
        try {
            ScanResponse result = scanService.processScanAnalysis(image, scanType, gaWeeks);
            return ApiResponse.success(result);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("BAD_REQUEST", e.getMessage());
        } catch (Exception e) {
            log.error("Analysis failed", e);
            return ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to process scan: " + e.getMessage());
        }
    }
}
