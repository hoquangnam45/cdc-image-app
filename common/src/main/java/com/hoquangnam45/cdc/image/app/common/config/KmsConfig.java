package com.hoquangnam45.cdc.image.app.common.config;

import com.google.cloud.kms.v1.KeyManagementServiceClient;
import com.google.cloud.kms.v1.KeyManagementServiceSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KmsConfig {
    @Bean
    public KeyManagementServiceClient keyManagementServiceClient() throws Exception {
        KeyManagementServiceSettings settings = KeyManagementServiceSettings.newBuilder().build();
        return KeyManagementServiceClient.create(settings);
    }
}
