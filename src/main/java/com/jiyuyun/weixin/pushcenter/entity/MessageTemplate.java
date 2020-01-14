package com.jiyuyun.weixin.pushcenter.entity;


import lombok.Data;

import java.util.Map;

/**
 * 信息通知模板
 *
 * @author pwm
 * @date 2019/11/20
 */
@Data
public class MessageTemplate {

    /**
     * 接收者openid
     */
    private String touser;

    /**
     * 模板id
     */
    private String template_id;

    /**
     * 点击跳转链接
     */
    private String url;

    /**
     * 模板内容字体颜色
     */
    private String color;

    /**
     * 模板数据
     */
    private Map<String,Object> data;
}
