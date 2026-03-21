package com.project.prenatalVision.infrastructure.azure;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobCorsRule;
import com.azure.storage.blob.models.BlobServiceProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AzureBlobConfig {

    @Value("${azure.storage.connection-string}")
    private String connectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public BlobServiceClient blobServiceClient() {
        BlobServiceClient client = new BlobServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

        try {
            BlobServiceProperties properties = client.getProperties();

            boolean corsExists = properties.getCors() != null && properties.getCors().stream()
                    .anyMatch(rule -> rule.getAllowedOrigins().contains("*")
                            || allowedOrigins.contains(rule.getAllowedOrigins()));

            if (!corsExists) {
                BlobCorsRule corsRule = new BlobCorsRule()
                        .setAllowedOrigins(allowedOrigins)
                        .setAllowedMethods("GET, HEAD, OPTIONS, PUT, POST")
                        .setAllowedHeaders("*")
                        .setExposedHeaders("*")
                        .setMaxAgeInSeconds(3600);

                properties.setCors(java.util.Collections.singletonList(corsRule));
                client.setProperties(properties);
                log.info("Azure Blob CORS configured for origins: {}", allowedOrigins);
            }
        } catch (Exception e) {
            log.warn("Could not configure Azure Blob CORS automatically: {}", e.getMessage());
        }

        return client;
    }

    @Bean
    public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }
        return containerClient;
    }
}
