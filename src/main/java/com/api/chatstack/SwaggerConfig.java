package com.api.chatstack;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Configuration
public class SwaggerConfig {
    private static final String SWAGGER_FILE = "swagger/ChatStack.yaml";

    @Bean
    public OpenAPI openAPI() {
        try {
            // Read the YAML file content
            String content = new String(Files.readAllBytes(Paths.get(SWAGGER_FILE)));

            // Parse the YAML content into OpenAPI object
            OpenAPIV3Parser parser = new OpenAPIV3Parser();
            return parser.readContents(content, null, null).getOpenAPI();

        } catch (IOException e) {
            throw new RuntimeException("Failed to load Swagger YAML file: " + SWAGGER_FILE, e);
        }
    }
}