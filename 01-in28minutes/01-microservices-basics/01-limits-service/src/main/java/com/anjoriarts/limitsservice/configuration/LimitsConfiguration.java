package com.anjoriarts.limitsservice.configuration;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
// Bind limits-service.minimum and limits-service.maximum from Spring configuration.
@ConfigurationProperties(prefix = "limits-service")
@Data
public class LimitsConfiguration {
    int minimum;
    int maximum;
}
