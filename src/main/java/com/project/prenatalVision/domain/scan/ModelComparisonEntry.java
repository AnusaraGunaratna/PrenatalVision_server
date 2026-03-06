package com.project.prenatalVision.domain.scan;

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
public class ModelComparisonEntry {

    private String modelName;

    private int detectionCount;

    private Map<String, Object> measurements;

    private List<DetectionResult> detections;
}
