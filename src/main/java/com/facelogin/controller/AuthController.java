package com.facelogin.controller;

import com.facelogin.model.User;
import com.facelogin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("faceImage") MultipartFile faceImage) {
        
        try {
            if (faceImage.isEmpty()) {
                throw new RuntimeException("Please select an image file");
            }
            
            User user = userService.registerUser(username, email, faceImage);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully with face recognition");
            response.put("user", Map.of(
                "id", user.getId(),
                "username", user.getUsername(),
                "email", user.getEmail()
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
            if (faceImage.isEmpty()) {
                throw new RuntimeException("Please select an image file");
            }
            
            Optional<User> user = userService.loginWithFace(faceImage);
            
            if (user.isPresent()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Face recognition login successful");
                response.put("user", Map.of(
                    "id", user.get().getId(),
                    "username", user.get().getUsername(),
                    "email", user.get().getEmail()
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
    
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Face Login API is running");
        response.put("system", userService.getSystemStatus());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/status")
    public ResponseEntity<?> systemStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "Face Login System");
        response.put("status", "running");
        response.put("system", userService.getSystemStatus());
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}