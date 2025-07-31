package com.anjori.limitsservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anjori.limitsservice.bean.Limits;
import com.anjori.limitsservice.configuration.Configuration;

@RestController

public class LimitsController {

    private final Configuration configuration;

    @Autowired
    public LimitsController(Configuration configuration){
        this.configuration = configuration;
    }

    @GetMapping("/limits")
    public Limits retrieveLimits(){
        return new Limits(configuration.getMinimum(), configuration.getMaximum());
       // return new Limits(1, 1000);
    }
}
