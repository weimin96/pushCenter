package com.jiyuyun.weixin.pushcenter.entity;

/**
 * 消息类型
 *
 * @author pwm
 * @date 2019/11/19
 */
public enum MessageType {

    //群发
    ALL(0),
    //一对一
    ONE(1);


    private int value;

    MessageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
