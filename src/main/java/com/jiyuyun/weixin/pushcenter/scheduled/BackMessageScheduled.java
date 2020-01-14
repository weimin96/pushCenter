package com.jiyuyun.weixin.pushcenter.scheduled;

import com.alibaba.fastjson.JSONObject;
import com.jiyuyun.weixin.pushcenter.commont.Constant;
import com.jiyuyun.weixin.pushcenter.entity.Message;
import com.jiyuyun.weixin.pushcenter.service.PushService;
import com.jiyuyun.weixin.pushcenter.utils.RedisLockUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 定时器 重试消息队列
 *
 * @author pwm
 * @date 2020/1/2
 */
@Slf4j
@Component
@EnableScheduling
public class BackMessageScheduled {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PushService pushService;

    @Autowired
    private RedisLockUtil redisLockUtil;

    @Scheduled(cron = "*/10 * * * * ?")
    public void listenerBackMessage() {
        log.info("监听失败消息");
        //
        Long time = System.currentTimeMillis();

        // 加锁
        boolean lock = redisLockUtil.setLock(Constant.WX_MESSAGE_BACK_LOCK_KEY,9000);
        if (!lock){
            return;
        }
        String lockId = redisLockUtil.getLock(Constant.WX_MESSAGE_BACK_LOCK_KEY);
        Set set = redisTemplate.opsForZSet().rangeByScore(Constant.WX_MESSAGE_BACK_KEY, 0, time);
        if (set == null || set.size() == 0) {
            // 释放锁
            redisLockUtil.releaseLock(Constant.WX_MESSAGE_BACK_LOCK_KEY,lockId);
            return;
        }
        redisTemplate.opsForZSet().removeRangeByScore(Constant.WX_MESSAGE_BACK_KEY, 0, time);
        // 释放锁
        redisLockUtil.releaseLock(Constant.WX_MESSAGE_BACK_LOCK_KEY,lockId);

        log.info("失败重试");
        for (Object o:set){
            Message message = JSONObject.parseObject((String)o, Message.class);
            pushService.pushMessage(message);
            log.info("消费失败消息<{}-{}>,剩余次数<{}>", message.getContent(),message.getOpenid(), message.getRemainCount());
        }
    }

    public static void main(String[] args) throws ParseException {
        Calendar cd=Calendar.getInstance();
        cd.setTimeInMillis(1578386347771L);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String now = simpleDateFormat.format(cd.getTime());
        System.out.println(now);
    }

}
