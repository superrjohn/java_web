package com.xuecheng.learning.service.Impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReceivePayNotifyService {

    @Autowired
    MyCourseTablesService myCourseTablesService;

    //監聽哪一個隊列
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message){
        //失敗後5秒後再重試
        try {
            Thread.sleep(5000);
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        //從mq中得到的消息為json
        byte[] body = message.getBody();
        String jsonString = new String(body);
        //將json轉為對象
        MqMessage mqMessage = JSON.parseObject(jsonString, MqMessage.class);
        //解析消息的內容,選課id
        String chooseCourseId = mqMessage.getBusinessKey1();
        //訂單類型
        String orderType = mqMessage.getBusinessKey2();
        //學習中心服務只要購買課程的支付訂單數據(用topic交換機也可實現)
        if(orderType.equals("60201")){
            boolean b = myCourseTablesService.saveChooseCourseSuccess(chooseCourseId);
            if(!b){
                XueChengPlusException.cast("選課記錄失敗!");
            }
        }
    }
}
