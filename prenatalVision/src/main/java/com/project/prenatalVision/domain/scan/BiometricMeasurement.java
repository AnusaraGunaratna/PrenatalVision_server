package com.project.prenatalVision.domain.scan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BiometricMeasurement {
    @JsonProperty("dimension_mm")
    private String dimensionMm;

    @JsonProperty("thickness_mm")
    private Double thicknessMm;

    @JsonProperty("length_cm")
    private Double lengthCm;

    @JsonProperty("length_mm")
    private Double lengthMm;

    @JsonProperty("distance_px")
    private Double distancePx;

    @JsonProperty("BPD_mm")
    private Double bpdMm;

    @JsonProperty("HC_mm")
    private Double hcMm;

    @JsonProperty("circumference_mm")
    private Double circumferenceMm;

    @JsonProperty("height_mm")
    private Double heightMm;

    @JsonProperty("width_mm")
    private Double widthMm;

    private Double confidence;
}
