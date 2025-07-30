package com.anjori.currency_exchange_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

@RestController
public class CircuitBreakerController {

    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    @GetMapping("/sample-api")
    //@Retry(name="sample-api", fallbackMethod = "fallbackResponse")
    @CircuitBreaker(name="default", fallbackMethod = "fallbackResponse")
    public String simpleApi(){
        logger.info("Sample API call received");
        ResponseEntity<String> forEntity = new RestTemplate().getForEntity("http://localhost:8080/dummy", String.class);
        return forEntity.getBody();
    }

    public String fallbackResponse(Exception ex){
        return "fallback response";
    }
}
