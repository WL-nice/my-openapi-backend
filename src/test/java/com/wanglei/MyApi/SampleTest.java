package com.wanglei.MyApi;

import com.wanglei.MyApicommon.model.dto.UserInterfaceInfoMessage;
import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;

import static com.wanglei.MyApicommon.model.constant.MqConstant.INVOKE_UNDO_QUEUE_NAME;

@SpringBootTest

public class SampleTest {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Test
    public void test() {
        UserInterfaceInfoMessage userInterfaceInfoMessage = new UserInterfaceInfoMessage();
        userInterfaceInfoMessage.setInterfaceInfoId(1L);
        userInterfaceInfoMessage.setUserId(1L);
        rabbitTemplate.convertAndSend(INVOKE_UNDO_QUEUE_NAME, userInterfaceInfoMessage);
    }
}