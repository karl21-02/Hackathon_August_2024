package com.kangwon.ai_asistant_be.voidce.controller;

import com.kangwon.ai_asistant_be.voidce.service.SpeechToTextService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class VoiceAssistantController {

    private final String openaiApiKey = "sk-svcacct-yba5oF7yonJlaSuoavhXT3BlbkFJRj6f81YAyep52G3uVhW1";

    // SpeechToTextService를 @Autowired로 주입
    @Autowired
    private SpeechToTextService speechToTextService;

    @PostMapping("/recognize")
    public ResponseEntity<String> recognize(@RequestParam("audio") MultipartFile audio) {
        try {
            // 음성 인식 처리
            String transcription = speechToTextService.transcribe(audio);

            // OpenAI API 요청
            String responseText = callOpenAiApiForAudio(transcription);

            return ResponseEntity.ok(responseText);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/bluetooth")
    public ResponseEntity<String> handleBluetoothData(@RequestBody Map<String, String> requestData) {
        try {
            String data = requestData.get("data");
            if (data == null || data.isEmpty()) {
                return ResponseEntity.badRequest().body("No data received");
            }

            // OpenAI API 요청
            String responseText = callOpenAiApi(data);

            return ResponseEntity.ok(responseText);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private String callOpenAiApiForAudio(String transcription) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com/v1/chat/completions";

        // JSON 객체 생성
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", "You work as a chatbot doctor at a general hospital, treating patients. "
                + "Based on the patient's symptoms and the following data received from the patient's monitoring device:  "
                + "Provide clear and positive responses, and respond concisely to ensure the conversation is easy to understand. "
                + "When asking the patient questions, ask only one question at a time. "
                + "Do not inform the patient of any suspected diseases or diagnoses from the first question; "
                + "instead, provide this information after obtaining sufficient answers about the patient's symptoms "
                + "during the consultation. Help decide and implement the best treatment methods based on the diagnosis results. "
                + "Respond with one sentence whenever possible. If the patient asks about a specific medication, provide information about that medication."
                + "and answer with korean language always!");
        // 사용자 메시지 설정
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", "Patient said: " + transcription);

        // 메시지 배열에 메시지 추가
        JSONArray messagesArray = new JSONArray();
        messagesArray.put(message);  // 시스템 메시지를 먼저 추가 (옵션)
        messagesArray.put(userMessage);    // 사용자 메시지 추가

        // 최종 JSON 객체 생성
        JSONObject json = new JSONObject();
        json.put("model", "gpt-4-turbo");
        json.put("messages", messagesArray);
        json.put("max_tokens", 400);
        json.put("temperature", 0.3);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openaiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(json.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                // 전체 응답을 출력하여 확인
                System.out.println("API 응답: " + response.getBody());

                JSONObject responseJson = new JSONObject(response.getBody());

                // 응답에 "choices" 배열이 있는지 확인
                if (responseJson.has("choices")) {
                    JSONArray choicesArray = responseJson.getJSONArray("choices");

                    // "choices" 배열이 비어있는지 확인
                    if (choicesArray.length() > 0) {
                        // 첫 번째 choice에서 "message" 객체를 가져옴
                        JSONObject choice = choicesArray.getJSONObject(0);
                        JSONObject messageObject = choice.getJSONObject("message");

                        // "message" 객체에서 "content" 키를 가져옴
                        if (messageObject.has("content")) {
                            return messageObject.getString("content");
                        } else {
                            return "API 응답의 'message' 객체에 'content' 키가 없습니다.";
                        }
                    } else {
                        return "API 응답의 'choices' 배열이 비어 있습니다.";
                    }
                } else {
                    return "API 응답에 'choices' 배열이 없습니다.";
                }
            } else {
                throw new RuntimeException("API 요청 실패: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("HTTP 요청 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new RuntimeException("RestTemplate 에러: " + e.getMessage(), e);
        }
    }

    private String callOpenAiApi(String transcription) {
        RestTemplate restTemplate = new RestTemplate();
        String url = "https://api.openai.com/v1/chat/completions";

        // JSON 객체 생성
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", "I will provide my current body temperature, heart rate, and other physical information. " + "this is what i give to you : " + transcription +
                ". this is about your patient's healthy info." + "with this information, you have to analyse your patient's healthy informations" +
                "Afterward, I will describe my symptoms, and based on this information, please assess the following: " +
                "1. The severity of the symptoms, 2. Immediate actions that can be taken, and 3. Whether I need to visit a hospital. " +
                "If the provided symptom information is insufficient, ask me for more specific details. Once all conditions are met, respond by saying, " +
                "'Please describe your symptoms!'" + "and respond with korean language always!");

        JSONArray messagesArray = new JSONArray();
        messagesArray.put(message);

        JSONObject json = new JSONObject();
        json.put("model", "gpt-4-turbo");
        json.put("messages", messagesArray);
        json.put("max_tokens", 400);
        json.put("temperature", 0.3);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + openaiApiKey);

        HttpEntity<String> entity = new HttpEntity<>(json.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                // 전체 응답을 출력하여 확인
                System.out.println("API 응답: " + response.getBody());

                JSONObject responseJson = new JSONObject(response.getBody());

                // 응답에 "choices" 배열이 있는지 확인
                if (responseJson.has("choices")) {
                    JSONArray choicesArray = responseJson.getJSONArray("choices");

                    // "choices" 배열이 비어있는지 확인
                    if (choicesArray.length() > 0) {
                        // 첫 번째 choice에서 "message" 객체를 가져옴
                        JSONObject choice = choicesArray.getJSONObject(0);
                        JSONObject messageObject = choice.getJSONObject("message");

                        // "message" 객체에서 "content" 키를 가져옴
                        if (messageObject.has("content")) {
                            return messageObject.getString("content");
                        } else {
                            return "API 응답의 'message' 객체에 'content' 키가 없습니다.";
                        }
                    } else {
                        return "API 응답의 'choices' 배열이 비어 있습니다.";
                    }
                } else {
                    return "API 응답에 'choices' 배열이 없습니다.";
                }
            } else {
                throw new RuntimeException("API 요청 실패: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("HTTP 요청 실패: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new RuntimeException("RestTemplate 에러: " + e.getMessage(), e);
        }
    }
}