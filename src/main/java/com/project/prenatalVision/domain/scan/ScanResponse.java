package com.project.prenatalVision.domain.scan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanResponse {
    @JsonProperty("scan_id")
    private String scanId;

    @JsonProperty("scan_type")
    private String scanType;

    @JsonProperty("original_image_base64")
    private String originalImageBase64;

    @JsonProperty("enhanced_image_base64")
    private String enhancedImageBase64;

    private List<DetectionResult> detections;

    private Map<String, BiometricMeasurement> measurements;

    @JsonProperty("annotated_image_base64")
    private String annotatedImageBase64;

    @JsonProperty("models_comparison")
    private List<ModelResult> modelsComparison;

    @JsonProperty("calibration_ratio")
    private Double calibrationRatio;

    @JsonProperty("processed_at")
    private String processedAt;
}
