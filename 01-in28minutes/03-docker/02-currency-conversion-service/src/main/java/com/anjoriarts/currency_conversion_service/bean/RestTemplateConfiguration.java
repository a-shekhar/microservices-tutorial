package com.anjoriarts.currency_conversion_service.bean;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// No direct calls between @Bean methods: a configuration proxy is unnecessary.
@Configuration(proxyBeanMethods = false)
public class RestTemplateConfiguration {
    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder){
        // Spring's injected builder applies observation customizers, including trace propagation.
        // A manually constructed new RestTemplate() does not receive those customizers.
        return builder.build();
    }
}
