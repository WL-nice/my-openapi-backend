package com.wanglei.myapiinterface.xfxhAI;

import io.github.briqt.spark4j.SparkClient;
import lombok.Data;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Data
@Component
public class AIClient {

    @Bean
    public SparkClient getSparkClient() {
        SparkClient sparkClient = new SparkClient();
        sparkClient.appid="5229b125";
        sparkClient.apiSecret="YTQ3ZTY2MTljNjc3NmY5ODExOThlNzI4";
        sparkClient.apiKey="25d5eea1b4053b195ab71bdf75ac01b7";
        return sparkClient;
    }

}
