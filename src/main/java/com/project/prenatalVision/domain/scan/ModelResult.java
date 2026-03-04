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
public class ModelResult {
    @JsonProperty("model_name")
    private String modelName;

    private List<DetectionResult> detections;
    private Map<String, BiometricMeasurement> measurements;

    @JsonProperty("annotated_image_base64")
    private String annotatedImageBase64;
}
