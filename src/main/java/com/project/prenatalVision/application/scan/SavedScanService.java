package com.project.prenatalVision.application.scan;

import com.project.prenatalVision.api.v1.scan.SaveScanRequest;
import com.project.prenatalVision.api.v1.scan.SavedScanDetail;
import com.project.prenatalVision.api.v1.scan.SavedScanSummary;
import com.project.prenatalVision.domain.scan.*;
import com.project.prenatalVision.infrastructure.azure.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Collections;
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

                // Upload additional annotated image if present
                String additionalAnnotatedUrl = null;
                if (request.getAdditionalAnnotatedImageBase64() != null
                                && !request.getAdditionalAnnotatedImageBase64().isBlank()) {
                        additionalAnnotatedUrl = storageService.uploadBase64Image(
                                        request.getAdditionalAnnotatedImageBase64(), "additional-annotated");
                }

                List<DetectionResult> detections = request.getDetections().stream()
                                .map(d -> DetectionResult.builder()
                                                .className(d.getClassName())
                                                .confidence(d.getConfidence())
                                                .bbox(d.getBbox())
                                                .build())
                                .toList();

                List<ModelComparisonEntry> modelsComparison = request.getModelsComparison().stream()
                                .map(m -> {
                                        List<DetectionResult> modelDetections = m.getDetections().stream()
                                                        .map(d -> DetectionResult.builder()
                                                                        .className(d.getClassName())
                                                                        .confidence(d.getConfidence())
                                                                        .bbox(d.getBbox())
                                                                        .build())
                                                        .toList();

                                        return ModelComparisonEntry.builder()
                                                        .modelName(m.getModelName())
                                                        .detectionCount(m.getDetections().size())
                                                        .measurements(m.getMeasurements())
                                                        .detections(modelDetections)
                                                        .build();
                                })
                                .toList();

                // Map additional detections from supporting models
                List<AdditionalDetection> additionalDetections = request.getAdditionalDetections() != null
                                ? request.getAdditionalDetections().stream()
                                                .map(d -> AdditionalDetection.builder()
                                                                .className(d.getClassName())
                                                                .confidence(d.getConfidence())
                                                                .bbox(d.getBbox())
                                                                .sourceModel(d.getSourceModel())
                                                                .build())
                                                .toList()
                                : Collections.emptyList();

                ScanRecord record = ScanRecord.builder()
                                .userEmail(userEmail)
                                .scanType(request.getScanType())
                                .originalImageUrl(originalUrl)
                                .enhancedImageUrl(enhancedUrl)
                                .annotatedImageUrl(annotatedUrl)
                                .bestModelName(request.getBestModelName())
                                .measurements(request.getBestModelMeasurements())
                                .detections(detections)
                                .modelsComparison(modelsComparison)
                                .additionalDetections(additionalDetections)
                                .additionalMeasurements(request.getAdditionalMeasurements())
                                .additionalAnnotatedImageUrl(additionalAnnotatedUrl)
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
                                                .bestModelName(record.getBestModelName())
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

                List<SavedScanDetail.AdditionalDetectionDetail> additionalDetails = record
                                .getAdditionalDetections() != null
                                                ? record.getAdditionalDetections().stream()
                                                                .map(d -> SavedScanDetail.AdditionalDetectionDetail
                                                                                .builder()
                                                                                .className(d.getClassName())
                                                                                .confidence(d.getConfidence())
                                                                                .bbox(d.getBbox())
                                                                                .sourceModel(d.getSourceModel())
                                                                                .build())
                                                                .toList()
                                                : Collections.emptyList();

                return SavedScanDetail.builder()
                                .id(record.getId())
                                .scanType(record.getScanType())
                                .originalImageUrl(storageService.generateSasUrl(record.getOriginalImageUrl()))
                                .enhancedImageUrl(storageService.generateSasUrl(record.getEnhancedImageUrl()))
                                .annotatedImageUrl(storageService.generateSasUrl(record.getAnnotatedImageUrl()))
                                .bestModelName(record.getBestModelName())
                                .measurements(record.getMeasurements())
                                .detections(record.getDetections())
                                .modelsComparison(record.getModelsComparison())
                                .additionalDetections(additionalDetails)
                                .additionalMeasurements(record.getAdditionalMeasurements())
                                .additionalAnnotatedImageUrl(
                                                record.getAdditionalAnnotatedImageUrl() != null
                                                                ? storageService.generateSasUrl(
                                                                                record.getAdditionalAnnotatedImageUrl())
                                                                : null)
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

                if (record.getAdditionalAnnotatedImageUrl() != null) {
                        storageService.deleteBlob(record.getAdditionalAnnotatedImageUrl());
                }

                scanRecordRepository.delete(record);
                log.info("Deleted scan record: {}", id);
        }
}
