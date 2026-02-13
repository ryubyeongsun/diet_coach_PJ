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

  @Value("${gms.openai.api-key:}")
  private String apiKey;

  @Value("${gms.openai.model:gpt-5-mini}")
  private String model;

  public AiMonthlySkeletonResponse generateMonthlySkeleton(Map<String, Object> payload) {
    boolean keyPresent = apiKey != null && !apiKey.isBlank();
    int keyLen = (apiKey == null ? 0 : apiKey.length());
    log.info("[AI] generateMonthlySkeleton start model={}, apiKeyPresent={}, keyLen={}, keys={}",
        model, keyPresent, keyLen, payload.keySet());
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
        Role: Professional Clinical Dietitian & Budget Analyst.
        Constraint: You must think step-by-step to calculate nutrition and cost before outputting JSON.

        Rules:
        1. VARIETY: NEVER repeat the same 'menuName' or same meal combination across the plan. Do not use the same ingredient twice in one meal (e.g. no Chicken Breast twice in Lunch).
        2. CALORIE PRECISION: 'actualTotalKcal' MUST be the exact sum of all ingredients' 'kcal' in that day.
        3. STANDARD DATA: Use standard nutritional data for COOKED food (e.g., Cooked Rice 100g = ~130kcal, Cooked Chicken Breast 100g = ~110kcal, Veggies 100g = ~30kcal).
        4. ALLERGY SAFETY: Strictly exclude ingredients based on the provided allergy list. If Milk is listed, exclude: milk, cheese, yogurt, butter, cream, and any dairy.
        5. PORTION REALISM: For high-calorie goals (e.g., 2800kcal), increase portion sizes (e.g., 200-300g of rice, 150-200g of protein) instead of adding unrealistic side dishes.
        6. JSON ONLY: No markdown, no conversational text. Return only the raw JSON.
        7. DATA TYPE: Ensure 'grams', 'kcal', 'targetKcal', 'actualTotalKcal', and 'estimatedCostKrw' are NUMBERS, not strings.
        8. LANGUAGE: All names and reasoning must be in Korean (Hangul).
        """;

    String userPrompt = toUserPrompt(payload);
    String combinedPrompt = system + "\n\n" + userPrompt;

    // Gemini Native Payload Structure
    Map<String, Object> body = Map.of(
        "contents", List.of(
            Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", combinedPrompt)))),
        "generationConfig", Map.of(
            "response_mime_type", "application/json"));

    String reqBody = objectMapper.writeValueAsString(body);
    log.info("[AI] request body size={}", reqBody.length());

    // Call Gemini API (Fixed to match GMS Guideline)
    String respBody = gmsOpenAiRestClient.post()
        .uri(uriBuilder -> uriBuilder
            .path("/generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
            .queryParam("key", apiKey)
            .build())
        .contentType(MediaType.APPLICATION_JSON)
        .body(reqBody)
        .retrieve()
        .body(String.class);

    if (respBody == null || respBody.isBlank()) {
      throw new RuntimeException("AI response empty");
    }

    // Parse Gemini Response
    JsonNode root = objectMapper.readTree(respBody);

    // Structure: candidates[0].content.parts[0].text
    JsonNode candidates = root.path("candidates");
    if (candidates.isEmpty()) {
      throw new RuntimeException("AI candidates empty");
    }

    String content = candidates.get(0).path("content").path("parts").get(0).path("text").asText();

    if (content == null || content.isBlank()) {
      throw new RuntimeException("AI content empty");
    }

    String json = extractJsonOnly(content);

    log.info("[AI] json preview={}", json.length() > 200 ? json.substring(0, 200) : json);

    return objectMapper.readValue(json, AiMonthlySkeletonResponse.class);
  }

  private String toUserPrompt(Map<String, Object> payload) {
    int targetKcal = (int) payload.getOrDefault("targetCaloriesPerDay", 2000);
    int lowerBound = (int) (targetKcal * 0.97);
    int upperBound = (int) (targetKcal * 1.03);
    String goalType = (String) payload.getOrDefault("goalType", "MAINTAIN");

    // 테마 및 세부 지침 정의
    String themeInfo = switch (goalType) {
      case "LOSE_WEIGHT" -> "다이어트 (체지방 감량, 고단백, 저탄수화물, 풍부한 식이섬유)";
      case "GAIN_WEIGHT" -> "벌크업 (근육 및 체중 증량, 고단백, 고복합탄수화물, 높은 칼로리 밀도)";
      default -> "유지 (건강한 균형식, 탄단지 5:3:2 비율 준수)";
    };

    return """
        Create a %d-day meal skeleton from startDate=%s.

        [Target User Data]
        - Daily Goal: %d kcal (Target Tolerance: ±3%%)
        - Goal Type: %s
        - Diet Theme: %s
        - Meals Per Day: %s (Ratio: Breakfast 30%% / Lunch 40%% / Dinner 30%%)
        - Preferences: %s
        - Allergies: %s

        [High Calorie Rule]
        IF targetKcal >= 2500:
          - YOU MUST INCLUDE: at least 200g of Rice/Carb AND 150g of Protein per meal.
          - Do not output small portions like 100g for main dishes.

        [Execution Step-by-Step]
        1. Calculate: First, determine the target kcal for each meal.
        2. Selection: Choose diverse Korean ingredients based on the Diet Theme (%s).
        3. Quantify: If targetKcal >= 2500, set Rice to 200g+ and Meat to 150g+. Adjust 'grams' to ensure total kcal reaches %d.
        4. Verify: Sum all ingredient kcal for each day. If it is not within %d-%d kcal, re-adjust the portions before generating.
        5. Diversity Check: Scan the plan to ensure no menu name is repeated.

        [Schema]
        {
          "days": [
            {
              "dayIndex": 1,
              "planDate": "yyyy-MM-dd",
              "validation": {
                "targetKcal": %d,
                "actualTotalKcal": 0,
                "estimatedCostKrw": 0,
                "isBudgetSafe": true
              },
              "meals": [
                {
                  "mealTime": "BREAKFAST/LUNCH/DINNER/SNACK",
                  "menuName": "Korean Menu Name",
                  "reasoning": "Explain why this fits the theme and budget",
                  "ingredients": [
                    {"name": "Korean Name", "grams": 0, "kcal": 0}
                  ]
                }
              ]
            }
          ]
        }

        Return JSON only.
        """
        .formatted(
            payload.get("totalDays"),
            payload.get("startDate"),
            targetKcal,
            goalType,
            themeInfo,
            payload.get("mealsPerDay"),
            payload.get("preferences"),
            payload.get("allergies"),
            themeInfo,
            targetKcal,
            lowerBound,
            upperBound,
            targetKcal);
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
      if (t.endsWith("```"))
        t = t.substring(0, t.length() - 3).trim();
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
