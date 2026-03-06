package com.project.prenatalVision.api.v1.scan;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.prenatalVision.domain.scan.DetectionResult;
import com.project.prenatalVision.domain.scan.ModelComparisonEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedScanDetail {

    private String id;

    @JsonProperty("scan_type")
    private String scanType;

    @JsonProperty("original_image_url")
    private String originalImageUrl;

    @JsonProperty("enhanced_image_url")
    private String enhancedImageUrl;

    @JsonProperty("annotated_image_url")
    private String annotatedImageUrl;

    @JsonProperty("best_model_name")
    private String bestModelName;

    private Map<String, Object> measurements;

    private List<DetectionResult> detections;

    @JsonProperty("models_comparison")
    private List<ModelComparisonEntry> modelsComparison;

    @JsonProperty("calibration_ratio")
    private Double calibrationRatio;

    @JsonProperty("created_at")
    private Instant createdAt;
}
