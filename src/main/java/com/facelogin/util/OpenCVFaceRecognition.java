package com.facelogin.util;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenCVFaceRecognition {
    
    private CascadeClassifier faceDetector;
    private boolean openCVLoaded = false;
    private static final int FIXED_FEATURE_SIZE = 256; // Fixed size for all feature vectors
    
    @PostConstruct
    public void init() {
        try {
            // Load OpenCV native library
            nu.pattern.OpenCV.loadShared();
            System.out.println("✅ OpenCV loaded successfully! Version: " + Core.VERSION);
            openCVLoaded = true;
            
            // Load face detection classifier
            loadClassifiers();
            
        } catch (Exception e) {
            System.err.println("❌ Error loading OpenCV: " + e.getMessage());
            e.printStackTrace();
            openCVLoaded = false;
        }
    }
    
    private void loadClassifiers() {
        try {
            // Load face detector
            ClassPathResource faceResource = new ClassPathResource("haarcascades/haarcascade_frontalface_default.xml");
            File faceFile = File.createTempFile("haarcascade_frontalface", ".xml");
            try (InputStream inputStream = faceResource.getInputStream()) {
                Files.copy(inputStream, faceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            faceDetector = new CascadeClassifier(faceFile.getAbsolutePath());
            
            if (faceDetector.empty()) {
                System.err.println("❌ Error loading face detector!");
            } else {
                System.out.println("✅ Face detector loaded successfully!");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error loading classifiers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public FaceDetectionResult detectAndExtractFace(MultipartFile multipartFile) throws Exception {
        if (!openCVLoaded) {
            throw new RuntimeException("OpenCV not loaded properly");
        }
        
        // Save uploaded file temporarily
        File tempFile = File.createTempFile("face_input", ".jpg");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }
        
        try {
            // Read image
            Mat image = Imgcodecs.imread(tempFile.getAbsolutePath());
            if (image.empty()) {
                throw new RuntimeException("Cannot read image file");
            }
            
            // Convert to grayscale for face detection
            Mat gray = new Mat();
            Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
            
            // Detect faces
            MatOfRect faceDetections = new MatOfRect();
            faceDetector.detectMultiScale(gray, faceDetections, 1.1, 3, 0, new Size(30, 30));
            
            Rect[] facesArray = faceDetections.toArray();
            if (facesArray.length == 0) {
                throw new RuntimeException("No face detected in the image");
            }
            
            // Use the largest face found
            Rect largestFace = getLargestFace(facesArray);
            
            // Extract face region
            Mat faceRegion = new Mat(gray, largestFace);
            
            // Extract features from the face with FIXED size
            List<Double> features = extractFixedSizeFeatures(faceRegion);
            
            // Save the detected face image
            String faceImagePath = saveDetectedFace(image, largestFace, "detected_face");
            
            System.out.println("✅ OpenCV generated " + features.size() + " features");
            return new FaceDetectionResult(features, faceImagePath, largestFace);
            
        } finally {
            // Clean up temp file
            tempFile.delete();
        }
    }
    
    private Rect getLargestFace(Rect[] faces) {
        Rect largest = faces[0];
        for (Rect face : faces) {
            if (face.area() > largest.area()) {
                largest = face;
            }
        }
        return largest;
    }
    
    private List<Double> extractFixedSizeFeatures(Mat faceImage) {
        // Resize to standard size for consistent feature extraction
        Mat resized = new Mat();
        Imgproc.resize(faceImage, resized, new Size(100, 100));
        
        // Extract multiple feature types to ensure consistent size
        List<Double> allFeatures = new ArrayList<>();
        
        // 1. Basic statistics features
        allFeatures.addAll(extractStatisticalFeatures(resized));
        
        // 2. Histogram features
        allFeatures.addAll(extractHistogramFeatures(resized));
        
        // 3. Grid-based features
        allFeatures.addAll(extractGridFeatures(resized));
        
        // 4. LBP-like features (simplified)
        allFeatures.addAll(extractTextureFeatures(resized));
        
        // Ensure we have exactly FIXED_FEATURE_SIZE features
        return normalizeFeatureSize(allFeatures);
    }
    
    private List<Double> extractStatisticalFeatures(Mat image) {
        List<Double> features = new ArrayList<>();
        
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stddev = new MatOfDouble();
        Core.meanStdDev(image, mean, stddev);
        
        // Add mean and standard deviation
        features.add(mean.get(0, 0)[0]);
        features.add(stddev.get(0, 0)[0]);
        
        // Add min and max
        Core.MinMaxLocResult minMax = Core.minMaxLoc(image);
        features.add(minMax.minVal);
        features.add(minMax.maxVal);
        
        return features;
    }
    
    private List<Double> extractHistogramFeatures(Mat image) {
        List<Double> features = new ArrayList<>();
        
        Mat histogram = new Mat();
        List<Mat> images = new ArrayList<>();
        images.add(image);
        
        // Calculate histogram
        Imgproc.calcHist(images, new MatOfInt(0), new Mat(), histogram, new MatOfInt(32), new MatOfFloat(0, 256));
        
        // Normalize histogram
        Core.normalize(histogram, histogram, 0, 1, Core.NORM_MINMAX);
        
        // Add histogram bins as features
        for (int i = 0; i < histogram.rows(); i++) {
            features.add(histogram.get(i, 0)[0]);
        }
        
        return features;
    }
    
    private List<Double> extractGridFeatures(Mat image) {
        List<Double> features = new ArrayList<>();
        
        int gridSize = 4; // 4x4 grid
        int cellWidth = image.cols() / gridSize;
        int cellHeight = image.rows() / gridSize;
        
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int x = col * cellWidth;
                int y = row * cellHeight;
                int width = Math.min(cellWidth, image.cols() - x);
                int height = Math.min(cellHeight, image.rows() - y);
                
                if (width > 0 && height > 0) {
                    Mat cell = new Mat(image, new Rect(x, y, width, height));
                    
                    // Calculate cell mean
                    Scalar mean = Core.mean(cell);
                    features.add(mean.val[0]);
                }
            }
        }
        
        return features;
    }
    
    private List<Double> extractTextureFeatures(Mat image) {
        List<Double> features = new ArrayList<>();
        
        // Simple texture features using Sobel filters
        Mat gradX = new Mat();
        Mat gradY = new Mat();
        
        Imgproc.Sobel(image, gradX, CvType.CV_32F, 1, 0);
        Imgproc.Sobel(image, gradY, CvType.CV_32F, 0, 1);
        
        // Calculate gradient magnitude
        Mat magnitude = new Mat();
        Core.magnitude(gradX, gradY, magnitude);
        
        // Add gradient statistics
        MatOfDouble gradMean = new MatOfDouble();
        MatOfDouble gradStd = new MatOfDouble();
        Core.meanStdDev(magnitude, gradMean, gradStd);
        
        features.add(gradMean.get(0, 0)[0]);
        features.add(gradStd.get(0, 0)[0]);
        
        return features;
    }
    
    private List<Double> normalizeFeatureSize(List<Double> features) {
        List<Double> normalized = new ArrayList<>();
        
        if (features.size() >= FIXED_FEATURE_SIZE) {
            // Take first FIXED_FEATURE_SIZE features
            normalized = features.subList(0, FIXED_FEATURE_SIZE);
        } else {
            // Pad with zeros if needed
            normalized = new ArrayList<>(features);
            while (normalized.size() < FIXED_FEATURE_SIZE) {
                normalized.add(0.0);
            }
        }
        
        // Normalize the feature vector
        return normalizeVector(normalized);
    }
    
    private List<Double> normalizeVector(List<Double> vector) {
        // Calculate magnitude
        double magnitude = 0;
        for (Double value : vector) {
            magnitude += value * value;
        }
        magnitude = Math.sqrt(magnitude);
        
        // Normalize
        List<Double> normalized = new ArrayList<>();
        if (magnitude > 0) {
            for (Double value : vector) {
                normalized.add(value / magnitude);
            }
        } else {
            normalized = vector;
        }
        
        return normalized;
    }
    
    private String saveDetectedFace(Mat originalImage, Rect faceRect, String prefix) {
        String uploadDir = "uploads/faces/";
        new File(uploadDir).mkdirs();
        
        // Extract face region from original color image
        Mat faceColor = new Mat(originalImage, faceRect);
        
        String filename = prefix + "_" + System.currentTimeMillis() + ".jpg";
        String filePath = uploadDir + filename;
        
        Imgcodecs.imwrite(filePath, faceColor);
        return filePath;
    }
    
    public double compareFeatures(List<Double> features1, List<Double> features2) {
        System.out.println("🔍 OpenCV comparing features - Size1: " + features1.size() + ", Size2: " + features2.size());
        
        // Ensure consistent comparison
        int minSize = Math.min(features1.size(), features2.size());
        
        if (features1.size() != features2.size()) {
            System.out.println("⚠️ OpenCV feature size mismatch, using min size: " + minSize);
        }
        
        // Calculate Euclidean distance
        double sumSquaredDiff = 0;
        int comparedCount = 0;
        
        for (int i = 0; i < minSize; i++) {
            double diff = features1.get(i) - features2.get(i);
            sumSquaredDiff += diff * diff;
            comparedCount++;
        }
        
        double distance = Math.sqrt(sumSquaredDiff);
        System.out.println("📏 OpenCV compared " + comparedCount + " features, distance: " + distance);
        
        return distance;
    }
    
    public boolean isOpenCVLoaded() {
        return openCVLoaded;
    }
    
    public int getFeatureSize() {
        return FIXED_FEATURE_SIZE;
    }
    
    // Inner class to hold face detection results
    public static class FaceDetectionResult {
        private List<Double> features;
        private String faceImagePath;
        private Rect faceRect;
        
        public FaceDetectionResult(List<Double> features, String faceImagePath, Rect faceRect) {
            this.features = features;
            this.faceImagePath = faceImagePath;
            this.faceRect = faceRect;
        }
        
        public List<Double> getFeatures() { return features; }
        public String getFaceImagePath() { return faceImagePath; }
        public Rect getFaceRect() { return faceRect; }
    }
}
