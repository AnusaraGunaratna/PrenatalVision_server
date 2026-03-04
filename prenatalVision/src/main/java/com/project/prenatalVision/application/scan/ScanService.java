package com.project.prenatalVision.application.scan;

import com.project.prenatalVision.domain.scan.ScanResponse;
import com.project.prenatalVision.infrastructure.python.PythonModelClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanService {

    private final PythonModelClient modelClient;

    public ScanResponse processScanAnalysis(MultipartFile image, String scanType, Integer gaWeeks) {
        log.info("Processing scan analysis. Type: {}, GA: {}", scanType, gaWeeks);

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        ScanResponse response = modelClient.analyzeImage(image, scanType, gaWeeks);
        
        log.info("Successfully processed scan analysis. Extracted {} model variants", 
                response.getModelsComparison() != null ? response.getModelsComparison().size() : 0);
                
        return response;
    }
}
