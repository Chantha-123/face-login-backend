package com.facelogin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String botToken;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendMessage(String chatId, String text) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
            Map<String, Object> params = Map.of(
                    "chat_id", chatId,
                    "text", text,
                    "parse_mode", "HTML"
            );

            String json = new ObjectMapper().writeValueAsString(params);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Telegram response: " + response.body());

        } catch (Exception e) {
            System.err.println("Failed to send Telegram message");
            e.printStackTrace();
        }
    }
}
