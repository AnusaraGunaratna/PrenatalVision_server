package com.project.prenatalVision.domain.scan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scan_records")
public class ScanRecord {

    @Id
    private String id;

    @Indexed
    private String userEmail;

    private String scanType;

    private String originalImageUrl;

    private String enhancedImageUrl;

    private String annotatedImageUrl;

    private String bestModelName;

    private Map<String, Object> measurements;

    private List<DetectionResult> detections;

    private List<ModelComparisonEntry> modelsComparison;

    private Double calibrationRatio;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
