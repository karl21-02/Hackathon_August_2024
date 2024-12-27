package com.kangwon.ai_asistant_be.voidce.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.SpeechSettings;
import com.google.cloud.speech.v1.stub.SpeechStubSettings;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class GoogleCloudConfig {

    @Value("classpath:aiassistant-432310-0327ad60b595.json")
    Resource gcsCredentials;

    @Bean
    public SpeechSettings speechSettings() {
        try {
            return SpeechSettings.newBuilder()
                    .setCredentialsProvider(() -> GoogleCredentials.fromStream(gcsCredentials.getInputStream()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public Storage storage() {
        try {
            return StorageOptions.newBuilder()
                    .setCredentials(GoogleCredentials.fromStream(gcsCredentials.getInputStream()))
                    .build()
                    .getService();
        } catch (IOException e) {
            throw new RuntimeException("GCS 인증 정보를 로드하는 동안 오류가 발생했습니다.", e);
        }
    }

    @Bean
    public SpeechStubSettings speechStubSettings() {
        try {
            return SpeechStubSettings.newBuilder()
                    .setCredentialsProvider(() -> GoogleCredentials.fromStream(gcsCredentials.getInputStream()))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}