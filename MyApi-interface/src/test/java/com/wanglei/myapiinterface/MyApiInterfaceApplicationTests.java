package com.wanglei.myapiinterface;

import com.wanglei.myapiinterface.xfxhAI.AIService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Random;
import java.util.Scanner;

@SpringBootTest
class MyApiInterfaceApplicationTests {

    @Test
    void contextLoads() {
        int number = new Random().nextInt(10) + 1;
        System.out.println(number);
    }

    @Resource
    private AIService aiService;
    @Test
    void AITest() {
        Scanner in = new Scanner(System.in);

        System.out.println(aiService.getAIResponse("鲁迅和周树人打过架吗"));

        //System.out.println(aiService.getAIResponse("你是谁"));
    }

}
