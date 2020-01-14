package com.jiyuyun.weixin.pushcenter.commont;

/**
 * @author pwm
 * @date 2019/11/19
 */
public class Constant {

    public static final String WX_MESSAGE_LISTENER_SIZE_KEY = "wx_message_listener_size";

    /**
     * redis key 消息通知
     */
    public static final String WX_MESSAGE_KEY = "wx_message";

    /**
     * redis key 消息通知 备份队列
     */
    public static final String WX_MESSAGE_BACK_KEY = "wx_message_back";

    /**
     * redis key 消息通知 备份队列 分布式锁
     */
    public static final String WX_MESSAGE_BACK_LOCK_KEY = "wx_message_back_lock";

    /**
     * redis key 消息发送状态次数
     */
    public static final String WX_MESSAGE_STATE_KEY = "wx_message_state";

    /**
     * access token key
     */
    public static final String ACCESS_TOKEN_KEY = "weixin_access_token";

    /**
     * js ticket key
     */
    public static final String JS_API_TICKET_KEY = "weixin_js_api_ticket";

    /**
     * 发送模板信息api
     */
    public static final String SENDMODEL_URL = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=";

}
