package com.project.prenatalVision.api.v1.scan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SaveScanRequest {

    @NotBlank
    @JsonProperty("scan_type")
    private String scanType;

    @NotBlank
    @JsonProperty("original_image_base64")
    private String originalImageBase64;

    @NotBlank
    @JsonProperty("enhanced_image_base64")
    private String enhancedImageBase64;

    @NotBlank
    @JsonProperty("annotated_image_base64")
    private String annotatedImageBase64;

    @NotBlank
    @JsonProperty("best_model_name")
    private String bestModelName;

    @NotNull
    @JsonProperty("best_model_measurements")
    private Map<String, Object> bestModelMeasurements;

    @NotNull
    private List<DetectionEntry> detections;

    @NotNull
    @JsonProperty("models_comparison")
    private List<ModelComparisonInput> modelsComparison;

    @JsonProperty("calibration_ratio")
    private Double calibrationRatio;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DetectionEntry {
        @JsonProperty("class_name")
        private String className;
        private double confidence;
        private List<Double> bbox;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ModelComparisonInput {
        @JsonProperty("model_name")
        private String modelName;
        private List<DetectionEntry> detections;
        private Map<String, Object> measurements;
    }
}
