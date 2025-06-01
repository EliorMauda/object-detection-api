package com.objectdetection.model;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class UrlRequest {
    @NotBlank(message = "URL cannot be empty")
    @Pattern(regexp = "^(https?|ftp)://.*$", message = "Invalid URL format")
    private String url;
}