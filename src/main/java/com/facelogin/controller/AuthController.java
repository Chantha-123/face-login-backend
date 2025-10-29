package com.facelogin.controller;

import com.facelogin.model.User;
import com.facelogin.service.UserService;
import com.facelogin.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TelegramService telegramService;

    @Value("${telegram.admin.chat-id}")
    private String adminChatId;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("faceImage") MultipartFile faceImage) {

        try {
            if (faceImage.isEmpty()) throw new RuntimeException("Please select an image file");

            User user = userService.registerUser(username, email, faceImage);
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss");
            currentDateTime.format(formatter);
            // Notify admin
           

            // Telegram link to be sent manually by admin
            String telegramLink = "https://t.me/FaceLoginDIPBot?start=" + user.getId();
            telegramService.sendMessage(adminChatId,
                    "🆕 New user registered User Name: " + user.getUsername() + " Email: (" + user.getEmail() + ")" + " Register Date: " + currentDateTime.format(formatter)
                    +" Telegram: "+ 
                		telegramLink );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully with face recognition");
            response.put("user", Map.of(
                    "id:", user.getId(),
                    "username:", user.getUsername(),
                    "email:", user.getEmail(),
                    "telegramlink:", telegramLink,
                    "registerdatetime:",currentDateTime
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam("faceImage") MultipartFile faceImage) {
        try {
            if (faceImage.isEmpty()) throw new RuntimeException("Please select an image file");

            Optional<User> userOpt = userService.loginWithFace(faceImage);

            if (userOpt.isPresent()) {
                User user = userOpt.get();
                LocalDateTime currentDateTime = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, yyyy-MM-dd HH:mm:ss");
               

                // Notify admin
                telegramService.sendMessage(adminChatId,
                        "👤 User logged in: " + user.getUsername() + " (" + user.getEmail() + ")"+" Login Date: " +  currentDateTime.format(formatter));

                // Notify user if Telegram linked
                if (user.getTelegramChatId() != null && !user.getTelegramChatId().isEmpty()) {
                    telegramService.sendMessage(user.getTelegramChatId(),
                            "👋 Welcome back, " + user.getUsername() + "!" + " Login Date: " + currentDateTime.format(formatter));
                }

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Face recognition login successful");
                response.put("user", Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "logindatetime",currentDateTime
                ));
                return ResponseEntity.ok(response);

            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Face recognition failed - no matching user found");
                return ResponseEntity.status(401).body(response);
            }

        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Login failed: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
