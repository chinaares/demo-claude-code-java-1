package com.cnares.democlaudecodejava1.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class WelcomeController {

    @GetMapping("/")
    public String welcome(Model model) {
        model.addAttribute("title", "Welcome to Demo");
        model.addAttribute("version", "v1.0.0");
        model.addAttribute("time", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        model.addAttribute("features", new String[]{
            "🚀 Lightning Fast",
            "🎨 Beautiful UI",
            "⚡ Real-time Updates",
            "🔒 Secure & Reliable"
        });
        return "welcome";
    }
}
