package com.anjoriarts.limitsservice.controller;

import com.anjoriarts.limitsservice.bean.Limits;
import com.anjoriarts.limitsservice.configuration.LimitsConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LimitsController {

    LimitsConfiguration configuration;

    @Autowired
    public LimitsController(LimitsConfiguration configuration) {
        this.configuration = configuration;
    }
   @GetMapping("/limits")
    public Limits retrieveLimits(){
       // Return the values Spring bound from the effective configuration sources.
       return new Limits(configuration.getMinimum(), configuration.getMaximum());
   }
}
