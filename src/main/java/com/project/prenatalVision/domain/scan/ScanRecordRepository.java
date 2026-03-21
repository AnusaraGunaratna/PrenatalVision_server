package com.project.prenatalVision.domain.scan;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ScanRecordRepository extends MongoRepository<ScanRecord, String> {

    List<ScanRecord> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<ScanRecord> findByIdAndUserEmail(String id, String userEmail);
}
