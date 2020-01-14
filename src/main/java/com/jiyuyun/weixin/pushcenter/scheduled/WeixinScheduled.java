package com.jiyuyun.weixin.pushcenter.scheduled;

import com.jiyuyun.weixin.pushcenter.service.WeixinService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 微信定时任务
 *
 * @author pwm
 * @date 2019/10/30
 */
@Slf4j
@Component
@EnableScheduling
public class WeixinScheduled {

    @Value("${weixin-appId}")
    private String appId;

    @Value("${weixin-appSecret}")
    private String appSecret;

    @Autowired
    private WeixinService weixinService;

    /**
     * 从微信服务端获取accessToken ticket并存入redis
     * 每隔两小时执行一次
     */
//    @Scheduled(cron = "0 0 0/2 * * ?")
    public void setAccessTokenAndTicket(){
        log.info("微信定时任务启动");
        String accessToken = weixinService.setAccessToken();
        if (StringUtils.isNotBlank(accessToken)){
            String ticket = weixinService.setTicket(accessToken);
            if (StringUtils.isNotBlank(ticket)){
                log.info("微信定时任务执行成功");
                return;
            }
        }
        log.error("定时任务执行失败");
    }




}
