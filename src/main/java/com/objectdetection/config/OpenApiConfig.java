package com.objectdetection.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Image Object Detection API")
                        .description("REST API for detecting objects in images using Hugging Face models")
                        .version("v0.1.0")
                        .contact(new Contact()
                                .name("Object Detection SDK")
                                .url("https://github.com/yourusername/ImageObjectDetectionProject"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")));
                .servers(Arrays.asList(
                        new Server()
                                .url("https://object-detection-api-production.up.railway.app")
                                .description("Production server (Railway)"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server")
                ));
    }
}
