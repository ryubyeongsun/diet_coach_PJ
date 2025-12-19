package com.dietcoach.project.client.ai;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.dietcoach.project.client.ai.dto.AiMonthlySkeletonResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DietAiClient {

    private final RestClient gmsOpenAiRestClient; // ✅ AiHttpConfig 빈 주입
    private final ObjectMapper objectMapper;

    @Value("${gms.openai.api-key}")
    private String apiKey;

    @Value("${gms.openai.model:gpt-5-mini}")
    private String model;

    public AiMonthlySkeletonResponse generateMonthlySkeleton(Map<String, Object> payload) {
        // 1회 retry 포함 (총 2회 시도)
        try {
            return callOnce(payload);
        } catch (Exception first) {
            log.warn("[AI] first attempt failed: {}", first.getMessage());
            try {
                return callOnce(payload);
            } catch (Exception second) {
                log.warn("[AI] second attempt failed: {}", second.getMessage());
                throw new RuntimeException("AI_CALL_FAILED", second);
            }
        }
    }

    private AiMonthlySkeletonResponse callOnce(Map<String, Object> payload) throws Exception {
        String system = """
            You are a diet planning assistant.
            Output MUST be valid JSON only. No markdown. No extra text.
            Follow this fixed schema exactly:

            {
              "days":[
                {
                  "dayIndex":1,
                  "planDate":"2025-12-01",
                  "meals":[
                    {
                      "mealTime":"BREAKFAST",
                      "menuName":"Oatmeal bowl",
                      "ingredients":["oats","milk","banana"],
                      "calories":450
                    }
                  ]
                }
              ]
            }

            Rules:
            - mealTime must be one of BREAKFAST|LUNCH|DINNER|SNACK
            - planDate format yyyy-MM-dd
            - ingredients must be keyword list (no grams, no prices)
            """;

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "developer", "content", system),
                        Map.of("role", "user", "content", toUserPrompt(payload))
                )
        );

        String respBody = gmsOpenAiRestClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);

        if (respBody == null || respBody.isBlank()) {
            throw new RuntimeException("AI response empty");
        }

        // choices[0].message.content 가 JSON 문자열
        JsonNode root = objectMapper.readTree(respBody);
        String content = root.path("choices").path(0).path("message").path("content").asText();

        if (content == null || content.isBlank()) {
            throw new RuntimeException("AI content empty");
        }

        String json = extractJsonOnly(content);

        log.info("[AI] json preview={}", json.length() > 200 ? json.substring(0, 200) : json);

        return objectMapper.readValue(json, AiMonthlySkeletonResponse.class);
    }

    private String toUserPrompt(Map<String, Object> payload) {
        return """
        Create a %d-day meal skeleton from startDate=%s.
        Constraints:
        - targetCaloriesPerDay=%s
        - mealsPerDay=%s
        - monthlyBudget=%s
        - preferences=%s
        - allergies=%s

        Return JSON only.
        """.formatted(
                payload.get("totalDays"),
                payload.get("startDate"),
                payload.get("targetCaloriesPerDay"),
                payload.get("mealsPerDay"),
                payload.get("monthlyBudget"),
                payload.get("preferences"),
                payload.get("allergies")
        );
    }

    /**
     * 모델이 가끔 ```json ... ``` 같은 펜스를 붙이는 경우 제거
     * + 앞뒤 잡텍스트가 섞여도 JSON object 부분만 최대한 추출
     */
    private String extractJsonOnly(String s) {
        String t = s.trim();

        // ```json ... ``` or ``` ... ```
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```(json)?", "").trim();
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3).trim();
        }

        // JSON object 범위만 잘라내기 (첫 '{' ~ 마지막 '}')
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1).trim();
        }
        return t;
    }
}
