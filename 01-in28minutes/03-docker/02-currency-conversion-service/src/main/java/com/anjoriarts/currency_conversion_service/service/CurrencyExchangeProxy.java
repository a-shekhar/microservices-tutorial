package com.anjoriarts.currency_conversion_service.service;

import com.anjoriarts.currency_conversion_service.bean.CurrencyConversion;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


//@FeignClient(name = "currency-exchange", url = "http://localhost:8080")
// Feign creates the HTTP client; discovery can resolve this name to an instance.
@FeignClient(name = "currency-exchange")
public interface CurrencyExchangeProxy {

    @GetMapping("currency-exchange/from/{from}/to/{to}")
    public CurrencyConversion retrieveExchange(@PathVariable String from, @PathVariable String to);
}
