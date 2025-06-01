package com.objectdetection.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.objectdetection.model.BoundingBox;
import com.objectdetection.model.DetectedObject;
import com.objectdetection.model.DetectionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectDetectionService {

    private final ImageStorageService imageStorageService;
    private final ObjectMapper objectMapper;

    @Value("${huggingface.api.url}")
    private String huggingFaceApiUrl;

    @Value("${huggingface.api.token}")
    private String huggingFaceApiToken;

    public DetectionResult detectObjectsFromFile(MultipartFile file) {
        long startTime = System.currentTimeMillis();
        try {
            // Upload the image to cloud storage
            String imageUrl = imageStorageService.uploadImage(file);
            
            // Process the image with Hugging Face API
            List<DetectedObject> detectedObjects = processImageWithHuggingFace(file.getBytes());
            
            return DetectionResult.builder()
                    .imageUrl(imageUrl)
                    .detectedObjects(detectedObjects)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("Error detecting objects from file", e);
            return DetectionResult.builder()
                    .error("Error processing image: " + e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    public DetectionResult detectObjectsFromUrl(String url) {
        long startTime = System.currentTimeMillis();
        try {
            // Download image and upload to cloud storage
            String imageUrl = imageStorageService.uploadImage(url);
            
            // Download image for processing
            byte[] imageBytes = downloadImage(url);
            
            // Process the image with Hugging Face API
            List<DetectedObject> detectedObjects = processImageWithHuggingFace(imageBytes);
            
            return DetectionResult.builder()
                    .imageUrl(imageUrl)
                    .detectedObjects(detectedObjects)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("Error detecting objects from URL: {}", url, e);
            return DetectionResult.builder()
                    .error("Error processing image: " + e.getMessage())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    private List<DetectedObject> processImageWithHuggingFace(byte[] imageBytes) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(huggingFaceApiUrl);
            
            // Set headers
            request.setHeader("Authorization", "Bearer " + huggingFaceApiToken);
            
            // Set image bytes as request body
            HttpEntity entity = new ByteArrayEntity(imageBytes, ContentType.IMAGE_JPEG);
            request.setEntity(entity);
            
            // Execute request
            try (CloseableHttpResponse response = client.execute(request)) {
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity);
                
                // Parse response
                return parseHuggingFaceResponse(responseString);
            }
        }
    }

    private List<DetectedObject> parseHuggingFaceResponse(String responseString) throws JsonProcessingException {
        List<DetectedObject> detectedObjects = new ArrayList<>();
        
        JsonNode rootNode = objectMapper.readTree(responseString);
        
        System.out.println("Hugging Face response: " + responseString);
        
        if (rootNode.isArray()) {
            for (JsonNode objectNode : rootNode) {
                String label = objectNode.path("label").asText();
                float score = objectNode.path("score").floatValue();
                
                JsonNode boxNode = objectNode.path("box");
                BoundingBox box = BoundingBox.builder()
                        .xMin(boxNode.path("xmin").floatValue())
                        .yMin(boxNode.path("ymin").floatValue())
                        .xMax(boxNode.path("xmax").floatValue())
                        .yMax(boxNode.path("ymax").floatValue())
                        .build();
                
                DetectedObject detectedObject = DetectedObject.builder()
                        .label(label)
                        .confidence(score)
                        .box(box)
                        .build();
                
                detectedObjects.add(detectedObject);
            }
        }
        
        return detectedObjects;
    }

    private byte[] downloadImage(String imageUrl) throws IOException {
        try (InputStream in = new URL(imageUrl).openStream()) {
            return in.readAllBytes();
        }
    }
}