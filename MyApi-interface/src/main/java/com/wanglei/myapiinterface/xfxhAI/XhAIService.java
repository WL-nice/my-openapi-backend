package com.wanglei.myapiinterface.xfxhAI;

import io.github.briqt.spark4j.SparkClient;
import io.github.briqt.spark4j.constant.SparkApiVersion;
import io.github.briqt.spark4j.exception.SparkException;
import io.github.briqt.spark4j.model.SparkMessage;
import io.github.briqt.spark4j.model.SparkSyncChatResponse;
import io.github.briqt.spark4j.model.request.SparkRequest;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class XhAIService implements AIService{
    @Resource
    private SparkClient sparkClient;

    @Override
    public String getAIResponse(String prompt) {
        List<SparkMessage> messages=new ArrayList<>();
        messages.add(SparkMessage.userContent(prompt));
        SparkRequest sparkRequest=SparkRequest.builder()
                // 消息列表
                .messages(messages)
                // 模型回答的tokens的最大长度,非必传，默认为2048。
                // V1.5取值为[1,4096]
                // V2.0取值为[1,8192]
                // V3.0取值为[1,8192]
                .maxTokens(2048)
                // 核采样阈值。用于决定结果随机性,取值越高随机性越强即相同的问题得到的不同答案的可能性越高 非必传,取值为[0,1],默认为0.5
                .temperature(0.2)
                // 指定请求版本，默认使用最新3.0版本
                .apiVersion(SparkApiVersion.V1_5)
                .build();

        try {
            // 同步调用
            SparkSyncChatResponse chatResponse = sparkClient.chatSync(sparkRequest);
            return chatResponse.getContent();
        } catch (SparkException e) {
            return "发生异常了：" + e.getMessage();
        }
    }
}
