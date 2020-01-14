//package com.jiyuyun.weixin.pushcenter.thread;
//
//import com.alibaba.fastjson.JSONObject;
//import com.jiyuyun.weixin.pushcenter.commont.Constant;
//import com.jiyuyun.weixin.pushcenter.entity.Message;
//import com.jiyuyun.weixin.pushcenter.service.PushService;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.stereotype.Component;
//
//import java.util.concurrent.TimeUnit;
//
///**
// * @author pwm
// * @date 2019/12/26
// */
//@Slf4j
//@Component
//public class MessageBackListenerThread extends Thread {
//
//    @Autowired
//    private RedisTemplate<String, Object> redisTemplate;
//
//    @Autowired
//    private PushService pushService;
//
//    private final Object lock = new Object();
//
//    private boolean pause = false;
//
//    /**
//     * 调用该方法实现线程的暂停
//     */
//    public void pauseThread() {
//        this.pause = true;
//    }
//
//    /**
//     * 调用该方法实现恢复线程的运行
//     */
//    public void resumeThread() {
//        pause = false;
//        synchronized (lock) {
//            lock.notify();
//            log.info("恢复线程");
//        }
//    }
//
//    /**
//     * 这个方法只能在run 方法中实现，不然会阻塞主线程，导致页面无响应
//     */
//    private void onPause() {
//        synchronized (lock) {
//            try {
//                log.info("暂停线程{}", Thread.currentThread().getName());
//                lock.wait();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Override
//    public void run() {
//        while (true) {
//            log.info("监听重试消息队列");
//            while (pause) {
//                onPause();
//            }
//            String msgJson = (String) redisTemplate.opsForList().rightPop(Constant.WX_MESSAGE_BACK_KEY, 10000, TimeUnit.MILLISECONDS);
//            if (StringUtils.isNotBlank(msgJson)) {
//                log.info("失败重试");
//                Message message = JSONObject.parseObject(msgJson, Message.class);
//                pushService.pushMessage(message);
//                log.info("消费失败消息<{}-{}>,剩余次数<{}>", message.getContent(),message.getOpenid(), message.getCount());
//            }
//        }
//    }
//
//
//}
