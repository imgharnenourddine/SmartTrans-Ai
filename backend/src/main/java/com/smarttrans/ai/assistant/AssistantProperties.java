package com.smarttrans.ai.assistant;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.assistant")
public record AssistantProperties(String openaiApiKey, String openaiModel, String openaiBaseUrl) {
}
