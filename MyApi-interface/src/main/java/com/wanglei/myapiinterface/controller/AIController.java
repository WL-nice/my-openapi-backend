package com.wanglei.myapiinterface.controller;

import com.wanglei.myapiinterface.annotation.AuthCheck;
import com.wanglei.myapiinterface.xfxhAI.AIService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AIController {
    @Resource
    private AIService aiService;

    @GetMapping("/ai")
    @AuthCheck
    public String getAIResponse(@RequestParam String prompt) {
        return aiService.getAIResponse(prompt);
    }
}
