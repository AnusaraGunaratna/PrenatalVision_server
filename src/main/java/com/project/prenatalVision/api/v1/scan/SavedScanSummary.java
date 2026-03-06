package com.project.prenatalVision.api.v1.scan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedScanSummary {

    private String id;

    @JsonProperty("scan_type")
    private String scanType;

    @JsonProperty("thumbnail_url")
    private String thumbnailUrl;

    @JsonProperty("best_model_name")
    private String bestModelName;

    @JsonProperty("detection_count")
    private int detectionCount;

    @JsonProperty("created_at")
    private Instant createdAt;
}
