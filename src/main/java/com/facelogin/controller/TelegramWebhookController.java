package com.facelogin.controller;

import com.facelogin.service.UserService;
import com.facelogin.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/telegram")
public class TelegramWebhookController {

    @Autowired
    private UserService userService;

    @Autowired
    private TelegramService telegramService;

    @PostMapping("/webhook")
    public void onUpdateReceived(@RequestBody Map<String, Object> update) {
        try {
            // Log the raw update for debugging
            System.out.println("Received Telegram update: " + update);

            // Telegram message can be in "message" or "edited_message"
            Map<String, Object> message = (Map<String, Object>) update.get("message");
            if (message == null) return;

            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
            if (chat == null) return;

            String chatId = String.valueOf(chat.get("id"));
            String text = (String) message.get("text");

            if (text != null && text.startsWith("/start ")) {
                // Extract userId
                Long userId = Long.parseLong(text.substring(7).trim());

                // Save chatId
                userService.saveTelegramChatId(userId, chatId);

                // Send confirmation message to user
                telegramService.sendMessage(chatId,
                        "✅ Telegram linked successfully! You will receive login notifications.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
