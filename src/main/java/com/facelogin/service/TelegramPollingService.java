package com.facelogin.service;

import com.facelogin.service.TelegramService;
import com.facelogin.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;

@Service
public class TelegramPollingService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private long lastUpdateId = 0; // Track the last update to avoid duplicates

    private final UserService userService;
    private final TelegramService telegramService;

    public TelegramPollingService(UserService userService, TelegramService telegramService) {
        this.userService = userService;
        this.telegramService = telegramService;
    }

    @Scheduled(fixedDelay = 2000) // Poll every 2 seconds
    public void pollUpdates() {
        try {
            String url = "https://api.telegram.org/bot" + botToken +
                    "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=10";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (!root.get("ok").asBoolean()) return;

            Iterator<JsonNode> updates = root.get("result").elements();
            while (updates.hasNext()) {
                JsonNode update = updates.next();
                lastUpdateId = update.get("update_id").asLong();

                JsonNode message = update.has("message") ? update.get("message") : null;
                if (message == null) continue;

                JsonNode chat = message.get("chat");
                String chatId = chat.get("id").asText();
                String text = message.has("text") ? message.get("text").asText() : "";

                if (text.startsWith("/start ")) {
                    Long userId = Long.parseLong(text.substring(7).trim());
                    userService.saveTelegramChatId(userId, chatId);

                    telegramService.sendMessage(chatId,
                            "✅ Telegram linked successfully! You will receive login notifications.");
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to poll Telegram updates");
            e.printStackTrace();
        }
    }
}
