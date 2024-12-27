//package com.kangwon.ai_asistant_be.voidce.service;
//
//import java.io.IOException;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.google.cloud.speech.v1.RecognitionAudio;
//import com.google.cloud.speech.v1.RecognitionConfig;
//import com.google.cloud.speech.v1.RecognizeResponse;
//import com.google.cloud.speech.v1.SpeechClient;
//import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
//import com.google.cloud.speech.v1.SpeechRecognitionResult;
//import com.google.cloud.speech.v1.SpeechSettings;
//import com.google.protobuf.ByteString;
//@Service
//public class SpeechToTextService {
//
//    @Autowired
//    private SpeechSettings speechSettings;
//
//    private final Logger logger = LoggerFactory.getLogger(SpeechToTextService.class);
//
//    public String transcribe(MultipartFile audioFile) throws IOException {
//        try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
//            ByteString audioBytes = ByteString.copyFrom(audioFile.getBytes());
//
//            RecognitionConfig config = RecognitionConfig.newBuilder()
//                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)  // 다른 오디오 인코딩 형식 시도                    .setSampleRateHertz(48000)  // 일반적인 음성 샘플링 속도
//                    .setLanguageCode("ko-KR")  // 한국어 설정
//                    .build();
//
//            RecognitionAudio audio = RecognitionAudio.newBuilder()
//                    .setContent(audioBytes)
//                    .build();
//
//            RecognizeResponse response = speechClient.recognize(config, audio);
//            StringBuilder transcription = new StringBuilder();
//            for (SpeechRecognitionResult result : response.getResultsList()) {
//                transcription.append(result.getAlternativesList().get(0).getTranscript());
//            }
//
//            return transcription.toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            throw new RuntimeException("음성 인식 중 오류 발생: " + e.getMessage(), e);
//        }
//    }
//}
//
package com.kangwon.ai_asistant_be.voidce.service;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.cloud.storage.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
public class SpeechToTextService {

    private final Logger logger = LoggerFactory.getLogger(SpeechToTextService.class);
    private static final String BUCKET_NAME = "ai_assitant_bucket"; // GCS 버킷 이름을 여기에 입력

    public String transcribe(MultipartFile audioFile) throws IOException, InterruptedException, ExecutionException {
        // GCS에 오디오 파일 업로드
        String gcsUri = uploadAudioToGCS(audioFile);

        try (SpeechClient speechClient = SpeechClient.create()) {
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
                    .setSampleRateHertz(48000)
                    .setLanguageCode("ko-KR")
                    .build();

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setUri(gcsUri)  // GCS URI를 설정
                    .build();

            // LongRunningRecognize를 사용하여 비동기 처리
            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> future =
                    speechClient.longRunningRecognizeAsync(config, audio);

            // 작업 완료 대기
            LongRunningRecognizeResponse response = future.get();

            // 결과 처리
            StringBuilder transcription = new StringBuilder();
            for (SpeechRecognitionResult result : response.getResultsList()) {
                transcription.append(result.getAlternativesList().get(0).getTranscript());
            }

            return transcription.toString();
        } catch (Exception e) {
            logger.error("음성 인식 중 오류 발생: ", e);
            throw new RuntimeException("음성 인식 중 오류 발생: " + e.getMessage(), e);
        }
    }

    private String uploadAudioToGCS(MultipartFile audioFile) throws IOException {
        String fileName = UUID.randomUUID().toString() + ".wav";
        Path tempFile = Files.createTempFile(fileName, ""); // 임시 파일 생성
        Files.write(tempFile, audioFile.getBytes());

        // GoogleCredentials를 사용하여 인증 정보 설정
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream("C:\\workspace\\bootcamp\\ai_asistant\\ai_asistant_be\\src\\main\\resources\\aiassistant-432310-0327ad60b595.json"));
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("audio/wav").build();
        storage.create(blobInfo, Files.readAllBytes(tempFile));

        return String.format("gs://%s/%s", BUCKET_NAME, fileName); // GCS URI 반환
    }
}

//package com.kangwon.ai_asistant_be.voidce.service;
//
//import com.google.api.gax.longrunning.OperationFuture;
//import com.google.cloud.speech.v1.*;
//import com.google.cloud.storage.BlobId;
//import com.google.cloud.storage.BlobInfo;
//import com.google.cloud.storage.Storage;
//import com.google.cloud.storage.StorageOptions;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.util.UUID;
//import java.util.concurrent.ExecutionException;
//
//@Service
//public class SpeechToTextService {
//
//    @Autowired
//    private SpeechSettings speechSettings;
//
//    private final Logger logger = LoggerFactory.getLogger(SpeechToTextService.class);
//
//    private static final String BUCKET_NAME = "ai_assitant_bucket"; // GCS 버킷 이름을 여기에 입력
//
//    public String transcribe(MultipartFile audioFile) throws IOException, InterruptedException, ExecutionException {
//        // 1분 이상의 오디오 파일 처리: GCS에 오디오 파일 업로드
//        String gcsUri = uploadAudioToGCS(audioFile);
//
//        try (SpeechClient speechClient = SpeechClient.create(speechSettings)) {
//            RecognitionConfig config = RecognitionConfig.newBuilder()
//                    .setEncoding(RecognitionConfig.AudioEncoding.WEBM_OPUS)
//                    .setSampleRateHertz(48000)
//                    .setLanguageCode("ko-KR")
//                    .build();
//
//            RecognitionAudio audio = RecognitionAudio.newBuilder()
//                    .setUri(gcsUri)  // GCS URI를 설정
//                    .build();
//
//            // LongRunningRecognize를 사용하여 비동기 처리
//            OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> future =
//                    speechClient.longRunningRecognizeAsync(config, audio);
//
//            // 작업 완료 대기
//            LongRunningRecognizeResponse response = future.get();
//
//            // 결과 처리
//            StringBuilder transcription = new StringBuilder();
//            for (SpeechRecognitionResult result : response.getResultsList()) {
//                transcription.append(result.getAlternativesList().get(0).getTranscript());
//            }
//
//            return transcription.toString();
//        } catch (Exception e) {
//            logger.error("음성 인식 중 오류 발생: ", e);
//            throw new RuntimeException("음성 인식 중 오류 발생: " + e.getMessage(), e);
//        }
//    }
//
//    private String uploadAudioToGCS(MultipartFile audioFile) throws IOException {
//        String fileName = UUID.randomUUID().toString() + ".wav";
//        Path tempFile = Files.createTempFile(fileName, ""); // 임시 파일 생성
//        Files.write(tempFile, audioFile.getBytes());
//
//        Storage storage = StorageOptions.getDefaultInstance().getService();
//        BlobId blobId = BlobId.of(BUCKET_NAME, fileName);
//        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("audio/wav").build();
//        storage.create(blobInfo, Files.readAllBytes(tempFile));
//
//        return String.format("gs://%s/%s", BUCKET_NAME, fileName); // GCS URI 반환
//    }
//}
