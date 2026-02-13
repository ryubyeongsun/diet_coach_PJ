package com.dietcoach.project.service.shopping;

import com.dietcoach.project.domain.ShoppingProduct;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiProductReranker {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gms.api.key:}")
    private String gmsKey;

    @Value("${gms.api.url:https://gms.ssafy.io/gmsapi/api.openai.com/v1/chat/completions}")
    private String gmsUrl;

    @Value("${gms.api.model:gpt-4o-mini}")
    private String model;

    public ShoppingProduct rerank(String ingredient, int budget, List<ShoppingProduct> candidates) {
        if (candidates == null || candidates.isEmpty()) return null;
        if (gmsKey == null || gmsKey.isBlank()) {
            log.warn("[AI_RERANKER] GMS Key missing. Skipping AI reranking.");
            return null;
        }

        try {
            // 1. Build Text Payload
            StringBuilder userPrompt = new StringBuilder();
            userPrompt.append("Target Ingredient: ").append(ingredient).append("\n");
            userPrompt.append("Budget: ").append(budget).append(" KRW\n");
            userPrompt.append("Candidates:\n");
            
            for (ShoppingProduct p : candidates) {
                userPrompt.append(String.format("- [ID:%s] %s | %d KRW | %s\n", 
                        p.getExternalId(), p.getTitle(), p.getPrice(), p.getCategoryName()));
            }
            userPrompt.append("\nSelect ONE best product ID for human food. Return ONLY JSON format: {\"selectedId\": \"...\", \"reason\": \"...\"}");

            // Debug Logging
            String keyStatus = (gmsKey == null || gmsKey.isBlank()) ? "MISSING" : "PRESENT(" + gmsKey.length() + ")";
            log.info("[AI_RERANKER] Calling GMS. Model={} Key={} URL={}", model, keyStatus, gmsUrl);

            // 2. Prepare Request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(gmsKey);

            GptRequest requestBody = new GptRequest(model, 0.1);
            // Strong system prompt to force JSON
            requestBody.messages.add(new Message("system", 
                "You are a backend JSON generator. Output ONLY valid JSON. " +
                "Do NOT include any introduction, explanation, or markdown formatting. " +
                "Schema: {\"selectedId\": string, \"reason\": string}"));
            requestBody.messages.add(new Message("user", userPrompt.toString()));

            // Log Request Body
            try {
                String debugBody = objectMapper.writeValueAsString(requestBody);
                log.debug("[AI_RERANKER] Payload: {}", debugBody);
            } catch (Exception e) {}

            HttpEntity<GptRequest> entity = new HttpEntity<>(requestBody, headers);

            // 3. Call API
            ResponseEntity<GptResponse> response = restTemplate.exchange(
                    gmsUrl, HttpMethod.POST, entity, GptResponse.class
            );

            if (response.getBody() == null || response.getBody().choices == null || response.getBody().choices.isEmpty()) {
                log.warn("[AI_RERANKER] Empty response from GMS.");
                return null;
            }

            // 4. Parse Output (Robust)
            String content = response.getBody().choices.get(0).message.content;
            
            // Extract JSON part only
            int startIndex = content.indexOf("{");
            int endIndex = content.lastIndexOf("}");
            
            if (startIndex == -1 || endIndex == -1) {
                // Fallback gracefully instead of throwing exception
                log.warn("[AI_RERANKER] No JSON found in response. Content: {}", 
                        content.length() > 100 ? content.substring(0, 100) + "..." : content);
                return null;
            }
            
            content = content.substring(startIndex, endIndex + 1);
            
            AiOutput output = objectMapper.readValue(content, AiOutput.class);

            if (output.selectedId == null) {
                log.warn("[AI_RERANKER] No selectedId in AI response.");
                return null;
            }

            // 5. Match & Log
            ShoppingProduct selected = candidates.stream()
                    .filter(p -> output.selectedId.equals(p.getExternalId()))
                    .findFirst()
                    .orElse(null);

            if (selected != null) {
                log.info("[AI_RERANKER] SUCCESS ingredient={} selected=\"{}\" reason=\"{}\"",
                        ingredient, selected.getTitle(), output.reason);
            } else {
                 log.warn("[AI_RERANKER] Selected ID {} not found in candidates.", output.selectedId);
            }
            return selected;

        } catch (Exception e) {
            log.error("[AI_RERANKER] FAIL ingredient={} error={}", ingredient, e.getMessage());
            return null; // Fallback
        }
    }

    // --- DTOs ---

    static class AiOutput {
        public String selectedId;
        public String reason;
    }

    static class GptRequest {
        public String model;
        public List<Message> messages = new ArrayList<>();
        public double temperature;
        // response_format removed

        public GptRequest(String model, double temperature) {
            this.model = model;
            this.temperature = temperature;
        }
    }
    // ... rest of classes

    static class Message {
        public String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    static class GptResponse {
        public List<Choice> choices;
    }

    static class Choice {
        public Message message;
    }
}
