package com.jiyuyun.weixin.pushcenter.entity;

import com.alibaba.fastjson.JSONObject;
import com.jiyuyun.weixin.pushcenter.commont.Constant;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author pwm
 * @date 2019/11/19
 */
@Data
public class Message implements Cloneable{

    private MessageType type;

    /**
     * 微信openid
     */
    private List<String> openidList;

    /**
     * 微信openid 批量
     */
    private String openid;

    /**
     * 失败次数
     */
    private Integer errorCount;

    /**
     * 剩余次数
     */
    private Integer remainCount;

    /**
     * 省份
     */
    private String province;

    /**
     * 模板内容
     */
    private String content;

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", openidList=" + openidList +
                ", openid='" + openid + '\'' +
                ", errorCount=" + errorCount +
                ", remainCount=" + remainCount +
                ", province='" + province + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
