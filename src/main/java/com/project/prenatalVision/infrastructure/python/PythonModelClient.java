package com.project.prenatalVision.infrastructure.python;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.prenatalVision.api.configuration.ApiResponse;
import com.project.prenatalVision.domain.scan.ScanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Component
public class PythonModelClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String pythonServerUrl;
    private final String apiKey;

    public PythonModelClient(
            @Value("${python.server.url}") String pythonServerUrl,
            @Value("${python.server.api-key}") String apiKey) {
        this.pythonServerUrl = pythonServerUrl;
        this.apiKey = apiKey;
    }

    public ScanResponse analyzeImage(MultipartFile file, String scanType, Integer gaWeeks) {
        log.info("Sending image to Python server for {} analysis", scanType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-API-Key", apiKey);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            ByteArrayResource fileAsResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("image", fileAsResource);
            body.add("scan_type", scanType);
            if (gaWeeks != null) {
                body.add("ga_weeks", gaWeeks.toString());
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    pythonServerUrl + "/api/v1/scans/analyze", requestEntity, String.class);

            ApiResponse<ScanResponse> apiResponse = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<>() {}
            );

            if (!apiResponse.isSuccess() || apiResponse.getData() == null) {
                log.error("Python server returned error: {}", apiResponse.getError());
                throw new RuntimeException("Model Inference Failed: " + 
                        (apiResponse.getError() != null ? apiResponse.getError().getMessage() : "Unknown"));
            }

            return apiResponse.getData();
            
        } catch (IOException e) {
            log.error("Failed to read multipart file", e);
            throw new RuntimeException("Failed to read image file", e);
        } catch (Exception e) {
            log.error("Communication with Python server failed", e);
            throw new RuntimeException("Communication with Model Server failed", e);
        }
    }
}
