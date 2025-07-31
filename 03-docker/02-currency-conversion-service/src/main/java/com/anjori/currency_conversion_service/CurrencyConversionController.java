package com.anjori.currency_conversion_service;

import java.math.BigDecimal;
import java.util.HashMap;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;


@RestController
public class CurrencyConversionController {

    private final CurrencyExchangeProxy proxy;

    public CurrencyConversionController(CurrencyExchangeProxy proxy){
        this.proxy = proxy;
    }

    @GetMapping("/currency-exchange/from/{from}/to/{to}/quantity/{qty}")
    public CurrencyConversion calculateCurrencyConversion(
        @PathVariable String from,
        @PathVariable String to,
        @PathVariable BigDecimal qty
    ){

        HashMap<String, String> urivariables = new HashMap<>();
        urivariables.put("from", from);
        urivariables.put("to", to);

        ResponseEntity<CurrencyConversion> responseEntity = new RestTemplate().getForEntity
        ("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
         CurrencyConversion.class,
         urivariables);

        CurrencyConversion currencyConversion = responseEntity.getBody();
        
        return new CurrencyConversion().builder()
        .id(10001L)
        .from(from)
        .to(to)
        .conversionMultiple(currencyConversion.getConversionMultiple())
        .quantity(qty)
        .totalCalculatedAmount(qty.multiply(currencyConversion.getConversionMultiple()))
        .environment(currencyConversion.getEnvironment() + " rest")
        .build();
    }

    
    @GetMapping("/currency-exchange-feign/from/{from}/to/{to}/quantity/{qty}")
    public CurrencyConversion calculateCurrencyConversionFeign(
        @PathVariable String from,
        @PathVariable String to,
        @PathVariable BigDecimal qty
    ){

        CurrencyConversion currencyConversion = proxy.retrieveExchangeValue(from, to);
        
        return new CurrencyConversion().builder()
        .id(10001L)
        .from(from)
        .to(to)
        .conversionMultiple(currencyConversion.getConversionMultiple())
        .quantity(qty)
        .totalCalculatedAmount(qty.multiply(currencyConversion.getConversionMultiple()))
        .environment(currencyConversion.getEnvironment() + " feign")
        .build();
    }


    @GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{qty}")
    public CurrencyConversion calculateCurrencyConversionV2(
        @PathVariable String from,
        @PathVariable String to,
        @PathVariable BigDecimal qty
    ){

        HashMap<String, String> urivariables = new HashMap<>();
        urivariables.put("from", from);
        urivariables.put("to", to);

        ResponseEntity<CurrencyConversion> responseEntity = new RestTemplate().getForEntity
        ("http://localhost:8000/currency-exchange/from/{from}/to/{to}",
         CurrencyConversion.class,
         urivariables);

        CurrencyConversion currencyConversion = responseEntity.getBody();
        
        return new CurrencyConversion().builder()
        .id(10001L)
        .from(from)
        .to(to)
        .conversionMultiple(currencyConversion.getConversionMultiple())
        .quantity(qty)
        .totalCalculatedAmount(qty.multiply(currencyConversion.getConversionMultiple()))
        .environment(currencyConversion.getEnvironment() + " rest")
        .build();
    }

    
    @GetMapping("/currency-conversion-feign/from/{from}/to/{to}/quantity/{qty}")
    public CurrencyConversion calculateCurrencyConversionFeignV2(
        @PathVariable String from,
        @PathVariable String to,
        @PathVariable BigDecimal qty
    ){

        CurrencyConversion currencyConversion = proxy.retrieveExchangeValue(from, to);
        
        return new CurrencyConversion().builder()
        .id(10001L)
        .from(from)
        .to(to)
        .conversionMultiple(currencyConversion.getConversionMultiple())
        .quantity(qty)
        .totalCalculatedAmount(qty.multiply(currencyConversion.getConversionMultiple()))
        .environment(currencyConversion.getEnvironment() + " feign")
        .build();
    }
}
