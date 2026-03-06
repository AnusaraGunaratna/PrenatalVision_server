package com.project.prenatalVision.infrastructure.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final BlobContainerClient containerClient;
    private static final long SAS_EXPIRY_HOURS = 1;

    public String uploadBase64Image(String base64Data, String prefix) {
        String cleanBase64 = stripDataUriPrefix(base64Data);
        byte[] imageBytes = Base64.getDecoder().decode(cleanBase64);

        String blobName = prefix + "/" + UUID.randomUUID() + ".jpg";
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        blobClient.upload(new ByteArrayInputStream(imageBytes), imageBytes.length, true);
        log.info("Uploaded blob: {}", blobName);

        return blobClient.getBlobUrl();
    }

    public String generateSasUrl(String blobUrl) {
        String blobName = extractBlobName(blobUrl);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
        BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusHours(SAS_EXPIRY_HOURS), permission);

        String sasToken = blobClient.generateSas(sasValues);
        return blobUrl + "?" + sasToken;
    }

    public void deleteBlob(String blobUrl) {
        String blobName = extractBlobName(blobUrl);
        BlobClient blobClient = containerClient.getBlobClient(blobName);

        if (blobClient.exists()) {
            blobClient.delete();
            log.info("Deleted blob: {}", blobName);
        } else {
            log.warn("Blob not found for deletion: {}", blobName);
        }
    }

    private String stripDataUriPrefix(String base64Data) {
        if (base64Data.contains(",")) {
            return base64Data.substring(base64Data.indexOf(",") + 1);
        }
        return base64Data;
    }

    private String extractBlobName(String blobUrl) {
        String containerName = containerClient.getBlobContainerName();
        int containerIndex = blobUrl.indexOf("/" + containerName + "/");
        if (containerIndex == -1) {
            throw new IllegalArgumentException("Invalid blob URL: " + blobUrl);
        }
        return blobUrl.substring(containerIndex + containerName.length() + 2);
    }
}
