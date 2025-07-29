package com.anjori.limitsservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Component
@ConfigurationProperties("limits-service")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Configuration {
    private int minimum;
    private int maximum;
}
