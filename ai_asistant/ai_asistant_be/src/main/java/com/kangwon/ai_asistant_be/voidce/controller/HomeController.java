package com.kangwon.ai_asistant_be.voidce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/member")
    public String member() {
        return "member";
    }
}
