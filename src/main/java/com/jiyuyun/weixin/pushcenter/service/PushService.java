package com.jiyuyun.weixin.pushcenter.service;

import com.alibaba.fastjson.JSONObject;
import com.jiyuyun.weixin.pushcenter.commont.Constant;
import com.jiyuyun.weixin.pushcenter.entity.DataExtra;
import com.jiyuyun.weixin.pushcenter.entity.Message;
import com.jiyuyun.weixin.pushcenter.entity.MessageTemplate;
import com.jiyuyun.weixin.pushcenter.entity.MessageType;
import com.jiyuyun.weixin.pushcenter.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 推送业务
 *
 * @author pwm
 * @date 2019/12/18
 */
@Service
@Slf4j
public class PushService {

    @Autowired
    private WeixinService weixinService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

//    private final int maxPoolSize = 50;

    @Value("${retry-count}")
    private Integer defaultRetryCount;

    @Value("${weixin.template-id.type.one}")
    private String templateIdOne;

    @Value("${weixin.template-id.type.second}")
    private String templateIdSecond;

    @Resource(name = "taskExecutor")
    private ExecutorService taskExecutorService;

    public void setSemaphore(int size) {
        log.info("设置信号量{}", size);
        this.semaphore = new Semaphore(size);
        this.maxPoolSize = size;
    }

    private int maxPoolSize = 50;

    private static AtomicInteger successCount = new AtomicInteger(0);

    /**
     * 信号量 控制线程数量
     */
    private Semaphore semaphore = new Semaphore(50);

    public void pushMessage(Message message) {
        MessageTemplate temp = buildMessageTemplate(message);
        //String accessToken = weixinService.getAccessToken();
        String url = "http://127.0.0.1:8080/test";//= Constant.SENDMODEL_URL + accessToken;
        // 群发
        if (message.getType() == MessageType.ALL) {
            for (String openid : message.getOpenidList()) {
                temp.setTouser(openid);
                String jsonString = JSONObject.toJSONString(temp);
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    // 深拷贝
                    Message m = (Message) message.clone();
                    serverSend(url, jsonString, openid, m, semaphore);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // 单点
            temp.setTouser(message.getOpenid());
            String jsonString = JSONObject.toJSONString(temp);
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            serverSend(url, jsonString, temp.getTouser(), message, semaphore);
        }
    }

    /**
     * 发送请求 另起线程
     */
    public void serverSend(String url, String jsonString, String openid, Message message, Semaphore semaphore) {
        taskExecutorService.execute(() -> {
            System.out.println("线程" + Thread.currentThread().getName() +
                    "进入，当前已有" + (maxPoolSize - semaphore.availablePermits()) + "个并发");
            JSONObject jsonObject = restTemplate.postForObject(url, jsonString, JSONObject.class);
            try {
                if (jsonObject != null && jsonObject.get("errmsg") != null) {
                    if ("ok".equals(jsonObject.get("errmsg"))) {
                        log.info("成功->{}-{}", message.getContent(), openid);
                        // TODO 成功入库
                        // 记录成功数
//                        List<Object> exec = null;
//                        do {
                            // watch某个key,当该key被其它客户端改变时,则会中断当前的操作
//                            redisTemplate.watch(Constant.WX_MESSAGE_STATE_KEY);
                            redisTemplate.opsForHash().increment(Constant.WX_MESSAGE_STATE_KEY, "success",1L);
                            // 开始事务
                            /*redisTemplate.multi();
                            if (successCount != null) {
                                successCount++;
                            } else {
                                successCount = 1;
                            }
                            redisTemplate.opsForHash().put(Constant.WX_MESSAGE_STATE_KEY, "success", successCount);
                            // 提交事务
                            exec = redisTemplate.exec();
                            if (exec.isEmpty()){
                                System.out.println("error"+(successCount-1)+"-->"+successCount);
                            }else{
                                System.out.println((successCount-1)+"-->"+successCount);
                            }*/

//                        } while (exec.isEmpty());//如果失败则重试
                    } else {
                        // 失败
                        errorHandle(message, openid);
                    }
                } else {
                    // 失败
                    errorHandle(message, openid);
                }
            } finally {
                semaphore.release();
            }
        });
    }

    /**
     * 失败处理
     *
     * @param message message
     * @param openid  openid
     */
    private void errorHandle(Message message, String openid) {
        // 失败处理
        log.error("失败->{}-{}", message.getContent(), openid);
        // 重新构造消息体 放入临时队列
        message.setOpenid(openid);
        message.setOpenidList(null);
        message.setType(MessageType.ONE);
        // 剩余次数
        int remainCount = message.getRemainCount() == null ? defaultRetryCount : message.getRemainCount();
        // 失败次数
        int errorCount = message.getErrorCount() == null ? 0 : message.getErrorCount();
        if (remainCount == 0) {
            log.error(defaultRetryCount + "次失败->{}-{}", message.getContent(), openid);
            // 记录失败

            return;
        }
        remainCount--;
        errorCount++;
        message.setRemainCount(remainCount);
        message.setErrorCount(errorCount);
        // 延时一分钟
        long time = System.currentTimeMillis() + (errorCount) * 60*1000;
        redisTemplate.opsForZSet().add(Constant.WX_MESSAGE_BACK_KEY, JSONObject.toJSONString(message), time);
        //redisTemplate.opsForList().leftPush(Constant.WX_MESSAGE_BACK_KEY, JSONObject.toJSONString(message));
        // 记录失败
//        List<Object> exec;
//        do {
            // watch某个key,当该key被其它客户端改变时,则会中断当前的操作
//            redisTemplate.watch(Constant.WX_MESSAGE_STATE_KEY);
            redisTemplate.opsForHash().increment(Constant.WX_MESSAGE_STATE_KEY, String.valueOf(errorCount),1L);
            // 开始事务
            /*redisTemplate.multi();
            if (count != null) {
                count++;
            } else {
                count = 1;
            }
            redisTemplate.opsForHash().put(Constant.WX_MESSAGE_STATE_KEY, String.valueOf(errorCount), count);
            // 提交事务
            exec = redisTemplate.exec();*/
            //如果失败则重试
//        } while (exec == null || exec.isEmpty());
    }

    /**
     * 构建模板消息
     *
     * @param message message
     * @return
     */
    private MessageTemplate buildMessageTemplate(Message message) {
        Date now = new Date();
        String nowStr = DateUtil.convertDate2Str(now);
        // 模板参数封装
        MessageTemplate temp = new MessageTemplate();

        Map<String, Object> data = new HashMap<>(4);

        DataExtra keyword1 = new DataExtra();
        DataExtra keyword2 = new DataExtra();
        DataExtra remark = new DataExtra();

        // 省份
        keyword1.setValue(message.getProvince());
        keyword1.setColor("#173177");
        // 预警信息
        keyword2.setValue(message.getContent());
        keyword2.setColor("#FF0000");
        // 备注
        remark.setValue("时间:" + nowStr);
        remark.setColor("#173177");

        data.put("keyword1", keyword1);
        data.put("keyword2", keyword2);
        data.put("remark", remark);

        temp.setTemplate_id(templateIdSecond);
        temp.setColor("#173177");
        temp.setUrl("https://open.weixin.qq.com/connect/oauth2/authorize?appid=wx45d1c40c975066f9&redirect_uri=http%3A%2F%2Fm.fengyulei.net%2Fwx%2Flogin.do&response_type=code&scope=snsapi_base&state=jiyuyun#wechat_redirect");
        temp.setData(data);
        return temp;
    }
}
