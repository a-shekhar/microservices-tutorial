package com.anjori.currency_conversion_service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

//@FeignClient(name = "currency-exchange", url="localhost:8000/")
//@FeignClient(name = "currency-exchange")
//@FeignClient(name="CURRENCY-EXCHANGE-SERVICE") // should be same as registered with eureka
@FeignClient(name="currency-exchange-service")
public interface CurrencyExchangeProxy {
        
    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyConversion retrieveExchangeValue(
        @PathVariable String from,
        @PathVariable String to
    );


    // @GetMapping("/currency-exchange/from/{from}/to/{to}/quantity/{quantity}")
    // public CurrencyConversion retrieveTotalExchangeValue(
    //     @PathVariable String from,
    //     @PathVariable String to,
    //     @PathVariable BigDecimal quantity
    // );
}
