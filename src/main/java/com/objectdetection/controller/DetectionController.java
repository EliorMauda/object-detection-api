package com.objectdetection.controller;

import com.objectdetection.model.DetectionResult;
import com.objectdetection.model.UrlRequest;
import com.objectdetection.service.ObjectDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/detect")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000", "https://object-detection-portal-production.up.railway.app"})@Tag(name = "Object Detection", description = "Object detection operations")
public class DetectionController {

    private final ObjectDetectionService objectDetectionService;
    
    @Autowired
    private DashboardService dashboardService;

    /**
     * Detect objects in an uploaded image file
     */
    @Operation(summary = "Detect objects in an image file",
            description = "Upload an image file and get detected objects with bounding boxes",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Detection successful",
                            content = @Content(schema = @Schema(implementation = DetectionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DetectionResult> detectFromImage(
            @RequestParam("image") MultipartFile imageFile,
            HttpServletRequest request) {
        
        log.info("Received request to detect objects in image file: {}", imageFile.getOriginalFilename());
        
        long startTime = System.currentTimeMillis();
        String deviceInfo = getDeviceInfo(request);
        
        try {
            if (imageFile.isEmpty()) {
                dashboardService.recordError("Empty file uploaded", "EMPTY_FILE_ERROR");
                return ResponseEntity.badRequest().body(
                        DetectionResult.builder().error("Empty file").build()
                );
            }
            
            DetectionResult result = objectDetectionService.detectObjectsFromFile(imageFile);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record statistics if detection was successful
            if (result != null && result.getError() == null && result.getDetectedObjects() != null) {
                List<Map<String, Object>> detectedObjectsForStats = convertToStatisticsFormat(result.getDetectedObjects());
                
                // Include image URL in the statistics
                String imageUrl = extractImageUrl(result);
                
                dashboardService.recordDetection(
                    detectedObjectsForStats, 
                    processingTime, 
                    deviceInfo,
                    imageUrl,
                    imageFile.getOriginalFilename()
                );
                
                log.info("Recorded detection statistics: {} objects detected in {}ms, image URL: {}", 
                         result.getDetectedObjects().size(), processingTime, imageUrl);
            } else if (result != null && result.getError() != null) {
                dashboardService.recordError(result.getError(), "DETECTION_ERROR");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error processing image file: {}", e.getMessage(), e);
            dashboardService.recordError(e.getMessage(), "FILE_PROCESSING_ERROR");
            
            return ResponseEntity.badRequest().body(
                    DetectionResult.builder()
                            .error("Failed to process image: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Detect objects in an image from URL
     */
    @Operation(summary = "Detect objects in an image from URL",
            description = "Provide an image URL and get detected objects with bounding boxes",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Detection successful",
                            content = @Content(schema = @Schema(implementation = DetectionResult.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid input")
            })
    @PostMapping("/url")
    public ResponseEntity<DetectionResult> detectFromUrl(
            @RequestBody @Valid UrlRequest urlRequest,
            HttpServletRequest request) {
        
        log.info("Received request to detect objects in image from URL: {}", urlRequest.getUrl());
        
        long startTime = System.currentTimeMillis();
        String deviceInfo = getDeviceInfo(request);
        
        try {
            DetectionResult result = objectDetectionService.detectObjectsFromUrl(urlRequest.getUrl());
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Record statistics if detection was successful
            if (result != null && result.getError() == null && result.getDetectedObjects() != null) {
                List<Map<String, Object>> detectedObjectsForStats = convertToStatisticsFormat(result.getDetectedObjects());
                
                // Include image URL in the statistics
                String imageUrl = extractImageUrl(result);
                if (imageUrl == null) {
                    imageUrl = urlRequest.getUrl(); // Fallback to original URL
                }
                
                dashboardService.recordDetection(
                    detectedObjectsForStats, 
                    processingTime, 
                    deviceInfo,
                    imageUrl,
                    "URL: " + urlRequest.getUrl()
                );
                
                log.info("Recorded detection statistics: {} objects detected in {}ms, image URL: {}", 
                         result.getDetectedObjects().size(), processingTime, imageUrl);
            } else if (result != null && result.getError() != null) {
                dashboardService.recordError(result.getError(), "DETECTION_ERROR");
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("Error processing image from URL: {}", e.getMessage(), e);
            dashboardService.recordError(e.getMessage(), "URL_PROCESSING_ERROR");
            
            return ResponseEntity.badRequest().body(
                    DetectionResult.builder()
                            .error("Failed to process image from URL: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * Get a specific detection record by ID
     */
    @Operation(summary = "Get detection record by ID",
            description = "Retrieve detailed information about a specific detection",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Detection found"),
                    @ApiResponse(responseCode = "404", description = "Detection not found")
            })
    @GetMapping("/{detectionId}")
    public ResponseEntity<?> getDetectionById(@PathVariable String detectionId) {
        log.info("Received request to get detection with ID: {}", detectionId);
        
        try {
            // Get detection from dashboard service
            Map<String, Object> detection = dashboardService.getDetectionById(detectionId);
            
            if (detection == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Detection not found");
                errorResponse.put("detectionId", detectionId);
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(detection);
            
        } catch (Exception e) {
            log.error("Error retrieving detection {}: {}", detectionId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve detection: " + e.getMessage());
            errorResponse.put("detectionId", detectionId);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Delete a specific detection record by ID
     */
    @Operation(summary = "Delete detection record by ID",
            description = "Remove a detection record from the system",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Detection deleted successfully"),
                    @ApiResponse(responseCode = "404", description = "Detection not found"),
                    @ApiResponse(responseCode = "500", description = "Failed to delete detection")
            })
    @DeleteMapping("/{detectionId}")
    public ResponseEntity<?> deleteDetection(@PathVariable String detectionId, HttpServletRequest request) {
        log.info("Received request to delete detection with ID: {}", detectionId);
        
        String deviceInfo = getDeviceInfo(request);
        
        try {
            // Check if detection exists
            Map<String, Object> detection = dashboardService.getDetectionById(detectionId);
            if (detection == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Detection not found");
                errorResponse.put("detectionId", detectionId);
                return ((BodyBuilder) ResponseEntity.notFound()).body(errorResponse);
            }
            
            // Delete the detection
            boolean deleted = dashboardService.deleteDetection(detectionId);
            
            if (deleted) {
                // Log the deletion
                dashboardService.recordDeletion(detectionId, deviceInfo);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Detection deleted successfully");
                response.put("detectionId", detectionId);
                response.put("deletedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                response.put("deletedBy", deviceInfo);
                
                log.info("Successfully deleted detection {} by device: {}", detectionId, deviceInfo);
                return ResponseEntity.ok(response);
                
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to delete detection");
                errorResponse.put("detectionId", detectionId);
                
                return ResponseEntity.internalServerError().body(errorResponse);
            }
            
        } catch (Exception e) {
            log.error("Error deleting detection {}: {}", detectionId, e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to delete detection: " + e.getMessage());
            errorResponse.put("detectionId", detectionId);
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Get all detection records with pagination and filtering
     */
    @Operation(summary = "Get all detection records",
            description = "Retrieve detection records with optional filtering and pagination",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Detections retrieved successfully")
            })
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllDetections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String device,
            @RequestParam(required = false) String search) {
        
        log.info("Received request to get all detections - page: {}, size: {}, category: {}, device: {}, search: {}", 
                 page, size, category, device, search);
        
        try {
            // Validate pagination parameters
            if (page < 0) page = 0;
            if (size <= 0 || size > 100) size = 20; // Limit max size to prevent performance issues
            
            Map<String, Object> result = dashboardService.getAllDetections(page, size, category, device, search);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error retrieving detections: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve detections: " + e.getMessage());
            errorResponse.put("detections", new ArrayList<>());
            errorResponse.put("totalCount", 0);
            errorResponse.put("page", page);
            errorResponse.put("size", size);
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Get detection statistics
     */
    @Operation(summary = "Get detection statistics",
            description = "Retrieve aggregated statistics about all detections",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
            })
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDetectionStatistics(
            @RequestParam(defaultValue = "day") String timeframe) {
        
        log.info("Received request to get detection statistics for timeframe: {}", timeframe);
        
        try {
            // Validate timeframe parameter
            if (!isValidTimeframe(timeframe)) {
                timeframe = "day";
            }
            
            Map<String, Object> statistics = dashboardService.getDetectionStatistics(timeframe);
            return ResponseEntity.ok(statistics);
            
        } catch (Exception e) {
            log.error("Error retrieving detection statistics: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statistics: " + e.getMessage());
            errorResponse.put("timeframe", timeframe);
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * Health check endpoint for detection service
     */
    @Operation(summary = "Health check",
            description = "Check if the detection service is running properly")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Basic health checks
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            health.put("service", "Object Detection API");
            health.put("version", "1.0.0");
            
            // Check if object detection service is available
            boolean serviceAvailable = objectDetectionService != null;
            health.put("objectDetectionService", serviceAvailable ? "UP" : "DOWN");
            
            // Check dashboard service
            boolean dashboardAvailable = dashboardService != null;
            health.put("dashboardService", dashboardAvailable ? "UP" : "DOWN");
            
            // Overall health
            boolean overallHealth = serviceAvailable && dashboardAvailable;
            if (!overallHealth) {
                health.put("status", "DOWN");
                return ResponseEntity.internalServerError().body(health);
            }
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage(), e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(health);
        }
    }
    
    /**
     * Extract image URL from DetectionResult - handles both Cloudinary and other URLs
     */
    private String extractImageUrl(DetectionResult result) {
        if (result == null) return null;
        
        // Try different possible field names for image URL
        if (result.getImageUrl() != null) {
            return result.getImageUrl();
        }
        
        if (result.getImageUrl() != null) {
            return result.getImageUrl();
        }
        
        if (result.getImageUrl() != null) {
            return result.getImageUrl();
        }
        
        // If your DetectionResult has other field names, add them here
        // For example: result.getCloudinaryUrl(), result.getSecureUrl(), etc.
        
        return null;
    }
    
    /**
     * Extract device information from HTTP request
     */
    private String getDeviceInfo(HttpServletRequest request) {
        if (request == null) return "Unknown Device";
        
        // Check for custom headers from frontend
        String clientType = request.getHeader("X-Client-Type");
        String deviceInfo = request.getHeader("X-Device-Info");
        String requestedWith = request.getHeader("X-Requested-With");
        
        // If we have detailed device info from frontend, parse it
        if (deviceInfo != null && !deviceInfo.trim().isEmpty()) {
            try {
                return parseClientDeviceInfo(deviceInfo, clientType);
            } catch (Exception e) {
                log.debug("Could not parse device info JSON: {}", e.getMessage());
            }
        }
        
        // Fallback to User-Agent analysis
        String userAgent = request.getHeader("User-Agent");
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        
        log.debug("Headers - User-Agent: {}, Origin: {}, Client-Type: {}", userAgent, origin, clientType);
        
        if (userAgent == null || userAgent.trim().isEmpty()) {
            if (clientType != null) {
                return clientType;
            }
            return "Unknown Client";
        }
        
        String ua = userAgent.toLowerCase();
        
        // Handle requests from your web portal
        if (clientType != null && clientType.contains("Web Portal")) {
            if (requestedWith != null && requestedWith.equals("XMLHttpRequest")) {
                return "Web Portal (AJAX)";
            }
            return "Web Portal";
        }
        
        // Handle OkHttp and other HTTP clients
        if (ua.contains("okhttp")) {
            String version = extractVersion(userAgent, "okhttp");
            if (origin != null && (origin.contains("localhost") || origin.contains("127.0.0.1"))) {
                return "Local Development (OkHttp " + version + ")";
            }
            return "Mobile App (OkHttp " + version + ")";
        }
        
        // Handle other HTTP clients
        if (ua.contains("curl")) return "cURL Client";
        if (ua.contains("postman")) return "Postman API Client";
        if (ua.contains("insomnia")) return "Insomnia API Client";
        if (ua.contains("httpie")) return "HTTPie Client";
        
        // Mobile browsers
        if (ua.contains("mobile") || ua.contains("android")) {
            if (ua.contains("chrome")) return "Android Chrome";
            if (ua.contains("firefox")) return "Android Firefox";
            if (ua.contains("samsung")) return "Samsung Browser";
            return "Android Device";
        }
        
        if (ua.contains("iphone")) {
            if (ua.contains("safari") && !ua.contains("chrome")) return "iPhone Safari";
            if (ua.contains("chrome")) return "iPhone Chrome";
            return "iPhone";
        }
        
        if (ua.contains("ipad")) {
            if (ua.contains("safari") && !ua.contains("chrome")) return "iPad Safari";
            if (ua.contains("chrome")) return "iPad Chrome";
            return "iPad";
        }
        
        // Desktop browsers
        if (ua.contains("windows")) {
            if (ua.contains("chrome") && !ua.contains("edg")) return "Windows Chrome";
            if (ua.contains("firefox")) return "Windows Firefox";
            if (ua.contains("edg")) return "Windows Edge";
            return "Windows PC";
        }
        
        if (ua.contains("macintosh") || ua.contains("mac os")) {
            if (ua.contains("chrome") && !ua.contains("edg")) return "Mac Chrome";
            if (ua.contains("firefox")) return "Mac Firefox";
            if (ua.contains("safari") && !ua.contains("chrome")) return "Mac Safari";
            return "Mac";
        }
        
        if (ua.contains("linux")) {
            if (ua.contains("chrome")) return "Linux Chrome";
            if (ua.contains("firefox")) return "Linux Firefox";
            return "Linux PC";
        }
        
        // Fallback
        if (ua.contains("mozilla")) return "Web Browser";
        
        return "Client (" + cleanUserAgent(userAgent) + ")";
    }

    /**
     * Parse detailed device info from frontend
     */
    private String parseClientDeviceInfo(String deviceInfoJson, String clientType) {
        try {
            String deviceType = extractJsonValue(deviceInfoJson, "deviceType");
            String os = extractJsonValue(deviceInfoJson, "os");
            String browser = extractJsonValue(deviceInfoJson, "browser");
            
            StringBuilder result = new StringBuilder();
            
            if (deviceType != null && !deviceType.equals("Unknown")) {
                result.append(deviceType).append(" ");
            }
            
            if (os != null && !os.equals("Unknown")) {
                result.append(os).append(" ");
            }
            
            if (browser != null && !browser.equals("Unknown")) {
                result.append(browser);
            }
            
            String finalResult = result.toString().trim();
            if (!finalResult.isEmpty()) {
                if (clientType != null && clientType.contains("Portal")) {
                    return "Web Portal (" + finalResult + ")";
                }
                return finalResult;
            }
            
        } catch (Exception e) {
            log.debug("Error parsing device info: {}", e.getMessage());
        }
        
        return clientType != null ? clientType : "Unknown Device";
    }

    /**
     * Simple JSON value extraction (without external JSON library)
     */
    private String extractJsonValue(String json, String key) {
        try {
            String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.debug("Could not extract {} from JSON: {}", key, e.getMessage());
        }
        return null;
    }

    /**
     * Extract version from user agent string
     */
    private String extractVersion(String userAgent, String client) {
        try {
            String pattern = client + "/([0-9.]+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern, java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(userAgent);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception e) {
            log.debug("Could not extract version for {}: {}", client, e.getMessage());
        }
        return "";
    }

    /**
     * Clean user agent for display
     */
    private String cleanUserAgent(String userAgent) {
        if (userAgent == null) return "Unknown";
        
        String cleaned = userAgent.replaceAll("\\([^)]*\\)", "")  // Remove parentheses content
                                  .replaceAll("\\s+", " ")         // Multiple spaces to single
                                  .trim();
        
        if (cleaned.length() > 25) {
            cleaned = cleaned.substring(0, 22) + "...";
        }
        
        return cleaned;
    }
    
    /**
     * Convert DetectionResult objects to the format expected by dashboard statistics
     */
    private List<Map<String, Object>> convertToStatisticsFormat(List<?> detectedObjects) {
        List<Map<String, Object>> statsObjects = new ArrayList<>();
        
        if (detectedObjects == null) {
            return statsObjects;
        }
        
        for (Object obj : detectedObjects) {
            Map<String, Object> statsObj = new HashMap<>();
            
            try {
                // Handle different types of detected objects
                if (obj instanceof Map) {
                    Map<?, ?> objMap = (Map<?, ?>) obj;
                    statsObj.put("label", objMap.get("label"));
                    statsObj.put("confidence", objMap.get("confidence"));
                    statsObj.put("box", objMap.get("box"));
                } else {
                    // If it's a custom object, use reflection or specific getters
                    statsObj.put("label", getFieldValue(obj, "label"));
                    statsObj.put("confidence", getFieldValue(obj, "confidence"));
                    statsObj.put("box", getFieldValue(obj, "box"));
                }
                
                statsObjects.add(statsObj);
                
            } catch (Exception e) {
                log.warn("Could not convert detected object to statistics format: {}", e.getMessage());
                // Create a basic entry
                Map<String, Object> basicObj = new HashMap<>();
                basicObj.put("label", "unknown");
                basicObj.put("confidence", 0.0);
                basicObj.put("box", new HashMap<>());
                statsObjects.add(basicObj);
            }
        }
        
        return statsObjects;
    }
    
    /**
     * Helper method to get field values using reflection or specific getters
     */
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            // Try common getter patterns
            String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            return obj.getClass().getMethod(getterName).invoke(obj);
        } catch (Exception e) {
            // If getter doesn't work, try direct field access
            try {
                return obj.getClass().getField(fieldName).get(obj);
            } catch (Exception ex) {
                log.debug("Could not get field value for {}: {}", fieldName, ex.getMessage());
                return null;
            }
        }
    }

    /**
     * Validate timeframe parameter
     */
    private boolean isValidTimeframe(String timeframe) {
        if (timeframe == null) return false;
        String tf = timeframe.toLowerCase();
        return tf.equals("hour") || tf.equals("day") || tf.equals("week") || tf.equals("month");
    }
}