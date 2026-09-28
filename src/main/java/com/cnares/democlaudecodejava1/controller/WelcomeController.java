package com.cnares.democlaudecodejava1.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class WelcomeController {

    @Value("${app.version:v1.0.0}")
    private String appVersion;

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @GetMapping("/")
    public String welcome(Model model) {
        model.addAttribute("title", "Welcome to Demo");
        model.addAttribute("version", appVersion);
        model.addAttribute("time", LocalDateTime.now().format(TIME_FORMATTER));
        return "welcome";
    }
}
