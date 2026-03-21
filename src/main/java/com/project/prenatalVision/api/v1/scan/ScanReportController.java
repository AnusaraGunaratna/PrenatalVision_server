package com.project.prenatalVision.api.v1.scan;

import com.project.prenatalVision.application.report.ReportService;
import com.project.prenatalVision.application.scan.SavedScanService;
import com.project.prenatalVision.domain.scan.ScanRecord;
import com.project.prenatalVision.domain.scan.ScanRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/saved-scans")
@RequiredArgsConstructor
public class ScanReportController {

    private final ReportService reportService;
    private final ScanRecordRepository scanRecordRepository;

    @GetMapping("/{id}/report")
    public ResponseEntity<byte[]> getScanReport(@PathVariable String id) {
        log.info("Requesting PDF report for scan: {}", id);
        
        String userEmail = getAuthenticatedEmail();
        ScanRecord scan = scanRecordRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Scan not found or access denied"));

        byte[] pdfBytes = reportService.generateScanReport(scan);

        String filename = "PrenatalVision_Report_" + scan.getId() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    private String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
