package com.anjoriarts.currency_conversion_service.controller;

import com.anjoriarts.currency_conversion_service.service.CurrencyExchangeProxy;
import com.anjoriarts.currency_conversion_service.bean.CurrencyConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;

@RestController
public class CurrencyConversionController {

    @Autowired
    private Environment environment;

    @Autowired
    private CurrencyExchangeProxy proxy;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/currency-conversion-rest/from/{from}/to/{to}/quantity/{qty}")
    public CurrencyConversion currencyConversion(@PathVariable String from, @PathVariable String to, @PathVariable long qty){
        HashMap<String, String> uriVariables = new HashMap<>();
        uriVariables.put("from", from);
        uriVariables.put("to", to);
        // The builder-created client can propagate trace context; this URL bypasses discovery.
        // localhost refers to this application's own container if it runs in Docker.
        ResponseEntity<CurrencyConversion> rest = restTemplate.getForEntity("http://localhost:8000/currency-exchange/from/{from}/to/{to}", CurrencyConversion.class, uriVariables);
        CurrencyConversion currencyConversion = rest.getBody();
        String port = environment.getProperty("local.server.port");
        currencyConversion.setPort(port + " rest");
        currencyConversion.setQuantity(qty);
        // Use decimal arithmetic for the rate and total.
        currencyConversion.setTotalCalculatedAmount(new BigDecimal(qty).multiply(currencyConversion.getConversionMultiple()));
        return currencyConversion;
    }


    @GetMapping("/currency-conversion-feign/from/{from}/to/{to}/quantity/{qty}")
    public CurrencyConversion currencyConversionFeign(@PathVariable String from, @PathVariable String to, @PathVariable long qty){
        HashMap<String, String> uriVariables = new HashMap<>();

        // Feign resolves the exchange service through discovery; its Micrometer capability
        // observes the outgoing call and propagates tracing context when configured.
        CurrencyConversion currencyConversion = proxy.retrieveExchange(from, to);
        String port = environment.getProperty("local.server.port");
        currencyConversion.setPort(currencyConversion.getPort() + " exchange " + port + " conversion (feign)");
        //currencyConversion.setEnvironment(port + " feign");
        currencyConversion.setQuantity(qty);
        currencyConversion.setTotalCalculatedAmount(new BigDecimal(qty).multiply(currencyConversion.getConversionMultiple()));
        return currencyConversion;
    }
}
