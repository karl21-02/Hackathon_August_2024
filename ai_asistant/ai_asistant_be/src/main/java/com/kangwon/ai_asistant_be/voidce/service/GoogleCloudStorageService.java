package com.kangwon.ai_asistant_be.voidce.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.FileInputStream;
import java.io.IOException;

public class GoogleCloudStorageService {

    public Storage getStorage() throws IOException {
        // 서비스 계정 키 파일을 로드하여 인증 정보 설정
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("classpath:aiassistant-432310-0327ad60b595.json"));
        return StorageOptions.newBuilder().setCredentials(credentials).build().getService();
    }
}
