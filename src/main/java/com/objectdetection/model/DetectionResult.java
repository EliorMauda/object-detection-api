package com.objectdetection.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DetectionResult {
    private String imageUrl;
    private List<DetectedObject> detectedObjects;
    private String error;
    private Long processingTimeMs;
}