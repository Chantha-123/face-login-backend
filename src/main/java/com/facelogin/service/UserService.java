package com.facelogin.service;

import com.facelogin.model.User;
import com.facelogin.repository.UserRepository;
import com.facelogin.util.OpenCVFaceRecognition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OpenCVFaceRecognition faceRecognition;
    
    // Adjust thresholds as needed
    private static final double REGISTRATION_THRESHOLD = 0.3;  // Lower = more similar
    private static final double LOGIN_THRESHOLD = 0.5;         // Lower = more strict
    
    /**
     * Register new user with face recognition and store embedding.
     */
    public User registerUser(String username, String email, MultipartFile faceImage) throws Exception {
        System.out.println("\n🎯 ===== OPENCV REGISTRATION START =====");
        System.out.println("👤 Registering user: " + username);

        if (!faceRecognition.isOpenCVLoaded()) {
            throw new RuntimeException("OpenCV face recognition not available");
        }

        // Check for duplicates
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // Extract face features
        OpenCVFaceRecognition.FaceDetectionResult result = faceRecognition.detectAndExtractFace(faceImage);
        List<Double> newFeatures = result.getFeatures();
        System.out.println("✅ OpenCV generated " + newFeatures.size() + " features");

        // Check if face already registered
        if (isFaceAlreadyRegistered(newFeatures)) {
            throw new RuntimeException("This face is already registered with another account");
        }

        // Create and save user
        User user = new User(username, email);
        user.setFaceEncoding(convertFeaturesToString(newFeatures));
        user.setFaceImagePath(result.getFaceImagePath());
        user.setTelegramChatId(null); // initially not linked

        User savedUser = userRepository.save(user);

        System.out.println("🎉 OPENCV REGISTRATION SUCCESS: " + savedUser.getUsername());
        System.out.println("===== REGISTRATION END =====\n");
        return savedUser;
    }

    /**
     * Login using face recognition. Returns Optional<User> if match found.
     */
    public Optional<User> loginWithFace(MultipartFile faceImage) throws Exception {
        System.out.println("\n🔐 ===== OPENCV LOGIN ATTEMPT =====");

        if (!faceRecognition.isOpenCVLoaded()) {
            throw new RuntimeException("OpenCV face recognition not available");
        }

        OpenCVFaceRecognition.FaceDetectionResult result = faceRecognition.detectAndExtractFace(faceImage);
        List<Double> loginFeatures = result.getFeatures();
        System.out.println("✅ OpenCV extracted " + loginFeatures.size() + " login features");

        List<User> allUsers = userRepository.findAll();
        if (allUsers.isEmpty()) {
            System.out.println("⚠️ No registered users found!");
            return Optional.empty();
        }

        User bestMatch = null;
        double bestScore = Double.MAX_VALUE;

        System.out.println("🔍 Comparing with " + allUsers.size() + " users...");

        for (User user : allUsers) {
            if (user.getFaceEncoding() == null || user.getFaceEncoding().isEmpty()) continue;

            List<Double> storedFeatures = parseFeatures(user.getFaceEncoding());
            double distance = faceRecognition.compareFeatures(loginFeatures, storedFeatures);
            System.out.println("   " + user.getUsername() + " → distance = " + distance);

            if (distance < bestScore && distance < LOGIN_THRESHOLD) {
                bestScore = distance;
                bestMatch = user;
                System.out.println("   ✅ New best match → " + user.getUsername());
            }
        }

        if (bestMatch != null) {
            System.out.println("✅ LOGIN SUCCESS: " + bestMatch.getUsername() + " (score: " + bestScore + ")");
        } else {
            System.out.println("❌ LOGIN FAILED: No match found (best score: " + bestScore + ")");
        }

        System.out.println("===== LOGIN END =====\n");
        return Optional.ofNullable(bestMatch);
    }

    /**
     * Store Telegram Chat ID for a user after linking via /start <userId>.
     */
    public void saveTelegramChatId(Long userId, String chatId) {
        System.out.println("💬 Linking Telegram chatId for userId=" + userId);
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found with ID: " + userId);
        }

        User user = optionalUser.get();
        user.setTelegramChatId(chatId);
        userRepository.save(user);
        System.out.println("✅ Telegram chat linked for " + user.getUsername() + ": " + chatId);
    }

    /**
     * Helper — check if face already exists in system.
     */
    private boolean isFaceAlreadyRegistered(List<Double> newFeatures) {
        List<User> allUsers = userRepository.findAll();
        if (allUsers.isEmpty()) {
            System.out.println("✅ No existing users - first registration!");
            return false;
        }

        for (User user : allUsers) {
            if (user.getFaceEncoding() == null || user.getFaceEncoding().isEmpty()) continue;
            List<Double> storedFeatures = parseFeatures(user.getFaceEncoding());
            double distance = faceRecognition.compareFeatures(newFeatures, storedFeatures);
            if (distance < REGISTRATION_THRESHOLD) {
                System.out.println("❌ Duplicate detected with " + user.getUsername() + " (distance: " + distance + ")");
                return true;
            }
        }
        System.out.println("✅ No duplicate faces found");
        return false;
    }

    /**
     * Convert list of features → string for database storage.
     */
    private String convertFeaturesToString(List<Double> features) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < features.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(String.format("%.6f", features.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Parse feature string back into list.
     */
    private List<Double> parseFeatures(String featuresString) {
        List<Double> features = new ArrayList<>();
        try {
            String cleanString = featuresString.replace("[", "").replace("]", "");
            for (String part : cleanString.split(",")) {
                features.add(Double.parseDouble(part.trim()));
            }
        } catch (Exception e) {
            System.err.println("❌ Error parsing features: " + e.getMessage());
        }
        return features;
    }

    /**
     * Check current system and OpenCV status.
     */
    public String getSystemStatus() {
        String opencvStatus = faceRecognition.isOpenCVLoaded()
                ? "OpenCV loaded (" + faceRecognition.getFeatureSize() + " features)"
                : "OpenCV not available";
        long userCount = userRepository.count();
        return "System ready - " + opencvStatus + ", " + userCount + " users registered";
    }

    /**
     * Optional: get user by ID (for controllers or Telegram linking).
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }
}
