package com.wanglei.myapiclientsdk;

import com.wanglei.myapiclientsdk.client.MyApiClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("myapi.client")
@Data
@ComponentScan
public class MyApiClientConfig {

    private String accessKey;

    private String secretKet;

    @Bean
    public MyApiClient myApiClient(){
        return new MyApiClient(accessKey,secretKet);
    }

}
