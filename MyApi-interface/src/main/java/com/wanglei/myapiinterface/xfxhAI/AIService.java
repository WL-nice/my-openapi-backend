package com.wanglei.myapiinterface.xfxhAI;

import org.springframework.stereotype.Service;

@Service
public interface AIService {

    String getAIResponse(String prompt);
}
