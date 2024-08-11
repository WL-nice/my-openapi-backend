package com.wanglei.MyApi;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.wanglei.MyApi.mapper") //扫描mapper
@EnableDubbo
public class MyApiBackApplication {

	public static void main(String[] args) {

		SpringApplication.run(MyApiBackApplication.class, args);
	}

	@Bean
	public MessageConverter Jackson2JsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

}
