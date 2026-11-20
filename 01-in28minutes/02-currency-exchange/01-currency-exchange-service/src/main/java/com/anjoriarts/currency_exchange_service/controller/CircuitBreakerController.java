package com.anjoriarts.currency_exchange_service.controller;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
//@Retry(name = "sample-api", fallbackMethod = "errorResponse")
public class CircuitBreakerController {

    Logger logger = LoggerFactory.getLogger(getClass().getName());

    @GetMapping("/sample-api")
    // Failures can use errorResponse; the bulkhead limits concurrent calls.
    @CircuitBreaker(name = "default", fallbackMethod = "errorResponse")
    //@RateLimiter(name = "default")
    @Bulkhead(name="default")
    public String sampleApi(){
        logger.info("Sample Call received");
        ResponseEntity<String> forEntity =
                new RestTemplate().getForEntity("http://localhost:8080/ex", String.class);
        //return forEntity.getBody();
        return "Sample API";
    }

    // Resilience4j may call this fallback when the protected invocation fails.
    public String errorResponse(Exception exception){
        return "Error Response for failure";
    }
}