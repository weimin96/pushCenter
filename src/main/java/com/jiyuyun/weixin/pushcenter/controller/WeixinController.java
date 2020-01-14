package com.jiyuyun.weixin.pushcenter.controller;

import com.alibaba.fastjson.JSONObject;
import com.jiyuyun.weixin.pushcenter.commont.Constant;
import com.jiyuyun.weixin.pushcenter.entity.Message;
import com.jiyuyun.weixin.pushcenter.entity.MessageTemplate;
import com.jiyuyun.weixin.pushcenter.entity.MessageType;
import com.jiyuyun.weixin.pushcenter.service.PushService;
import com.jiyuyun.weixin.pushcenter.thread.MessageListenerThread;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author pwm
 * @date 2019/11/20
 */
@RestController
@Slf4j
public class WeixinController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private MessageListenerThread messageListenerThread;

//    @Autowired
//    private MessageBackListenerThread messageBackListenerThread;

    @Autowired
    private PushService pushService;

    @Resource(name = "taskExecutor")
    private ExecutorService taskExecutorService;

    @GetMapping("/pushMessage")
    public String pushMessage(Integer num) {
        if (num == null) {
            num = 1;
        }
        Message message = new Message();
        //message.setContent("预警信息");
        message.setProvince("广东");
        message.setType(MessageType.ONE);
        message.setRemainCount(3);
//        List<String> list = new ArrayList<>(num);
//        list.add("og6lH6OJ0XxtUo3bkXaA7-Asm-B4");
        message.setOpenid("og6lH6OJ0XxtUo3bkXaA7-Asm-B4");
        // 批量存入队列
        for (int i = 0; i < num; i++) {
            message.setContent("消息" + i);
            log.info("生产消息{}", message.getContent());
            redisTemplate.opsForList().leftPush(Constant.WX_MESSAGE_KEY, JSONObject.toJSONString(message));
        }

        List<String> openidList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            openidList.add(String.valueOf(i));
        }
        message.setType(MessageType.ALL);
        message.setOpenidList(openidList);
        // 批量存入队列
        for (int i = 0; i < num; i++) {
            message.setContent("大量消息" + i);
            log.info("生产大量消息{}", message.getContent());
            redisTemplate.opsForList().leftPush(Constant.WX_MESSAGE_KEY, JSONObject.toJSONString(message));
        }
        return "成功";
    }

    @PostMapping("/test")
    public Map<String, Object> test(MessageTemplate temp) {
        Map<String, Object> result = new HashMap<>();
        int time = (int) (Math.random() * (800) + 500);
        try {
            Thread.sleep(time);
            if (new Random().nextInt(10) == 2) {
                result.put("errmsg", "error");
            } else {
                result.put("errmsg", "ok");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    @GetMapping("/updateThreadSize")
    public void updateThreadSize(Integer size) {
        if (size == null || size == 0) {
            return;
        }
        // 暂停监听队列
        messageListenerThread.pauseThread();
//        messageBackListenerThread.pauseThread();
        try {
            // 监听队列超时时间
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 设置信号量
        pushService.setSemaphore(size);
        log.info("启动监听队列");
        // 启动监听线程
//        messageBackListenerThread.resumeThread();
        messageListenerThread.resumeThread();
    }

    /*@GetMapping("/push")
    public void push(){
        Message message = new Message();
        message.setContent("绿雅中学(即将或正有特大暴雨并伴随瞬时大风)");
        message.setProvince("广东");
        message.setCount(3);
        message.setOpenId("og6lH6DNyQVmQMXzI2Dn1tpbF0yg");
        weixinService.sendMessage(message);
    }*/
}
