package com.wanglei.MyApi.Listener;

import com.rabbitmq.client.Channel;
import com.wanglei.MyApi.service.UserInterfaceInfoService;
import com.wanglei.MyApicommon.model.dto.UserInterfaceInfoMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static com.wanglei.MyApicommon.model.constant.MqConstant.INVOKE_UNDO_QUEUE_NAME;

@Component
@Slf4j
public class MqListener {

    @Resource
    private UserInterfaceInfoService userInterfaceInfoService;
    @RabbitListener(queuesToDeclare = @Queue(INVOKE_UNDO_QUEUE_NAME))
    public void receiveSms(UserInterfaceInfoMessage userInterfaceInfoMessage, Message message, Channel channel) throws IOException {
        log.info("监听到消息啦，内容是："+userInterfaceInfoMessage);

        Long userId = userInterfaceInfoMessage.getUserId();
        Long interfaceInfoId = userInterfaceInfoMessage.getInterfaceInfoId();

        boolean result = false;
        try {
            result = userInterfaceInfoService.recoverInvokeCount(userId, interfaceInfoId);
        } catch (Exception e) {
            log.error("接口统计数据回滚失败！！！");
            e.printStackTrace();
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
            return;
        }

        if (!result){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);

    }

}
