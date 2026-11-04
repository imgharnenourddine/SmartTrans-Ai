package com.smarttrans.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bootstrap")
public record AdminBootstrapProperties(String adminEmail, String adminPassword, String adminFirstName, String adminLastName) {
}
