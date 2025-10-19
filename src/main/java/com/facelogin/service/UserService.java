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
    
    // Adjust these thresholds based on testing
    private static final double REGISTRATION_THRESHOLD = 0.3 ;  // Lower = more similar
    private static final double LOGIN_THRESHOLD = 0.5;         // Lower = more strict
    
    public User registerUser(String username, String email, MultipartFile faceImage) throws Exception {
        System.out.println("\\n🎯 ===== OPENCV REGISTRATION START =====");
        System.out.println("👤 Registering user: " + username);
        
        // Check if OpenCV is loaded
        if (!faceRecognition.isOpenCVLoaded()) {
            throw new RuntimeException("OpenCV face recognition not available");
        }
        
        // Check if username exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        
        // Detect face and extract features using OpenCV
        OpenCVFaceRecognition.FaceDetectionResult result = faceRecognition.detectAndExtractFace(faceImage);
        List<Double> newFeatures = result.getFeatures();
        System.out.println("✅ OpenCV generated " + newFeatures.size() + " features");
        
        // Check for duplicate faces
        if (isFaceAlreadyRegistered(newFeatures)) {
            throw new RuntimeException("This face is already registered with another account");
        }
        
        // Create and save user
        User user = new User(username, email);
        user.setFaceEncoding(convertFeaturesToString(newFeatures));
        user.setFaceImagePath(result.getFaceImagePath());
        
        User savedUser = userRepository.save(user);
        System.out.println("🎉 OPENCV REGISTRATION SUCCESS: " + savedUser.getUsername());
        System.out.println("===== REGISTRATION END =====\\n");
        return savedUser;
    }
    
    private boolean isFaceAlreadyRegistered(List<Double> newFeatures) {
        List<User> allUsers = userRepository.findAll();
        
        if (allUsers.isEmpty()) {
            System.out.println("✅ No existing users - first registration!");
            return false;
        }
        
        System.out.println("🔍 Checking against " + allUsers.size() + " existing users...");
        
        for (User user : allUsers) {
            System.out.println("📊 Comparing with user: " + user.getUsername());
            
            List<Double> storedFeatures = parseFeatures(user.getFaceEncoding());
            System.out.println("   Stored features: " + storedFeatures.size() + ", New features: " + newFeatures.size());
            
            try {
                double distance = faceRecognition.compareFeatures(newFeatures, storedFeatures);
                System.out.println("   OpenCV distance to " + user.getUsername() + ": " + distance + " (threshold: " + REGISTRATION_THRESHOLD + ")");
                
                if (distance < REGISTRATION_THRESHOLD) {
                    System.out.println("❌ REJECTED: Face too similar to " + user.getUsername());
                    return true;
                }
            } catch (Exception e) {
                System.err.println("💥 Error comparing with " + user.getUsername() + ": " + e.getMessage());
                // Continue with other users
            }
        }
        
        System.out.println("✅ ACCEPTED: No duplicate faces found");
        return false;
    }
    
    public Optional<User> loginWithFace(MultipartFile faceImage) throws Exception {
        System.out.println("\\n🔐 ===== OPENCV LOGIN ATTEMPT =====");
        
        // Check if OpenCV is loaded
        if (!faceRecognition.isOpenCVLoaded()) {
            throw new RuntimeException("OpenCV face recognition not available");
        }
        
        // Extract features from login image using OpenCV
        OpenCVFaceRecognition.FaceDetectionResult result = faceRecognition.detectAndExtractFace(faceImage);
        List<Double> loginFeatures = result.getFeatures();
        System.out.println("✅ OpenCV extracted " + loginFeatures.size() + " login features");
        
        // Compare with all registered users
        List<User> allUsers = userRepository.findAll();
        User bestMatch = null;
        double bestScore = Double.MAX_VALUE;
        
        System.out.println("🔍 Comparing with " + allUsers.size() + " users...");
        
        for (User user : allUsers) {
            List<Double> storedFeatures = parseFeatures(user.getFaceEncoding());
            
            try {
                double distance = faceRecognition.compareFeatures(loginFeatures, storedFeatures);
                System.out.println("   " + user.getUsername() + ": OpenCV distance = " + distance);
                
                if (distance < bestScore && distance < LOGIN_THRESHOLD) {
                    bestScore = distance;
                    bestMatch = user;
                    System.out.println("   👉 New best match!");
                }
            } catch (Exception e) {
                System.err.println("💥 Error comparing with " + user.getUsername() + ": " + e.getMessage());
            }
        }
        
        if (bestMatch != null) {
            System.out.println("✅ OPENCV LOGIN SUCCESS: " + bestMatch.getUsername() + " (score: " + bestScore + ")");
            System.out.println("===== LOGIN END =====\\n");
        } else {
            System.out.println("❌ OPENCV LOGIN FAILED: No match found (best score: " + bestScore + ")");
            System.out.println("===== LOGIN END =====\\n");
        }
        
        return Optional.ofNullable(bestMatch);
    }
    
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
    
    private List<Double> parseFeatures(String featuresString) {
        List<Double> features = new ArrayList<>();
        try {
            String cleanString = featuresString.replace("[", "").replace("]", "");
            String[] parts = cleanString.split(",");
            for (String part : parts) {
                features.add(Double.parseDouble(part.trim()));
            }
            System.out.println("📈 Parsed " + features.size() + " features from database");
        } catch (Exception e) {
            System.err.println("❌ Error parsing features: " + e.getMessage());
        }
        return features;
    }
    
    public String getSystemStatus() {
        String opencvStatus = faceRecognition.isOpenCVLoaded() ? 
            "OpenCV loaded (" + faceRecognition.getFeatureSize() + " features)" : 
            "OpenCV not available";
        long userCount = userRepository.count();
        return "System ready - " + opencvStatus + ", " + userCount + " users registered";
    }
}