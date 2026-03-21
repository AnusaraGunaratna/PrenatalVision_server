package com.project.prenatalVision.application.scan;

import com.project.prenatalVision.api.v1.scan.SaveScanRequest;
import com.project.prenatalVision.api.v1.scan.SavedScanDetail;
import com.project.prenatalVision.api.v1.scan.SavedScanSummary;
import com.project.prenatalVision.domain.scan.*;
import com.project.prenatalVision.infrastructure.azure.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedScanService {

        private final ScanRecordRepository scanRecordRepository;
        private final StorageService storageService;

        public ScanRecord saveScan(String userEmail, SaveScanRequest request) {
                log.info("Saving scan for user: {}, type: {}", userEmail, request.getScanType());

                String originalUrl = storageService.uploadBase64Image(
                                request.getOriginalImageBase64(), "original");
                String enhancedUrl = storageService.uploadBase64Image(
                                request.getEnhancedImageBase64(), "enhanced");
                String annotatedUrl = storageService.uploadBase64Image(
                                request.getAnnotatedImageBase64(), "annotated");

                List<DetectionResult> detections = request.getDetections().stream()
                                .map(d -> DetectionResult.builder()
                                                .className(d.getClassName())
                                                .confidence(d.getConfidence())
                                                .bbox(d.getBbox())
                                                .sourceModel(d.getSourceModel())
                                                .build())
                                .toList();

                List<ModelComparisonEntry> modelsComparison = request.getModelsComparison().stream()
                                .map(m -> {
                                        List<DetectionResult> modelDetections = m.getDetections().stream()
                                                        .map(d -> DetectionResult.builder()
                                                                        .className(d.getClassName())
                                                                        .confidence(d.getConfidence())
                                                                        .bbox(d.getBbox())
                                                                        .sourceModel(d.getSourceModel())
                                                                        .build())
                                                        .toList();

                                        String modelAnnotatedUrl = null;
                                        if (m.getAnnotatedImageBase64() != null && !m.getAnnotatedImageBase64().isEmpty()) {
                                                modelAnnotatedUrl = storageService.uploadBase64Image(
                                                                m.getAnnotatedImageBase64(),
                                                                "annotated_" + m.getModelName().toLowerCase().replace(" ", "_"));
                                        }

                                        return ModelComparisonEntry.builder()
                                                        .modelName(m.getModelName())
                                                        .detectionCount(m.getDetections().size())
                                                        .measurements(m.getMeasurements())
                                                        .detections(modelDetections)
                                                        .annotatedImageUrl(modelAnnotatedUrl)
                                                        .build();
                                })
                                .toList();

                ScanRecord record = ScanRecord.builder()
                                .userEmail(userEmail)
                                .scanType(request.getScanType())
                                .originalImageUrl(originalUrl)
                                .enhancedImageUrl(enhancedUrl)
                                .annotatedImageUrl(annotatedUrl)
                                .measurements(request.getMeasurements())
                                .detections(detections)
                                .modelsComparison(modelsComparison)
                                .calibrationRatio(request.getCalibrationRatio())
                                .build();

                ScanRecord saved = scanRecordRepository.save(record);
                log.info("Saved scan record with id: {}", saved.getId());
                return saved;
        }

        public List<SavedScanSummary> getUserScans(String userEmail) {
                return scanRecordRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                                .map(record -> SavedScanSummary.builder()
                                                .id(record.getId())
                                                .scanType(record.getScanType())
                                                .thumbnailUrl(storageService
                                                                .generateSasUrl(record.getAnnotatedImageUrl()))
                                                .detectionCount(record.getDetections() != null
                                                                ? record.getDetections().size()
                                                                : 0)
                                                .createdAt(record.getCreatedAt())
                                                .build())
                                .toList();
        }

        public SavedScanDetail getScanDetail(String id, String userEmail) {
                ScanRecord record = scanRecordRepository.findByIdAndUserEmail(id, userEmail)
                                .orElseThrow(() -> new IllegalArgumentException("Scan not found or access denied"));

                List<ModelComparisonEntry> detailedComparison = record.getModelsComparison().stream()
                                .map(m -> ModelComparisonEntry.builder()
                                                .modelName(m.getModelName())
                                                .detectionCount(m.getDetectionCount())
                                                .measurements(m.getMeasurements())
                                                .detections(m.getDetections())
                                                .annotatedImageUrl(storageService.generateSasUrl(m.getAnnotatedImageUrl()))
                                                .build())
                                .toList();

                return SavedScanDetail.builder()
                                .id(record.getId())
                                .scanType(record.getScanType())
                                .originalImageUrl(storageService.generateSasUrl(record.getOriginalImageUrl()))
                                .enhancedImageUrl(storageService.generateSasUrl(record.getEnhancedImageUrl()))
                                .annotatedImageUrl(storageService.generateSasUrl(record.getAnnotatedImageUrl()))
                                .measurements(record.getMeasurements())
                                .detections(record.getDetections())
                                .modelsComparison(detailedComparison)
                                .calibrationRatio(record.getCalibrationRatio())
                                .createdAt(record.getCreatedAt())
                                .build();
        }

        public void deleteScan(String id, String userEmail) {
                ScanRecord record = scanRecordRepository.findByIdAndUserEmail(id, userEmail)
                                .orElseThrow(() -> new IllegalArgumentException("Scan not found or access denied"));

                storageService.deleteBlob(record.getOriginalImageUrl());
                storageService.deleteBlob(record.getEnhancedImageUrl());
                storageService.deleteBlob(record.getAnnotatedImageUrl());

                if (record.getModelsComparison() != null) {
                        record.getModelsComparison().forEach(m -> {
                                if (m.getAnnotatedImageUrl() != null) {
                                        storageService.deleteBlob(m.getAnnotatedImageUrl());
                                }
                        });
                }

                scanRecordRepository.delete(record);
                log.info("Deleted scan record: {}", id);
        }
}
