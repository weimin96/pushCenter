package com.jiyuyun.weixin.pushcenter.service;

import com.alibaba.fastjson.JSONObject;
import com.jiyuyun.weixin.pushcenter.commont.Constant;
import com.jiyuyun.weixin.pushcenter.entity.DataExtra;
import com.jiyuyun.weixin.pushcenter.entity.Message;
import com.jiyuyun.weixin.pushcenter.entity.MessageTemplate;
import com.jiyuyun.weixin.pushcenter.entity.MessageType;
import com.jiyuyun.weixin.pushcenter.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author pwm
 * @date 2019/11/20
 */
@Slf4j
@Service
public class WeixinService {

    @Value("${weixin-appId}")
    private String appId;

    @Value("${weixin-appSecret}")
    private String appSecret;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RestTemplate restTemplate;


    /**
     * 获取accessToken
     *
     * @return accessToken
     */
    public String getAccessToken() {
//        String accessToken = (String) redisTemplate.opsForValue().get(Constant.ACCESS_TOKEN_KEY);
        String accessToken = "28_E6H91yFRFwFTktDprU-erUd653WaFf7bkP3uf1ObGs821vIp9_Tb7LicV4gv_p6zMwRsBi3oPIvm5bDEHh0D6htoD7C-1mTcSeEKCpvCbw9GuKj48x0nq3WJP-I1r2nL701dKyojZC9Z1CA8PLQhAIATPU";
        if (StringUtils.isBlank(accessToken)) {
            return setAccessToken();
        }
        return accessToken;
    }

    /**
     * 设置accessToken
     *
     * @return accessToken
     */
    public String setAccessToken() {
        /*准备发送GET请求*/
        String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";
        url = url.replace("APPID", appId).replace("APPSECRET", appSecret);
        String response = restTemplate.getForObject(url, String.class);
        String accessToken = (String) JSONObject.parseObject(response).get("access_token");

        if (StringUtils.isBlank(accessToken)) {
            log.error("设置AccessToken失败：appid：{},appSecret：{}", appId, appSecret);
            return null;
        }
        redisTemplate.opsForValue().set(Constant.ACCESS_TOKEN_KEY, accessToken, 7200, TimeUnit.SECONDS);
        log.info("设置accessToken->{}", accessToken);
        return accessToken;
    }

    /**
     * 获取ticket
     *
     * @return ticket
     */
    public String getTicket() {
        return (String) redisTemplate.opsForValue().get(Constant.JS_API_TICKET_KEY);
    }

    /**
     * 设置ticket
     *
     * @param accessToken accessToken
     * @return ticket
     */
    public String setTicket(String accessToken) {
        String url = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token=ACCESS_TOKEN&type=jsapi";
        url = url.replace("ACCESS_TOKEN", accessToken);
        String response = restTemplate.getForObject(url, String.class);
        JSONObject jsonObject = JSONObject.parseObject(response);
        String errcode = jsonObject.getString("errcode");
        if ("0".equals(errcode)) {
            String ticket = jsonObject.getString("ticket");
            redisTemplate.opsForValue().set(Constant.JS_API_TICKET_KEY, ticket, 7200, TimeUnit.SECONDS);
            log.info("设置ticket成功：{}", ticket);
            return ticket;
        }
        log.error("设置ticket失败,errcode：{}", errcode);
        return null;
    }





}
