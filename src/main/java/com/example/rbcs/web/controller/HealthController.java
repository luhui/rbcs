package com.example.rbcs.web.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actuator")
public class HealthController {
    @RequestMapping("/health")
    public String health() {
        return "OK";
    }
}
