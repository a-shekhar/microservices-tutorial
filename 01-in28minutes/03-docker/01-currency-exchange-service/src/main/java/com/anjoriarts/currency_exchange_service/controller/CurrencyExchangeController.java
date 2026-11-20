package com.anjoriarts.currency_exchange_service.controller;

import com.anjoriarts.currency_exchange_service.bean.CurrencyExchange;
import com.anjoriarts.currency_exchange_service.repository.CurrencyExchangeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Currency;

@RestController
public class CurrencyExchangeController {

    private Logger logger = LoggerFactory.getLogger(getClass().getName());

    @Autowired
    private Environment env;

    CurrencyExchangeRepository repository;

    @Autowired
    public CurrencyExchangeController(CurrencyExchangeRepository repository){
        this.repository = repository;
    }

    @GetMapping("/currency-exchange/from/{from}/to/{to}")
    public CurrencyExchange retrieveExchangeValue(@PathVariable String from, @PathVariable String to){
        // The configured log pattern includes trace/span IDs when a tracing context exists.
        logger.info("retrieveExchangeValue called with {} to {}", from, to);

        // Spring Data derives this query from the method name. Missing pairs are not handled here.
        CurrencyExchange exchange = repository.findByFromAndTo(from, to);
        if(exchange == null){
            throw new RuntimeException("Unable to find data for " + from + " to " + to);
        }
        String port = env.getProperty("local.server.port");
        exchange.setPort(port);
        return exchange;
    }

}
