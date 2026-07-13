package com.example.soattro.ai;

import com.example.soattro.exception.BadRequestException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Lớp gọi Google Gemini dùng chung (pattern mang từ finance app sang).
 * Gói gọn: cấu hình key/model, gọi generateContent, tự thử lại khi 5xx,
 * và map lỗi HTTP (429 hết lượt / 503 quá tải) thành thông báo thân thiện.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final String BASE = "https://generativelanguage.googleapis.com/v1beta";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final boolean enabled;

    public GeminiClient(@Value("${app.ai.api-key:}") String apiKey,
                        @Value("${app.ai.model:gemini-3-flash-preview}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.enabled = apiKey != null && !apiKey.isBlank();
        this.restClient = RestClient.builder().baseUrl(BASE).build();
    }

    /** Có cấu hình key hay chưa (để frontend ẩn/hiện tính năng). */
    public boolean isEnabled() {
        return enabled;
    }

    /** Gọi generateContent với body cho sẵn; trả về JsonNode phản hồi (đã xử lý lỗi). */
    public JsonNode generate(Map<String, Object> body) {
        if (!enabled) {
            throw new BadRequestException("Chức năng AI chưa được cấu hình (thiếu GEMINI_API_KEY).");
        }
        try {
            return postWithRetry(body);
        } catch (HttpClientErrorException e) {
            log.warn("Gemini lỗi {}: {}", e.getStatusCode(), e.getMessage());
            if (e.getStatusCode().value() == 429) {
                throw new BadRequestException("Đã hết lượt AI miễn phí hôm nay — hãy thử lại sau.");
            }
            throw new BadRequestException("Không gọi được AI lúc này, hãy thử lại.");
        } catch (HttpServerErrorException e) {
            log.warn("Gemini quá tải {}", e.getStatusCode());
            throw new BadRequestException("Dịch vụ AI đang quá tải, hãy thử lại sau giây lát.");
        } catch (RuntimeException e) {
            log.warn("Gọi Gemini thất bại: {}", e.getMessage());
            throw new BadRequestException("Không gọi được AI lúc này, hãy thử lại.");
        }
    }

    /** Lấy text ở candidates[0].content.parts[0].text (null nếu không có). */
    public String firstText(JsonNode response) {
        return response.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text").asText(null);
    }

    private JsonNode postWithRetry(Map<String, Object> body) {
        HttpServerErrorException last = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return restClient.post()
                        .uri("/models/{model}:generateContent", model)
                        .header("x-goog-api-key", apiKey)
                        .header("Content-Type", "application/json")
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (HttpServerErrorException e) {
                last = e;   // 5xx tạm thời -> thử lại 1 lần
            }
        }
        throw last;
    }
}
