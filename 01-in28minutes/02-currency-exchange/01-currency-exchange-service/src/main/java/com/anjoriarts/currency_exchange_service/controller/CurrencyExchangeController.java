package com.anjoriarts.currency_exchange_service.controller;

import com.anjoriarts.currency_exchange_service.bean.CurrencyExchange;
import com.anjoriarts.currency_exchange_service.repository.CurrencyExchangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Currency;

@RestController
public class CurrencyExchangeController {

    @Autowired
    private Environment env;

    CurrencyExchangeRepository repository;

    @Autowired
    public CurrencyExchangeController(CurrencyExchangeRepository repository){
        this.repository = repository;
    }

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyExchange retrieveExchangeValue(@PathVariable String from, @PathVariable String to){
        String port = env.getProperty("local.server.port");
        // Spring Data derives this query from the method name. Missing pairs are not handled here.
        CurrencyExchange exchange = repository.findByFromAndTo(from, to);
        exchange.setPort(port);
        return exchange;
    }

}
