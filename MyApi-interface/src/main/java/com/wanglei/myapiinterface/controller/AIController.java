package com.wanglei.myapiinterface.controller;

import com.wanglei.myapiclientsdk.model.AIRequest;
import com.wanglei.myapiinterface.annotation.AuthCheck;
import com.wanglei.myapiinterface.xfxhAI.AIService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

@RestController
public class AIController {
    @Resource
    private AIService aiService;

    @PostMapping("/ai")
    @AuthCheck
    public String getAIResponse(@RequestBody AIRequest prompt) {
        if(prompt == null){
            return "内容不能为空";
        }
        return aiService.getAIResponse(prompt.getPrompt());
    }
}
