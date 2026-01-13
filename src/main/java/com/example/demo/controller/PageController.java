package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "index";
    }


    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/timelog")
    public String timelog() {
        return "timelog";
    }

    @GetMapping("/qr-scanner")
    public String qrScanner() {
        return "qr-scanner";
    }
}

